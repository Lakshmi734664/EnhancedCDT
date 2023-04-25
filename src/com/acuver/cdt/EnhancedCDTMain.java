package com.acuver.cdt;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;

import com.acuver.cdt.constants.*;
import com.acuver.cdt.xml.CDTXmlComparator;

public class EnhancedCDTMain {

	public static XPath xPath = XPathFactory.newInstance().newXPath();
	/* Create DOM Parser */
	public static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	public static void main(String args[]) throws Exception {

		Document outputDoc = null;
		Properties prop = null;
		try {

			CDTFileReader fileReader = new CDTFileReader();
			prop = fileReader.readPropertiesFile1("enhancedcdt.properties");

			String CDT_REPORT_DIR1 = prop.getProperty(CDTConstants.CDT_REPORT_DIR1);
			System.out.println("CDT_REPORT_DIR1: " + CDT_REPORT_DIR1);

			try {

				File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR1);
				if (filesList == null) {
					System.out.println("No files found in directory: " + CDT_REPORT_DIR1);
				}

				String location = prop.getProperty(CDTConstants.OUTPUT_DIR);

				String fullPath = CDTFileWriter.findPathOfDirectory(location);

				if (filesList != null && filesList.length > 0) {
					for (File f : filesList) {
						if (f != null && f.length() > 0) {
							System.out.println("The files in the CDT_REPORT_DIR1 are " + f.getName());

							String name = f.getName();

							CDTXmlComparator xmlComparator = new CDTXmlComparator();

							try {

								if (name.startsWith("YFS") || name.startsWith("PLT")) {

									outputDoc = xmlComparator.cleanCompareReport(f);
									CDTFileWriter.fileWriterMethod(fullPath, outputDoc, prop, f);
								} else {

									try {

										CDTFileWriter.copyFileToDirectory(f, fullPath);
									} catch (IOException e) {
										System.out.println("An error occurred while copying the file.");
										e.printStackTrace();
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
			String CDT_REPORT_DIR2 = prop.getProperty(CDTConstants.CDT_REPORT_DIR2);
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

			String CDT_XMLS1 = prop.getProperty(CDTConstants.CDT_XMLS1);
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
			String CDT_XMLS2 = prop.getProperty(CDTConstants.CDT_XMLS2);
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

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
