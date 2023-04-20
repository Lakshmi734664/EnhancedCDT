package com.acuver.cdt;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.file.CDTHelper;
import com.acuver.cdt.xml.CDTXmlComparator;

public class EnhancedCDTMain {

	public static Document outputDoc = null;
	public static File enhancedcdtfile;
	public static String CDT_REPORT_DIR1;
	public static String CDT_REPORT_DIR2;
	public static String CDT_XMLS1;
	public static String CDT_XMLS2;
	public static String fileData;
	public static String fileLocation;
	public static String fileName;

	public static void main(String args[]) throws Exception {

		Properties prop = null;
		try {
			enhancedcdtfile = new File("enhancedcdt.properties");
			CDTHelper helper = new CDTHelper();
			if (!enhancedcdtfile.exists()) {
				// Display message and exit if config file does not exist
				helper.formPropertiesFileHelpMsg();
				System.exit(1);
			}

			CDTFileReader fileReader = new CDTFileReader();
			prop = fileReader.readPropertiesFile(enhancedcdtfile.getPath());

			System.out.println("Properties : " + "\n" + prop + "\n");

			CDT_REPORT_DIR1 = prop.getProperty("CDT_REPORT_DIR1");
			System.out.println("CDT_REPORT_DIR1: " + CDT_REPORT_DIR1);

			if (CDT_REPORT_DIR1 != null && !CDT_REPORT_DIR1.isEmpty()) {

				File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR1);
				if (filesList == null) {
					System.out.println("No files found in directory: " + CDT_REPORT_DIR1);
				}
				if (filesList != null && filesList.length > 0) {
					for (File f : filesList) {
						if (f != null && f.length() > 0) {
							System.out.println("The files in the CDT_REPORT_DIR1 are " + f.getName());
							CDTXmlComparator xmlComparator = new CDTXmlComparator();
							try {
								outputDoc = xmlComparator.cleanCompareReport(f);
								fileWriterMethod(outputDoc, prop, f);
								toString(outputDoc, "Output Document for File Name : " + f.getName());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			} else {
				helper.printMsg("You don't have the CDT_REPORT_DIR1 properties.Please add it.");
				System.exit(1);
			}

			CDT_REPORT_DIR2 = prop.getProperty("CDT_REPORT_DIR2");
			System.out.println("CDT_REPORT_DIR2: " + CDT_REPORT_DIR2);
			if (CDT_REPORT_DIR2 != null && !CDT_REPORT_DIR2.isEmpty()) {
				File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR2);
				if (filesList != null) {

					for (File file : filesList) {
						System.out.println("The files in the CDT_REPORT_DIR2 are " + file.getName());
					}
				} else {
					System.out.println("No files found in directory: " + CDT_REPORT_DIR2);
				}

			} else {
				helper.printMsg("You don't have the CDT_REPORT_DIR2 properties.Please add it.");
				System.exit(1);
			}

			CDT_XMLS1 = prop.getProperty("CDT_XMLS1");
			System.out.println("CDT_XMLS1: " + CDT_XMLS1);
			if (CDT_XMLS1 != null && !CDT_XMLS1.isEmpty()) {

				File[] filesList = fileReader.readFilesFromDir(CDT_XMLS1);
				if (filesList != null) {

					for (File file : filesList) {
						System.out.println("The files in the CDT_XMLS1 are " + file.getName());
					}
				} else {
					System.out.println("No files found in directory: " + CDT_XMLS1);
				}
			} else {
				helper.printMsg("You don't have the CDT_XMLS1 properties.Please add it.");
				System.exit(1);
			}

			CDT_XMLS2 = prop.getProperty("CDT_XMLS2");
			System.out.println("CDT_XMLS2: " + CDT_XMLS2);
			if (CDT_XMLS2 != null && !CDT_XMLS2.isEmpty()) {

				File[] filesList = fileReader.readFilesFromDir(CDT_XMLS2);
				if (filesList != null) {

					for (File file : filesList) {
						System.out.println("The files in the CDT_XMLS1 are " + file.getName());
					}

				} else {
					System.out.println("No files found in directory: " + CDT_XMLS2);
				}
			} else {
				helper.printMsg("You don't have the CDT_XMLS2 properties.Please add it.");
				System.exit(1);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void toString(Document newDoc, String Type) throws Exception {
		DOMSource domSource = new DOMSource(newDoc);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		System.out.println(Type + "\n" + sw.toString());
	}

	private static void fileWriterMethod(Document outputDoc, Properties prop, File f)
			throws TransformerException, IllegalArgumentException, IOException {
		DOMSource domSource = new DOMSource(outputDoc);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);

		fileData = sw.toString();

		String location = prop.getProperty("OUTPUT_DIR");

		fileLocation = location + "//";
		System.out.println("OUTPUT_DIR: " + fileLocation);
		// String fileLocation = "D://Reports//CDT//";
		fileName = f.getName();
		CDTFileWriter writer = new CDTFileWriter(fileLocation, fileName, fileData);
	}
}
