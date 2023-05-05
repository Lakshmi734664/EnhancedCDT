package com.acuver.cdt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.acuver.cdt.constants.CDTConstants;
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.xml.CDTXmlComparator;

public class EnhancedCDTMain {

	public static XPath xPath = XPathFactory.newInstance().newXPath();
	/* Create DOM Parser */
	// Create a new DocumentBuilderFactory
	public static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	public static TransformerFactory tf = TransformerFactory.newInstance();

	public static String CDT_REPORT_DIR1 = null;
	public static String CDT_REPORT_DIR2 = null;
	public static String CDT_XMLS1 = null;
	public static String CDT_XMLS2 = null;
	public static String OUTPUT_DIR = null;
	public static String YDKPREF1 = null;
	public static String YDKPREF2 = null;
	public static String CDT_REPORT_DIR1_OUT = null;
	public static String CDT_REPORT_DIR2_OUT = null;

	public static void main(String[] mode) throws Exception {

		Document outputDoc = null;

		try {

			CDTFileReader fileReader = new CDTFileReader();
			CDTFileWriter fileWriter = new CDTFileWriter();
			fileReader.readPropertiesFile();
			CDTConstants.fullPath = fileWriter.createOutDir(OUTPUT_DIR);
			CDT_REPORT_DIR1_OUT = CDTConstants.fullPath;
			System.out.println("CDT_REPORT_DIR1: " + CDT_REPORT_DIR1);
			try {

				File[] ydfPref1FilesList = fileReader.readFilesFromDir(YDKPREF1);
				ArrayList<String> tableNamesList = new ArrayList<String>();
				if (ydfPref1FilesList != null && ydfPref1FilesList.length > 0) {
					for (File file : ydfPref1FilesList) {
						if (file != null && file.length() > 0) {
							DocumentBuilder db = null;
							db = factory.newDocumentBuilder();
							Document doc = null;
							doc = db.parse(file);
							String expression = "//" + "Ignore" + "//" + "Table";
							NodeList nodeList = null;
							nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
							System.out.println("nodeList Length: " + nodeList.getLength());
							for (int itr = 0; itr < nodeList.getLength(); itr++) {
								Element tableElement = (Element) nodeList.item(itr);
								String tableName = tableElement.getAttribute("Name");
								if (tableName != null && !tableName.isEmpty()) {
									tableName = tableName + ".xml";
									tableNamesList.add(tableName);
								}
							}
							System.out.println("tableNamesList : " + tableNamesList.toString());
						}
					}
				}

				File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR1);
				if (filesList == null) {
					System.out.println("No files found in directory: " + CDT_REPORT_DIR1);
				}
				if (filesList != null && filesList.length > 0) {
					for (File f : filesList) {
						if (f != null && f.length() > 0) {
							System.out.println("The files in the CDT_REPORT_DIR1 are " + f.getName());
							String fileName = f.getName();
							CDTXmlComparator xmlComparator = new CDTXmlComparator();
							try {
								if (tableNamesList.isEmpty() || !(tableNamesList.contains(fileName))) {
									System.out.println(
											"YDKPREF1 Table Names List is Empty or YDKPREF1 Table Names List does not contains the File Name : "
													+ fileName);
									if (fileName.startsWith("YFS") || fileName.startsWith("PLT")) {

										outputDoc = xmlComparator.cleanCompareReport(f);
										fileWriter.fileWriterMethod(CDTConstants.fullPath, outputDoc, fileName);
									} else {
										try {

											fileWriter.copyFileToDirectory(f, CDTConstants.fullPath);
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
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			System.out.println("CDT_REPORT_DIR2: " + CDT_REPORT_DIR2);
			try {

				File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR2);
				if (filesList != null) {

					for (File file : filesList) {
						System.out.println("The files in the CDT_REPORT_DIR2 are " + file.getName());
					}
				} else {
					System.out.println("No files found in directory: " + CDT_REPORT_DIR2);
				}

			} catch (SecurityException e) {
				e.printStackTrace();
			}
			System.out.println("CDT_XMLS1: " + CDTConstants.CDT_XMLS1);

			try {
				File[] filesList = fileReader.readFilesFromDir(CDT_XMLS1);
				if (filesList != null) {

					for (File file : filesList) {
						System.out.println("The files in the CDT_XMLS1 are " + file.getName());
					}
				} else {
					System.out.println("No files found in directory: " + CDT_XMLS1);
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			System.out.println("CDT_XMLS2: " + CDT_XMLS2);

			try {
				File[] filesList = fileReader.readFilesFromDir(CDT_XMLS2);
				if (filesList != null) {

					for (File file : filesList) {
						System.out.println("The files in the CDT_XMLS1 are " + file.getName());
					}

				} else {
					System.out.println("No files found in directory: " + CDT_XMLS2);
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			}

			for (int i = 0; i < mode.length; i++) {
				System.out.println("Mode is passed as " + mode[i]);
				if (mode[i].equals("merge")) {

					// fileWriter.mergeAfterReview(fileWriter.fullPath + "\\manual",
					// fileWriter.fullPath);
					fileWriter.mergeAfterReview("D:\\Parent\\manual", "D:\\Parent");
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}