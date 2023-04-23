package com.acuver.cdt.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.xml.CDTXmlComparator;

public class CDTFileReader {

	public Properties readPropertiesFile(String filePath) throws Exception {
		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(filePath);

			prop = new Properties();

			prop.load(fis);

		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			fis.close();
		}
		return prop;
	}

	public void readPropertiesFile1(String propertiesFilePath) throws Exception {
		File enhancedcdtfile = new File(propertiesFilePath);

		CDTHelper helper = new CDTHelper();
		if (!enhancedcdtfile.exists()) {
			// Display message and exit if config file does not exist
			helper.formPropertiesFileHelpMsg();
			System.exit(1);
		}

		EnhancedCDTMain.prop = readPropertiesFile(enhancedcdtfile.getPath());
		System.out.println("Properties : " + "\n" + EnhancedCDTMain.prop + "\n");

		EnhancedCDTMain.CDT_REPORT_DIR1 = EnhancedCDTMain.prop.getProperty("CDT_REPORT_DIR1");
		EnhancedCDTMain.CDT_REPORT_DIR2 = EnhancedCDTMain.prop.getProperty("CDT_REPORT_DIR2");
		EnhancedCDTMain.CDT_XMLS1 = EnhancedCDTMain.prop.getProperty("CDT_XMLS1");
		EnhancedCDTMain.CDT_XMLS2 = EnhancedCDTMain.prop.getProperty("CDT_XMLS2");

	}

	public File[] readFilesFromDir(String directory) {
		// Creating a File object for directory
		File directoryPath = new File(directory);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		return filesList;
	}

}
