/*
 */
package keboola.adform.masterdata_extractor.filemerger;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import keboola.adform.masterdata_extractor.pojo.MasterFile;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class CsvFileMerger {

    public static void mergeFiles(List<MasterFile> masterFiles, String mergedPath, String mergedName) throws MergeException {
        BufferedReader reader = null;
        String headerLine = "";

        //create output file
        File folder = new File(mergedPath);
        // if the directory does not exist, create it
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File outFile = new File(mergedPath + File.separator + mergedName);

        int i = 0;
        FileChannel out = null;
        FileOutputStream fout = null;

        try {
            //validate merged csv files
            dataStructureMatch(masterFiles);
        } catch (MergeException ex) {
            throw ex;
        }

        for (MasterFile mFile : masterFiles) {
            FileInputStream fis = null;
            String fPath = mFile.getLocalAbsolutePath();
            try {
                File file = new File(fPath);
                //retrieve file header
                fis = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(fis));
                headerLine = reader.readLine();

                if (headerLine == null) {
                    continue;
                }
                //write header from first file and retrieve filechannel 
                if (i == 0) {
                    fout = new FileOutputStream(outFile);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fout));
                    bw.write(headerLine);
                    bw.newLine();
                    bw.flush();
                    out = fout.getChannel();

                }
                //write to outFile using NIO
                FileChannel in = fis.getChannel();
                long pos = 0;
                //set position according to header (first run is set by writer)
                pos = headerLine.length() + 2;//+2 because of NL character

                for (long p = pos, l = in.size(); p < l;) {
                    p += in.transferTo(p, l - p, out);
                }

                i++;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CsvFileMerger.class.getName()).log(Level.SEVERE, null, ex);
                throw new MergeException("File not found. " + ex.getMessage());
            } catch (IOException ex) {
                Logger.getLogger(CsvFileMerger.class.getName()).log(Level.SEVERE, null, ex);
                throw new MergeException("Error merging files. " + ex.getMessage());
            } finally {
                try {
                    reader.close();
                    fis.close();

                } catch (IOException ex) {
                    Logger.getLogger(CsvFileMerger.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    /**
     *
     * @param filePaths - list of files to merge
     * @return header line with the most columns
     * @throws Exception
     */
    private static String getLongestHeaders(List<MasterFile> mFiles) throws Exception {
        String[] headers = null;
        String headerLine = "";
        String currFile = "";
        BufferedReader reader;
        FileInputStream fis;
        try {
            int maxHeaderSize = 0;
            for (MasterFile mFile : mFiles) {
                String fPath = mFile.getLocalAbsolutePath();
                currFile = fPath;

                File csvFile = new File(fPath);
                FileReader freader = new FileReader(csvFile);
                CSVReader csvreader = new CSVReader(freader, '\t', CSVWriter.NO_QUOTE_CHARACTER);
                headers = csvreader.readNext();
                if (headers != null) {
                    if (headers.length > maxHeaderSize) {
                        maxHeaderSize = headers.length;
                        //get header line
                        fis = new FileInputStream(fPath);
                        reader = new BufferedReader(new InputStreamReader(fis));
                        headerLine = reader.readLine();
                        reader.close();
                    }

                } else {
                    throw new Exception("Error reading csv file header: " + currFile);
                }
                csvreader.close();
            }
            if (maxHeaderSize == 0 || headerLine.equals("")) {
                throw new Exception("Zero length header in csv file!");
            }
            return headerLine;

        } catch (FileNotFoundException ex) {
            throw new Exception("CSV file not found. " + currFile + " " + ex.getMessage());
        } catch (IOException ex) {
            throw new Exception("Error reading csv file: " + currFile + " " + ex.getMessage());
        }
    }

    /**
     * Validates the structure of the merged csv files.
     *
     * @param mFiles List of MasterFiles to check
     * @return Returns true if the data structure of all given files matches.
     * Throws an user exception (MergeException) otherwise.
     * @throws MergeException
     */
    public static boolean dataStructureMatch(List<MasterFile> mFiles) throws MergeException {
        String[] headers = null;
        String headerLine = "";
        String currFile = "";
        BufferedReader reader;
        FileInputStream fis;
        boolean firstRun = true;
        try {
            int maxHeaderSize = 0;
            for (MasterFile mFile : mFiles) {
                String fPath = mFile.getLocalAbsolutePath();
                currFile = fPath;

                File csvFile = new File(fPath);
                FileReader freader = new FileReader(csvFile);
                CSVReader csvreader = new CSVReader(freader, '\t', CSVWriter.NO_QUOTE_CHARACTER);
                headers = csvreader.readNext();
                if (headers != null) {
                    if (headers.length != maxHeaderSize) {

                        maxHeaderSize = headers.length;
                        //get header line
                        fis = new FileInputStream(fPath);
                        reader = new BufferedReader(new InputStreamReader(fis));
                        headerLine = reader.readLine();
                        reader.close();
                        if (!firstRun) {
                            throw new MergeException("Data structure has changed within the given interval. File:" + mFile.getName()
                                    + "; Time tag of the file:" + mFile.getCreated() + ". The number of columns has changed.", 1);
                        }
                        firstRun = false;

                    }

                } else {
                    throw new MergeException("Error reading csv file header: " + currFile, 1);
                }
                csvreader.close();
            }
            if (maxHeaderSize == 0 || headerLine.equals("")) {
                throw new MergeException("Zero length header in csv file!", 1);
            }
            return true;

        } catch (FileNotFoundException ex) {
            throw new MergeException("CSV file not found. " + currFile + " " + ex.getMessage(), 1);
        } catch (IOException ex) {
            throw new MergeException("Error reading csv file: " + currFile + " " + ex.getMessage(), 1);
        }
    }

    private static String implode(String separator, String... data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length - 1; i++) {
            //data.length - 1 => to not add separator at the end
            if (!data[i].matches(" *")) {//empty string are ""; " "; "  "; and so on
                sb.append(data[i]);
                sb.append(separator);
            }
        }
        sb.append(data[data.length - 1].trim());
        return sb.toString();
    }
}
