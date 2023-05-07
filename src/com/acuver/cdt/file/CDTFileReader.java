package com.acuver.cdt.file;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.stream.Collectors;

public class CDTFileReader {
	public void readPropertiesFile() throws Exception {

		File enhancedcdtfile = new File(CDTConstants.currentDirectory + "/enhancedcdt.properties");
		Properties prop = null;

		CDTHelper helper = new CDTHelper();
		if (!enhancedcdtfile.exists()) {
			// Display message and exit if config file does not exist
			helper.showPropertiesFileHelpMsg();
			System.exit(1);
		}

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(enhancedcdtfile.getPath());

			prop = new Properties();

			prop.load(fis);

		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			fis.close();
		}

		System.out.println("Properties : " + "\n" + prop + "\n");

		EnhancedCDTMain.CDT_REPORT_DIR1 = prop.getProperty(CDTConstants.CDT_REPORT_DIR1);
		EnhancedCDTMain.CDT_REPORT_DIR2 = prop.getProperty(CDTConstants.CDT_REPORT_DIR2);
		EnhancedCDTMain.CDT_XMLS1 = prop.getProperty(CDTConstants.CDT_XMLS1);
		EnhancedCDTMain.CDT_XMLS2 = prop.getProperty(CDTConstants.CDT_XMLS2);
		EnhancedCDTMain.OUTPUT_DIR = prop.getProperty(CDTConstants.OUTPUT_DIR);
		

		if (EnhancedCDTMain.CDT_REPORT_DIR1 == null && EnhancedCDTMain.CDT_REPORT_DIR1.isEmpty()
				|| EnhancedCDTMain.CDT_REPORT_DIR2 == null && EnhancedCDTMain.CDT_REPORT_DIR2.isEmpty()
				|| EnhancedCDTMain.CDT_XMLS1 == null && EnhancedCDTMain.CDT_XMLS1.isEmpty()
				|| EnhancedCDTMain.CDT_XMLS2 == null && EnhancedCDTMain.CDT_XMLS2.isEmpty()) {
			helper.showPropertiesFileHelpMsg();
			System.exit(1);
		}

	}

	public void populateRecordIdentifier()
	{
		Properties properties = new Properties();

		try {
			properties.load( new FileInputStream("recordIdentifier.config"));
		} catch (Exception e) {

		}
		EnhancedCDTMain.recordIdentifierMap.putAll(properties.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(),
						e -> e.getValue().toString())));
	}

	public ArrayList<String> readYDKPrefs(String ydkperfs)
	{
		return new ArrayList<String>();
	}

	public File[] readFilesFromDir(String directory) {
		// Creating a File object for directory
		File directoryPath = new File(directory);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		return filesList;
	}

	// Read File From Directory
	public String readFileFromDir(String directory, String fileName) {
		// Creating a File object for directory
		File directoryPath = new File(directory);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		String fileData = "";
		if (filesList != null && filesList.length > 0) {
			for (File file : filesList) {
				if (file != null && file.length() > 0) {
					if (file.getName().startsWith(fileName)) {
						fileData = file.toString();
					}
				}
			}
		}
		return fileData;
	}
}