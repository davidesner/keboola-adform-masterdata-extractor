/*
 */
package keboola.adform.masterdata_extractor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import keboola.adform.masterdata_extractor.api_client.ClientException;
import keboola.adform.masterdata_extractor.config.JsonConfigParser;
import keboola.adform.masterdata_extractor.config.KBCConfig;
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

	private static final String MD_PRIMARY_KEY = "GUID";
	final static Logger log = LogManager.getLogger(Runner.class);

    public static void main(String[] args) {

    	printEnvStats();
        if (args.length == 0) {
            System.out.print("No parameters provided.");
            System.exit(1);
        }
        String dataPath = args[0];
        String outTablesPath = dataPath + File.separator + "out" + File.separator + "tables"; //parse config
        KBCConfig config = null;
        File confFile = new File(args[0] + File.separator + "config.json");
        if (!confFile.exists()) {
            System.out.println("config.json does not exist!");
            System.err.println("config.json does not exist!");
            System.exit(1);
        }
        //Parse config file
        try {
            if (confFile.exists() && !confFile.isDirectory()) {
                config = JsonConfigParser.parseFile(confFile);
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
        Extractor ex = new Extractor(config.getParams().getUser(), config.getParams().getPass(), config.getParams().getMdListId(), log);
        try {
            //authenticate, get session token
            ex.client.authenticate();
        } catch (ClientException ex1) {
            System.out.println("Error authenticating connection to API.");
            System.err.println(ex1.getMessage());
            System.exit(1);
        }

        //get list of files
        MasterFileList fileList = null;
        try {
            fileList = ex.client.retrieveFileList();
        } catch (Exception ex1) {
            System.out.println("Error retrieving list of masterdata files.");
            System.err.println(ex1.getMessage());
            System.exit(1);
        }

        //download files in specified interval
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date startInterval = null;
        if (config.getParams().getDate_to() != null) {
            c.setTime(config.getParams().getDate_to());
            c.add(Calendar.DATE, -config.getParams().getDaysInterval());
            c.add(Calendar.HOUR, -config.getParams().getHoursInterval());
            startInterval = c.getTime();

        } else {
            c.add(Calendar.DATE, -config.getParams().getDaysInterval());
            c.add(Calendar.HOUR, -config.getParams().getHoursInterval());
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
                	log.warn("No new files found for " + prefix);
                    continue;
                }
                //sort from oldest
                Collections.sort(filesSince, Collections.reverseOrder());
                System.out.println("Downloading files with prefix: " + prefix + " Since: " + c.getTime() + " Files found: " + filesSince.size());
                String resFileFolder = outTablesPath + File.separator + prefix.toLowerCase() + ".csv";
                List<MasterFile> downloadedFiles = ex.downloadAndUnzip(filesSince, resFileFolder);

//                /*This should not happen, check anyway*/
//                if (downloadedFiles.isEmpty()) {
//                    System.out.print("Error downloading files with prefix: " + prefix);
//                    System.err.print("Error downloading files with prefix: " + prefix);
//                    System.exit(1);
//                }
//
//                //merge downloaded files
//                String resFileName = prefix.toLowerCase();
//                System.out.println("Preparing sliced tables...");
//                String[] headerCols = null;
//				try {
//					headerCols = prepareSlicedTables(downloadedFiles, config.getParams().getSrcCharset());
//				} catch (Exception e) {
//					System.err.println("Error processing files." + e.getMessage());
//					System.exit(2);
//				}
//
//                /*Build manifest file*/
//                try {
//                	String[] pkey = null;
//                	if (!config.getParams().getKeyMap().containsKey(prefix)) {
//                		pkey = new String[] {MD_PRIMARY_KEY};
//                	} else {
//                		pkey = config.getParams().getKeyMap().get(prefix);
//                	}
//                	buildManifestFile(resFileName,  config.getParams().getBucket(), outTablesPath, headerCols, pkey, true);
//                } catch (Exception ex1) {
//                    System.out.println("Error writing manifest file." + ex1.getMessage());
//                    System.err.println(ex1.getMessage());
//                    System.exit(2);
//                }
//                i++;
//                dataExtracted = true;
//            }
//
//
//            if (config.hasMeta()) {
//                System.out.println("Downloading meta files");
//                List<MasterFile> filesSince = null;
//				if (config.getParams().isAlwaysGetMeta()) {
//					filesSince = fileList.getFilesByPrefix("meta");
//				} else if (config.getParams().getDate_to() != null) {
//					filesSince = fileList.getFilesSince(startInterval, config.getParams().getDate_to(), "meta");
//				} else {
//					filesSince = fileList.getFilesSince(startInterval, "meta");
//				}
//
//
//                List<MasterFile> metaFiles = ex.downloadAndUnzip(filesSince, dataPath);
//
//                dataExtracted = processMetaDataFiles(metaFiles, config, dataPath, outTablesPath);

            }

//            if (dataExtracted && i > 0) {
//                System.out.println("Files extracted successfully..");
//            } else if (!dataExtracted) {
//                System.out.println("Proccess finished successfully but no meta files were extracted. Check configuration parameters.");
//            } else {
//                System.out.println("Proccess finished successfully but only metadata tables were extracted. Check configuration parameters.");
//            }
//            System.exit(0);
        } catch (ExtractorException ex1) {
            System.out.print("Error extracting data.");
            System.err.print(ex1.getMessage());
            System.exit(ex1.getSeverity());
        }
    }

    private static boolean processMetaDataFiles(List<MasterFile> metaFiles, KBCConfig config, String dataPath, String outTablesPath) {
    	if (metaFiles.isEmpty()) {
        	log.warn("No new metadata were retrieved!");
    		return false;
    	}
        /*Convert from JSON to csv*/
        JsonToCsvConvertor conv = new JsonToCsvConvertor();
        for (String metaF : config.getParams().getMetaFiles()) {
            String resFileName = "meta-" + metaF;
            File jsonFile = new File(dataPath + File.separator + metaF + ".json");
            if (!jsonFile.exists()) {
            	log.warn(metaF + " metadata file does not exist in the source!");
            	continue;
            }
            boolean notEmpty = false;
            try {
                System.out.println("Converting meta file: " + metaF + " to CSV");
                notEmpty = conv.convert(dataPath + File.separator + metaF + ".json", outTablesPath + File.separator + resFileName + ".csv");
            } catch (Exception ex1) {
                System.out.print("Error converting meta data file to csv.");
                System.err.print(ex1.getMessage());
                System.exit(1);
            }
            /*Build manifest file,not incremental*/
            if (notEmpty){
	            try {
	            	buildManifestFile(resFileName, config.getParams().getBucket(), outTablesPath, null, null, false);
	            } catch (Exception ex1) {
	                System.out.println("Error writing manifest file." + ex1.getMessage());
	                System.err.println(ex1.getMessage());
	                System.exit(2);
	            }
            }
        }
        //delete temp files
        for (MasterFile file : metaFiles) {
        	try {
				FileHandler.deleteFile(file.getLocalAbsolutePath());
			} catch (IOException e) {
				log.warn("Error deleting original metafile.", e);
			}
        }
       return true;
    }

    private static void buildManifestFile(String resFileName, String destination, String outPath, String[] cols, String[] pkey, boolean incremental) throws Exception {
        // remove BOM because some files contain it and KBC is incapable of handling it
        if (cols != null) {
            System.out.println(cols[0]);
            cols[0] = CsvUtils.removeUTF8BOM(cols[0]);
        }

        System.out.println(Arrays.toString(cols));
        ManifestFile.Builder builder = new ManifestFile.Builder(resFileName, destination + "." + resFileName)
                .setIncrementalLoad(incremental).setDelimiter(String.valueOf(DEFAULT_SEPARATOR)).setEnclosure(String.valueOf(DEFAULT_ENCLOSURE))
                .setColumns(cols);
        if (pkey != null) {
            builder.setPrimaryKey(pkey);
        }
        ManifestFile manFile = builder.build();
        ManifestBuilder.buildManifestFile(manFile, outPath, resFileName + ".csv");
    }

	private static String[] prepareSlicedTables(List<MasterFile> downloadedFiles, String charset) throws Exception {
		List<File> files = new ArrayList<>();
		for(MasterFile mf : downloadedFiles) {
			files.add(new File(mf.getLocalAbsolutePath()));
		}

		// get colums
				String[] headerCols = CsvUtils.readHeader(files.get(0),
						DEFAULT_SEPARATOR, DEFAULT_ENCLOSURE, DEFAULT_ESCAPE_CHAR, false, false, Charset.forName(charset));
				// remove headers and create results
				for (File mFile : files) {
					CsvUtils.removeHeaderFromCsv(mFile, Charset.forName(charset));
				}
				//in case some files did not contain any data
				CsvUtils.deleteEmptyFiles(files);
				return headerCols;

	}

	private static void printEnvStats() {
		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory();

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		 // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
		long heapFreeSize = Runtime.getRuntime().freeMemory();

		log.info("Initial Heap size (MB): " + heapSize/1000000);
		log.info("Max Heap size (MB): " + heapMaxSize/1000000);
		log.info("Initial free memory (MB): " + heapFreeSize/1000000);
	}
}
