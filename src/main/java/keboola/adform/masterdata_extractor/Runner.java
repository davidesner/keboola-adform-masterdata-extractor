/*
 */
package keboola.adform.masterdata_extractor;

import com.google.common.io.Files;
import keboola.adform.masterdata_extractor.api_client.ClientException;
import keboola.adform.masterdata_extractor.config.KBCConfig;
import keboola.adform.masterdata_extractor.config.YamlConfigParser;
import keboola.adform.masterdata_extractor.filemerger.CsvFileMerger;
import keboola.adform.masterdata_extractor.filemerger.MergeException;
import keboola.adform.masterdata_extractor.utils.FileHandler;
import keboola.adform.masterdata_extractor.pojo.MasterFile;
import keboola.adform.masterdata_extractor.pojo.MasterFileList;
import keboola.adform.masterdata_extractor.utils.JsonToCsvConvertor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class Runner {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.print("No parameters provided.");
            System.exit(1);
        }
        String dataPath = args[0];
        String outTablesPath = dataPath + File.separator + "out" + File.separator + "tables"; //parse config
        KBCConfig config = null;
        File confFile = new File(args[0] + File.separator + "config.yml");
        if (!confFile.exists()) {
            System.out.println("config.yml does not exist!");
            System.err.println("config.yml does not exist!");
            System.exit(1);
        }
        //Parse config file
        try {
            if (confFile.exists() && !confFile.isDirectory()) {
                config = YamlConfigParser.parseFile(confFile);
            }
        } catch (Exception ex) {
            System.out.println("Failed to parse config file");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        if (!config.validate()) {
            System.out.println(config.getValidationError());
            System.err.println(config.getValidationError());
            System.exit(1);
        }

        Extractor ex = new Extractor(config.getParams().getUser(), config.getParams().getPass(), config.getParams().getMdListUrl());
        File dataFolder = new File(dataPath);
        try {
            //authenticate, get session token
            ex.client.authenticate();
        } catch (ClientException ex1) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex1);
            System.out.println("Error authenticating connection to API.");
            System.err.println(ex1.getMessage());
            System.exit(2);
        }

        //get list of files
        MasterFileList fileList = null;
        try {
            fileList = ex.client.retrieveFileList();
        } catch (Exception ex1) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex1);
            System.out.println("Error retrieving list of masterdata files.");
            System.err.println(ex1.getMessage());
            System.exit(2);
        }

        //download files in specified interval
        Calendar c = Calendar.getInstance();
        Date startInterval = null;
        if (config.getParams().getDate_to() != null) {
            c.setTime(config.getParams().getDate_to());
            c.add(Calendar.DATE, -config.getParams().getDaysInterval());
            startInterval = c.getTime();

        } else {
            c.add(Calendar.DATE, -config.getParams().getDaysInterval());
            startInterval = c.getTime();

        }
        int i = 0;
        try {
            for (String prefix : config.getParams().getPrefixes()) {
                //get sublist of files in given interval and for given prefix
                List<MasterFile> filesSince = null;
                if (config.getParams().getDate_to() != null) {
                    filesSince = fileList.getFilesSince(startInterval, config.getParams().getDate_to(), prefix);
                } else {
                    filesSince = fileList.getFilesSince(startInterval, prefix);
                }
                if (filesSince == null) {
                    continue;
                }
                System.out.println("Downloading files with prefix: " + prefix);
                List<String> csvFilesPaths = ex.downloadAndUnzip(filesSince, dataPath);

                //merge downloaded files
                String resFileName = config.getParams().getBucket() + "." + prefix.toLowerCase();
                System.out.println("Merging files with prefix: " + prefix);
                CsvFileMerger.mergeFiles(csvFilesPaths, outTablesPath, resFileName + ".csv");

                /*Build manifest file*/
                String manifest = /*"destination: " + resFileName + "\n"
                        + "incremental: true\n"
                        + */ "delimiter: \"\\t\"\n"
                        + "enclosure: \"\"\n"
                        + "escaped_by: \"\\\\\"";
                File manifestFile = new File(outTablesPath + File.separator + resFileName + ".csv.manifest");
                try {
                    Files.write(manifest, manifestFile, Charset.forName("UTF-8"));
                } catch (IOException ex1) {
                    System.out.println("Error writing manifest file." + ex1.getMessage());
                    System.err.println(ex1.getMessage());
                    System.exit(2);
                }
                //delete single csv files
                try {
                    FileHandler.deleteFiles(csvFilesPaths);
                } catch (IOException ex1) {

                    System.out.println("Error deleting single csv files.");
                }
                i++;
            }

            /*Download metadata files*/
            boolean metaChanged;
            if (config.getParams().getDate_to() != null) {
                metaChanged = fileList.metaChangedSince(startInterval, config.getParams().getDate_to());
            } else {
                metaChanged = fileList.metaChangedSince(startInterval);
            }
            if (config.hasMeta() && metaChanged) {
                System.out.println("Downloading meta files");
                List<String> metaFilesPaths = ex.downloadAndUnzip(Arrays.asList(fileList.getMeta()), dataPath);

                /*Convert from JSON to csv*/
                JsonToCsvConvertor conv = new JsonToCsvConvertor();
                String metaFolder = new File(metaFilesPaths.get(0)).getParent();
                for (String metaF : config.getParams().getMetaFiles()) {
                    String resFileName = config.getParams().getBucket() + "." + "meta-" + metaF;
                    try {
                        System.out.println("Converting meta file: " + metaF + " to CSV");
                        conv.convert(metaFolder + File.separator + metaF + ".json", outTablesPath + File.separator + resFileName + ".csv");
                    } catch (Exception ex1) {
                        System.out.print("Error converting meta data file to csv.");
                        System.err.print(ex1.getMessage());
                        System.exit(1);
                    }
                    /*Build manifest file*/
                    String manifest = /*"destination: " + resFileName + "\n"
                        + "incremental: true\n"
                        + */ "delimiter: \"\\t\"\n"
                            + "enclosure: \"\"\n"
                            + "escaped_by: \"\\\\\"";
                    File manifestFile = new File(outTablesPath + File.separator + resFileName + ".csv.manifest");
                    try {
                        Files.write(manifest, manifestFile, Charset.forName("UTF-8"));
                    } catch (IOException ex1) {
                        System.out.println("Error writing manifest file." + ex1.getMessage());
                        System.err.println(ex1.getMessage());
                        System.exit(2);
                    }
                }

                /*delete original JSON files*/
                try {
                    FileHandler.deleteFiles(metaFilesPaths);
                } catch (IOException ex1) {
                    System.out.println("Error deleting original meta files. " + ex1.getMessage());
                }
            }

            System.out.println("Files extracted successfully..");
            System.exit(0);
        } catch (ExtractorException ex1) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex1);
            System.out.print("Error extracting data.");
            System.err.print(ex1.getMessage());
            System.exit(2);
        } catch (MergeException ex1) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex1);
            System.out.print("Error merging data.");
            System.err.print(ex1.getMessage());
            System.exit(2);
        }
    }
}
