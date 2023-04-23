package com.acuver.cdt;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.w3c.dom.Document;

import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.file.CDTHelper;
import com.acuver.cdt.xml.CDTXmlComparator;

public class EnhancedCDTMain {

	public static Document outputDoc = null;

	public static String CDT_REPORT_DIR1;
	public static String CDT_REPORT_DIR2;
	public static String CDT_XMLS1;
	public static String CDT_XMLS2;
	public static String OUTPUT_DIR;
	public static String YDKPREF1;
	public static String YDKPREF2;
	public static Properties prop = null;

	public static void main(String args[]) throws Exception {

		try {

			CDTFileReader fileReader = new CDTFileReader();
			fileReader.readPropertiesFile1("enhancedcdt.properties");

			CDTHelper helper = new CDTHelper();

			System.out.println("CDT_REPORT_DIR1: " + CDT_REPORT_DIR1);
			if (CDT_REPORT_DIR1 != null && !CDT_REPORT_DIR1.isEmpty()) {

				try {

					File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR1);
					if (filesList == null) {
						System.out.println("No files found in directory: " + CDT_REPORT_DIR1);
					}
					if (filesList != null && filesList.length > 0) {
						for (File f : filesList) {
							if (f != null && f.length() > 0) {
								System.out.println("The files in the CDT_REPORT_DIR1 are " + f.getName());

								String name = f.getName();

								CDTXmlComparator xmlComparator = new CDTXmlComparator();

								String location = prop.getProperty("OUTPUT_DIR");
								CDTFileWriter.findPathOfDirectory(location);

								try {

									if (name.startsWith("YFS") || name.startsWith("PLT")) {

										outputDoc = xmlComparator.cleanCompareReport(f);
										CDTFileWriter.fileWriterMethod(outputDoc, prop, f);
									} else {

										try {
											CDTFileWriter.copyFileToDirectory(f, CDTFileWriter.fullPath);
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

			} else {
				helper.formPropertiesFileHelpMsg();
				System.exit(1);
			}

			System.out.println("CDT_REPORT_DIR2: " + CDT_REPORT_DIR2);
			if (CDT_REPORT_DIR2 != null && !CDT_REPORT_DIR2.isEmpty()) {

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

			} else {
				helper.printMsg("You don't have the CDT_REPORT_DIR2 properties.Please add it.");
				System.exit(1);
			}

			System.out.println("CDT_XMLS1: " + CDT_XMLS1);
			if (CDT_XMLS1 != null && !CDT_XMLS1.isEmpty()) {

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
			} else {
				helper.formPropertiesFileHelpMsg();
				System.exit(1);
			}

			System.out.println("CDT_XMLS2: " + CDT_XMLS2);
			if (CDT_XMLS2 != null && !CDT_XMLS2.isEmpty()) {

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
			} else {
				helper.formPropertiesFileHelpMsg();
				System.exit(1);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
