/*
 */
package keboola.adform.masterdata_extractor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import au.com.bytecode.opencsv.CSVWriter;

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
            writer = new CSVWriter(new OutputStreamWriter(fos, Charset.forName("UTF-8")), this.SEPARATOR, CSVWriter.DEFAULT_QUOTE_CHARACTER);

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

            //retrieve all headers
            Map<String, String> lineData = getAllHeaders(source);

            // For each of the records in the array
            int line = 1;
            while (currentToken != JsonToken.END_ARRAY) {
                line++;
                // read the record into a tree model,
                // this moves the parsing position to the end of it
                JsonNode node = jp.readValueAsTree();

                for (Iterator<String> fields = node.fieldNames(); fields.hasNext();) {

                    String field = fields.next();
                    lineData.put(field, node.get(field).asText());
                }

                //writer to output file
                //write header if firstRun
                if (firstRun) {
                    writer.writeNext(lineData.keySet().toArray(new String[0]));
                    firstRun = false;
                }
                writer.writeNext(lineData.values().toArray(new String[0]));
                //reset the line
                for (Map.Entry<String, String> entry : lineData.entrySet()) {
                    entry.setValue(null);
                }
                currentToken = jp.nextToken();
            }

        } catch (IOException ex) {
            throw new Exception("Error writing metadata file: " + source.getName());
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

    /**
     * Iterates through whole file to find all possible headers
     *
     * @param jsonFile - json file to parse
     * @return Map of all possible headers as keys and empty values
     * @throws Exception
     */
    private Map<String, String> getAllHeaders(File jsonFile) throws Exception {
        try {
            Map<String, String> headers = new LinkedHashMap<String, String>();

            FileInputStream fis = null;
            BufferedReader rd = null;
            JsonFactory f = new MappingJsonFactory();
            fis = new FileInputStream(jsonFile);
            rd = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            JsonParser jp = f.createParser(rd);
            JsonToken currentToken;
            currentToken = jp.nextToken();
            currentToken = jp.nextToken();

            while (currentToken != JsonToken.END_ARRAY) {

                JsonNode node = jp.readValueAsTree();

                for (Iterator<String> fields = node.fieldNames(); fields.hasNext();) {
                    String field = fields.next();
                    //add header,if not already exist
                    headers.put(field, null);
                }
                currentToken = jp.nextToken();
            }

            return headers;

        } catch (FileNotFoundException ex) {
            throw new Exception("Error reading metadata file: " + jsonFile.getName() + " " + ex.getMessage());
        } catch (IOException ex) {
            throw new Exception("Error reading metadata file: " + jsonFile.getName() + " " + ex.getMessage());
        }
    }
}
