/*
 */
package keboola.adform.masterdata_extractor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import keboola.adform.masterdata_extractor.api_client.ClientException;
import keboola.adform.masterdata_extractor.config.KBCConfig;
import keboola.adform.masterdata_extractor.config.YamlConfigParser;
import keboola.adform.masterdata_extractor.config.tableconfig.ManifestBuilder;
import keboola.adform.masterdata_extractor.config.tableconfig.ManifestFile;
import keboola.adform.masterdata_extractor.pojo.MasterFile;
import keboola.adform.masterdata_extractor.pojo.MasterFileList;
import keboola.adform.masterdata_extractor.utils.CsvUtils;
import keboola.adform.masterdata_extractor.utils.FileHandler;
import keboola.adform.masterdata_extractor.utils.JsonToCsvConvertor;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class Runner {

	private static char DEFAULT_SEPARATOR = '\t';
	private static char DEFAULT_ENCLOSURE = '"';
	private static char DEFAULT_ESCAPE_CHAR = '\\';

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
        boolean dataExtracted = false;
        Extractor ex = new Extractor(config.getParams().getUser(), config.getParams().getPass(), config.getParams().getMdListUrl());      
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
                if (filesSince.isEmpty()) {
                    continue;
                }
                //sort from oldest
                Collections.sort(filesSince, Collections.reverseOrder());
                System.out.println("Downloading files with prefix: " + prefix);
                String resFileFolder = outTablesPath + File.separator + prefix.toLowerCase() + ".csv";
                List<MasterFile> downloadedFiles = ex.downloadAndUnzip(filesSince, resFileFolder);

                /*This should not happen, check anyway*/
                if (downloadedFiles.isEmpty()) {
                    System.out.print("Error downloading files with prefix: " + prefix);
                    System.err.print("Error downloading files with prefix: " + prefix);
                    System.exit(1);
                }

                //merge downloaded files
                String resFileName = prefix.toLowerCase();
                System.out.println("Preparing sliced tables...");
                String[] headerCols = null;
				try {
					headerCols = prepareSlicedTables(downloadedFiles);
				} catch (Exception e) {
					System.err.println("Error processing files." + e.getMessage());
					System.exit(2);
				}

                /*Build manifest file*/               
                try {
                	buildManifestFile(resFileName,  config.getParams().getBucket(), outTablesPath, headerCols, true);
                } catch (Exception ex1) {
                    System.out.println("Error writing manifest file." + ex1.getMessage());
                    System.err.println(ex1.getMessage());
                    System.exit(2);
                }               
                i++;
                dataExtracted = true;
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
                List<MasterFile> metaFiles = ex.downloadAndUnzip(Arrays.asList(fileList.getMeta()), dataPath);

                List<String> metaFilesPaths = new ArrayList<String>();
                for (MasterFile f : metaFiles) {
                    metaFilesPaths.add(f.getLocalAbsolutePath());
                }

                /*Convert from JSON to csv*/
                JsonToCsvConvertor conv = new JsonToCsvConvertor();
                String metaFolder = new File(metaFilesPaths.get(0)).getParent();
                for (String metaF : config.getParams().getMetaFiles()) {
                    String resFileName = "meta-" + metaF;
                    try {
                        System.out.println("Converting meta file: " + metaF + " to CSV");
                        conv.convert(metaFolder + File.separator + metaF + ".json", outTablesPath + File.separator + resFileName + ".csv");
                    } catch (Exception ex1) {
                        System.out.print("Error converting meta data file to csv.");
                        System.err.print(ex1.getMessage());
                        System.exit(1);
                    }
                    /*Build manifest file,not incremental*/
                    try {
                    	buildManifestFile(resFileName, config.getParams().getBucket(), outTablesPath, null, false);
                    } catch (Exception ex1) {
                        System.out.println("Error writing manifest file." + ex1.getMessage());
                        System.err.println(ex1.getMessage());
                        System.exit(2);
                    }
                }
                dataExtracted = true;
                /*delete original JSON files*/
                try {
                    FileHandler.deleteFiles(metaFilesPaths);
                } catch (IOException ex1) {
                    System.out.println("Error deleting original meta files. " + ex1.getMessage());
                }
            }
            if (dataExtracted && i > 0) {
                System.out.println("Files extracted successfully..");
            } else if (!dataExtracted) {
                System.out.println("Proccess finished successfully but no files were extracted. Check configuration parameters.");
            } else {
                System.out.println("Proccess finished successfully but only metadata tables were extracted. Check configuration parameters.");
            }
            System.exit(0);
        } catch (ExtractorException ex1) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex1);
            System.out.print("Error extracting data.");
            System.err.print(ex1.getMessage());
            System.exit(ex1.getSeverity());
        }
    }

    private static void buildManifestFile(String resFileName, String destination, String outPath, String [] cols, boolean incremental) throws Exception {
    	ManifestFile manFile = new ManifestFile.Builder(resFileName, destination + "." + resFileName)
				.setIncrementalLoad(incremental).setDelimiter(String.valueOf(DEFAULT_SEPARATOR)).setEnclosure(String.valueOf(DEFAULT_ENCLOSURE))
				.setColumns(cols).build();
		ManifestBuilder.buildManifestFile(manFile, outPath, resFileName + ".csv");	
    }

	private static String[] prepareSlicedTables(List<MasterFile> downloadedFiles) throws Exception {
		List<File> files = new ArrayList<>();
		for(MasterFile mf : downloadedFiles) {
			files.add(new File(mf.getLocalAbsolutePath()));
		}
		
		// get colums
				String[] headerCols = CsvUtils.readHeader(files.get(0),
						DEFAULT_SEPARATOR, DEFAULT_ENCLOSURE, DEFAULT_ESCAPE_CHAR, false, false);
				// remove headers and create results
				for (File mFile : files) {
					CsvUtils.removeHeaderFromCsv(mFile);					
				}
				//in case some files did not contain any data
				CsvUtils.deleteEmptyFiles(files);				
				return headerCols;
		
	}
}
