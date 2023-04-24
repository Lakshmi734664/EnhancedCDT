package com.acuver.cdt.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import com.acuver.cdt.constants.*;

public class CDTFileReader {

	public Properties readPropertiesFile1(String propertiesFilePath) throws Exception {
		File enhancedcdtfile = new File(propertiesFilePath);

		Properties prop = null;
		CDTHelper helper = new CDTHelper();
		if (!enhancedcdtfile.exists()) {
			// Display message and exit if config file does not exist
			helper.formPropertiesFileHelpMsg();
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

		String CDT_REPORT_DIR1 = prop.getProperty(CDTConstants.CDT_REPORT_DIR1);
		String CDT_REPORT_DIR2 = prop.getProperty(CDTConstants.CDT_REPORT_DIR2);
		String CDT_XMLS1 = prop.getProperty(CDTConstants.CDT_XMLS1);
		String CDT_XMLS2 = prop.getProperty(CDTConstants.CDT_XMLS2);

		if (CDT_REPORT_DIR1 == null && CDT_REPORT_DIR1.isEmpty()) {
			helper.formPropertiesFileHelpMsg();
			System.exit(1);
		}

		if (CDT_REPORT_DIR2 == null && CDT_REPORT_DIR2.isEmpty()) {
			helper.formPropertiesFileHelpMsg();
			System.exit(1);
		}

		if (CDT_XMLS1 == null && CDT_XMLS1.isEmpty()) {
			helper.formPropertiesFileHelpMsg();
			System.exit(1);
		}

		if (CDT_XMLS2 == null && CDT_XMLS2.isEmpty()) {
			helper.formPropertiesFileHelpMsg();
			System.exit(1);
		}

		return prop;

	}

	public File[] readFilesFromDir(String directory) {
		// Creating a File object for directory
		File directoryPath = new File(directory);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		return filesList;
	}

}
