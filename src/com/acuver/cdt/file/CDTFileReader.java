package com.acuver.cdt.file;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

public class CDTFileReader {
	public void readPropertiesFile() throws FileNotFoundException, IOException {

		File enhancedcdtfile = new File(CDTConstants.currentDirectory + File.separator + "enhancedcdt.properties");
		Properties prop = null;

		if (!enhancedcdtfile.exists()) {
			// Display message and exit if config file does not exist
			CDTHelper.showPropertiesFileHelpMsg();
			System.exit(1);
		}

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(enhancedcdtfile.getPath());

			prop = new Properties();

			prop.load(fis);
		} finally {

			if (fis != null) {
			fis.close();
		}
		}

		EnhancedCDTMain.CDT_REPORT_DIR1 = prop.getProperty(CDTConstants.CDT_REPORT_DIR1);
		EnhancedCDTMain.CDT_REPORT_DIR2 = prop.getProperty(CDTConstants.CDT_REPORT_DIR2);
		EnhancedCDTMain.CDT_XMLS1 = prop.getProperty(CDTConstants.CDT_XMLS1);
		EnhancedCDTMain.CDT_XMLS2 = prop.getProperty(CDTConstants.CDT_XMLS2);
		EnhancedCDTMain.OUTPUT_DIR = prop.getProperty(CDTConstants.OUTPUT_DIR);
		EnhancedCDTMain.YDKPREF1 = prop.getProperty(CDTConstants.YDKPREF1);
		EnhancedCDTMain.YDKPREF2 = prop.getProperty(CDTConstants.YDKPREF2);
		EnhancedCDTMain.CDT_REPORT_OUT_DIR1 = prop.getProperty(CDTConstants.CDT_REPORT_OUT_DIR1);
		EnhancedCDTMain.CDT_REPORT_OUT_DIR2 = prop.getProperty(CDTConstants.CDT_REPORT_OUT_DIR2);

		if (EnhancedCDTMain.CDT_REPORT_DIR1 != null && EnhancedCDTMain.CDT_REPORT_DIR1.trim().isEmpty()
				|| EnhancedCDTMain.CDT_REPORT_DIR2 != null && EnhancedCDTMain.CDT_REPORT_DIR2.trim().isEmpty()
				|| EnhancedCDTMain.CDT_XMLS1 != null && EnhancedCDTMain.CDT_XMLS1.trim().isEmpty()
				|| EnhancedCDTMain.CDT_XMLS2 != null && EnhancedCDTMain.CDT_XMLS2.trim().isEmpty()) {
			CDTHelper.showPropertiesFileHelpMsg();
			System.exit(1);
		}

	}

	public void populateRecordIdentifier() {
		Properties properties = new Properties();

		try {
			properties.load(
					new FileInputStream(CDTConstants.currentDirectory + File.separator + "recordIdentifier.config"));
		} catch (Exception e) {
		}
		EnhancedCDTMain.recordIdentifierMap = new HashMap<String, String>();
		EnhancedCDTMain.recordIdentifierMap.putAll(properties.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString())));
	}

	public ArrayList<String> readYDKPrefs(String ydkperfs) throws Exception {

		ArrayList<String> tableNamesList = new ArrayList<String>();
		File file = new File(ydkperfs);
				if (file != null && file.length() > 0) {
					DocumentBuilder db = null;
					db = EnhancedCDTMain.factory.newDocumentBuilder();
					Document doc = null;
					doc = db.parse(file);
			String expression = CDTConstants.forwardSlash + CDTConstants.ignore + CDTConstants.forwardSlash
					+ CDTConstants.table;
					NodeList nodeList = null;
					nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
							XPathConstants.NODESET);
					for (int itr = 0; itr < nodeList.getLength(); itr++) {
						Element tableElement = (Element) nodeList.item(itr);
						String tableName = tableElement.getAttribute("Name");
						if (tableName != null && !tableName.isEmpty()) {
					tableName = tableName + CDTConstants.xmlExtension;
							tableNamesList.add(tableName);
						}
					}
			}
		return tableNamesList;
	}

	public File[] readFilesFromDir(String directory) {
		// Creating a File object for directory
		File directoryPath = new File(directory);
		// List of all files and directories
		File filesList[] = directoryPath.listFiles();
		return filesList;
	}

	// Read File From Directory
    public Document readFileFromDir(String directory, String fileName) throws Exception {
        // Creating a File object for directory
        File file = new File(directory + File.separator + fileName );
		if (file != null && file.length() > 0) {
            DocumentBuilder documentBuilder = EnhancedCDTMain.factory.newDocumentBuilder();
            return documentBuilder.parse(file);
        }
        return null;
    }
}