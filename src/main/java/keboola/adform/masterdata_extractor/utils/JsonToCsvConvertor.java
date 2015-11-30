/*
 */
package keboola.adform.masterdata_extractor.utils;

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Convertor from JSON to CSV files.
 * The JSON file needs to have following structure:
 * [
 * {name:value, name:value, ..}
 * ]
 * Otherwise it wont be converted.
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class JsonToCsvConvertor {

    private final char SEPARATOR;

    public JsonToCsvConvertor(char SEPARATOR) {
        this.SEPARATOR = SEPARATOR;
    }

    public JsonToCsvConvertor() {
        this.SEPARATOR = '\t';
    }

    /**
     * Converts JSON file to CSV
     *
     * @param sourcePath path of the source json file
     * @param destPath path of the destination csv file
     * @throws Exception
     */
    public void convert(String sourcePath, String destPath) throws Exception {

        File source = new File(sourcePath);
        if (!source.exists()) {
            throw new Exception("File: " + sourcePath + " does not exist.");
        }
        CSVWriter writer = null;
        FileInputStream fis = null;
        BufferedReader rd = null;
        FileOutputStream fos = new FileOutputStream(destPath);

        try {
            writer = new CSVWriter(new OutputStreamWriter(fos, Charset.forName("UTF-8")), this.SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);

            JsonFactory f = new MappingJsonFactory();
            fis = new FileInputStream(source);
            rd = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            JsonParser jp = f.createParser(rd);
            JsonToken currentToken;

            boolean firstRun = true;
            currentToken = jp.nextToken();
            if (currentToken != JsonToken.START_ARRAY) {
                System.out.println("Error: invalid JSON format in file: " + source.getName());
                throw new Exception("\"Error: invalid JSON format in file: \" + source.getName()");
            }
            currentToken = jp.nextToken();

            List<String> headers = new ArrayList<String>();
            List<String> values = new ArrayList<String>();
            // For each of the records in the array
            int line = 1;
            while (currentToken != JsonToken.END_ARRAY) {
                line++;
                // read the record into a tree model,
                // this moves the parsing position to the end of it
                JsonNode node = jp.readValueAsTree();

                for (Iterator<String> fields = node.fieldNames(); fields.hasNext();) {

                    String field = fields.next();
                    //write header if firstRun
                    if (firstRun) {
                        headers.add(field);
                    }
                    values.add(node.get(field).asText());
                }

                //writer to output file
                //write header if firstRun
                if (firstRun) {
                    writer.writeNext(headers.toArray(new String[0]));
                    firstRun = false;
                }
                writer.writeNext(values.toArray(new String[0]));
                values.clear();
                headers.clear();
                currentToken = jp.nextToken();
            }

        } catch (IOException ex) {
            Logger.getLogger(JsonToCsvConvertor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
                rd.close();
                fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

    }
}
