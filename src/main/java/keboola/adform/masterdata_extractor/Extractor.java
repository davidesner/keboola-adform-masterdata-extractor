/*
 */
package keboola.adform.masterdata_extractor;

import keboola.adform.masterdata_extractor.api_client.ClientException;
import keboola.adform.masterdata_extractor.api_client.APIClient;
import keboola.adform.masterdata_extractor.utils.FileHandler;
import keboola.adform.masterdata_extractor.pojo.MasterFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class Extractor {

    public final APIClient client;
    private final static long BACKOFF_INTERVAL = 500;
    private final static int RETRIES = 5;

    public Extractor(String userName, String password, String masterDataListUrl) {
        //init client
        this.client = new APIClient(userName, password, masterDataListUrl);
    }

    public List<String> downloadAndUnzip(List<MasterFile> fileList, String folderPath) throws ExtractorException {
        //download files

        downloadFiles(fileList, folderPath);

        List<String> rawFilePaths;
        //unzip archives and delete data
        rawFilePaths = unzip(fileList, folderPath);
        //delete zipFiles
        try {
            FileHandler.deleteFile(folderPath + File.separator + fileList.get(0).getPrefix());
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rawFilePaths;

    }

    public List<String> unzip(List<MasterFile> fileList, String folderPath) throws ExtractorException {
        byte[] buffer = new byte[2048];
        List<String> rawFiles = new ArrayList<String>();
        try {

            //create output directory is not exists
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            for (MasterFile file : fileList) {

                // check if for filetype
                String fileType = file.getName().substring(file.getName().lastIndexOf('.') + 1);
                fileType = fileType.toLowerCase();

                if (fileType.equals("zip")) {//case it is a zip file

                    //get the zip file content
                    ZipInputStream zis
                            = new ZipInputStream(new FileInputStream(folderPath + File.separator + file.getPrefix() + File.separator + file.getName()));
                    //get the zipped file list entry
                    ZipEntry ze = zis.getNextEntry();
                    File newFile = null;
                    while (ze != null) {

                        String fileName = ze.getName();
                        newFile = new File(folderPath + File.separator + fileName);

                        //create all non exists folders            
                        new File(newFile.getParent()).mkdirs();

                        FileOutputStream fos = new FileOutputStream(newFile);

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.close();
                        ze = zis.getNextEntry();

                        rawFiles.add(newFile.getAbsolutePath());
                    }

                    zis.closeEntry();
                    zis.close();
                }

                if (fileType.equals("gz")) {
                    FileInputStream fis = new FileInputStream(folderPath + File.separator + file.getPrefix() + File.separator + file.getName());
                    GZIPInputStream gis = new GZIPInputStream(fis);
                    File newFile = new File(folderPath + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".")));

                    //create all non exists folders            
                    new File(newFile.getParent()).mkdirs();

                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = gis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    //close resources
                    fos.close();
                    rawFiles.add(newFile.getAbsolutePath());
                    gis.close();
                }
                if (!fileType.equals("gz") || fileType.equals("zip")) {
                    throw new ExtractorException("Failed to unzip downloaded file. Unsupported archive type.", 1);
                }

            }

            return rawFiles;

        } catch (IOException ex) {
            throw new ExtractorException("Failed to unzip downloaded file. " + ex.getMessage());
        } catch (Exception ex) {
            throw new ExtractorException("Failed to unzip downloaded file. " + ex.getMessage());
        }
    }

    public void downloadFiles(List<MasterFile> fileList, String folderPath) throws ExtractorException {
        File folder = new File(folderPath);

        // if the directory does not exist, create it
        if (!folder.exists()) {
            folder.mkdirs();
        }
        for (MasterFile file : fileList) {
            int r = 0;
            boolean succ = false;
            String filePath = folderPath + File.separator + file.getPrefix() + File.separator + file.getName();
            do {
                /*Wait until next try*/
                try {
                    Thread.sleep(BACKOFF_INTERVAL * r);

                } catch (InterruptedException ex) {
                    Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
                }
                r++;

                try {
                    succ = client.downloadFile(file.getAbsolutePath(), filePath, false);
                } catch (ClientException ex) {
                    throw new ExtractorException("Failed to download files. " + ex.getMessage());
                }
            } while (r < RETRIES && !succ);

            if (r == RETRIES - 1 || !succ) {

                try {
                    succ = client.downloadFile(file.getAbsolutePath(), filePath, true);
                    if (!succ) {
                        throw new ExtractorException("Failed to download files. " + client.getApiException().getMessage());
                    }
                } catch (ClientException ex) {
                    throw new ExtractorException("Failed to download files. " + ex.getMessage());
                }
            }
        }
    }
}
