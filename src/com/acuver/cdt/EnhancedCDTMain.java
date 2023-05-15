package com.acuver.cdt;

import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.util.CDTHelper;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.xml.CDTXmlComparator;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class EnhancedCDTMain {

	public static XPath xPath = XPathFactory.newInstance().newXPath();
	public static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	public static TransformerFactory tf = TransformerFactory.newInstance();
	// properties
	public static String CDT_REPORT_DIR1 = null;
	public static String CDT_REPORT_DIR2 = null;
	public static String CDT_XMLS1 = null;
	public static String CDT_XMLS2 = null;
	public static String OUTPUT_DIR = null;
	public static String YDKPREF1 = null;
	public static String YDKPREF2 = null;
	public static ArrayList<String> ydkPerf1IgnoreTables = null;
	public static ArrayList<String> ydkPerf2IgnoreTables = null;
	// merged reports directories
	
	public static String CDT_REPORT_OUT_DIR1 = null; 
	public static String CDT_REPORT_OUT_DIR2 = null;
	public static Map<String, String> recordIdentifierMap;

	public static void main(String[] argMode) throws Exception {
		try {
			factory.setIgnoringElementContentWhitespace(true);
			CDTFileReader fileReader = new CDTFileReader();
			CDTFileWriter fileWriter = new CDTFileWriter();

			String mode = argMode.length > 0 ? argMode[0] : "--merge";

			switch (mode) {
			case "--help":
				CDTHelper.showPropertiesFileHelpMsg();
				break;
			case "--merge":
				 fileReader.readPropertiesFile();
				 fileReader.populateRecordIdentifier();
				mergeCDTReports(fileReader, fileWriter);
			//	fileWriter.readDataFromExcelSheet();
				break;
			case "--mergeManualReview":
				fileReader.readPropertiesFile();
				System.out.println("CDT_REPORT_OUT_DIR1 : "+CDT_REPORT_OUT_DIR1);
				System.out.println("CDT_REPORT_OUT_DIR2 : "+CDT_REPORT_OUT_DIR2);
				fileWriter.mergeAfterReview(CDT_REPORT_OUT_DIR1);
				//fileWriter.mergeAfterReview(CDT_REPORT_OUT_DIR2);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void mergeCDTReports(CDTFileReader fileReader, CDTFileWriter fileWriter) throws Exception {
		try {
			
			if (YDKPREF1 != null && !YDKPREF1.trim().isEmpty()) {
				ydkPerf1IgnoreTables = fileReader.readYDKPrefs(YDKPREF1);
			}

			File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR1);
			if (filesList == null) {
				System.out.println("No files found in directory: " + CDT_REPORT_DIR1);
			}
			if (filesList != null && filesList.length > 0) {
				CDT_REPORT_OUT_DIR1 = fileWriter.createOutDir(OUTPUT_DIR);

				for (File f : filesList) {
					processFile(f, CDT_REPORT_OUT_DIR1, fileReader, fileWriter);
				}

			}

		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private static void processFile(File f, String outDir, CDTFileReader fileReader, CDTFileWriter fileWriter)
			throws Exception {
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		
		if(f.isFile()) {
		  
		if (f != null && f.length() > 0) {
			System.out.println("The files in the CDT_REPORT_DIR1 are " + f.getName());
			String fileName = f.getName();

			try {
				if (ydkPerf1IgnoreTables == null || !(ydkPerf1IgnoreTables.contains(fileName))) {
					System.out.println(
							"YDKPREF1 Table Names List is Empty or YDKPREF1 Table Names List does not contains the File Name : "
									+ fileName);
					if (fileName.startsWith("YFS") || fileName.startsWith("PLT")) {
						CDTXmlComparator xmlComparator = new CDTXmlComparator();
						xmlComparator.setInputDoc(documentBuilder.parse(f));
						xmlComparator.setOutDir(outDir);
						xmlComparator.setFileReader(fileReader);
						xmlComparator.setFileWriter(fileWriter);
						Document outputDoc = xmlComparator.merge();
						fileWriter.writeFile(outDir, outputDoc, fileName);
					} else {
						try {
							fileWriter.copyFileToDirectory(f, outDir);
						} catch (IOException e) {
							System.out.println("An error occurred while copying the file.");
							e.printStackTrace();
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	}
}