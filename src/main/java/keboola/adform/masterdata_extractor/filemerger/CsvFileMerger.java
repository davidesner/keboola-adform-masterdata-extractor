/*
 */
package keboola.adform.masterdata_extractor.filemerger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class CsvFileMerger {

    public static void mergeFiles(List<String> filePaths, String mergedPath, String mergedName) throws MergeException {
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
        for (String fPath : filePaths) {
            FileInputStream fis = null;
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
}
