package com.acuver.cdt;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.Transformer;
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

	public static void main(String args[]) throws Exception {

		Properties prop = null;
		try {
			File enhancedcdtfile = new File("enhancedcdt.properties");
			CDTHelper helper = new CDTHelper();
			if (!enhancedcdtfile.exists()) {
				// Display message and exit if config file does not exist
				helper.formPropertiesFileHelpMsg();
				System.exit(1);
			}

			CDTFileReader fileReader = new CDTFileReader();
			prop = fileReader.readPropertiesFile(enhancedcdtfile.getPath());

			System.out.println("Properties : " + "\n" + prop + "\n");

			String CDT_REPORT_DIR1 = prop.getProperty("CDT_REPORT_DIR1");
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

			String CDT_REPORT_DIR2 = prop.getProperty("CDT_REPORT_DIR2");
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

			String CDT_XMLS1 = prop.getProperty("CDT_XMLS1");
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

			String CDT_XMLS2 = prop.getProperty("CDT_XMLS2");
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

		String fileData = "<Order  DocumentType=\"0001\"  EnterpriseCode=\"Matrix\"  >\r\n" + "<OrderHoldTypes>\r\n"
				+ "<OrderHoldType HoldType=\"FRAUD_HOLD\"/>\r\n" + "</OrderHoldTypes>\r\n" + "\r\n" + "<OrderLines>\r\n"
				+ "<OrderLine OrderedQty=\"1\" >\r\n"
				+ "<Item ItemID=\"100013\" UnitCost=\"10.0\" UnitOfMeasure=\"EACH\"/>\r\n" + "</OrderLine>\r\n"
				+ "</OrderLines>\r\n"
				+ "<PersonInfoShipTo AddressLine1=\"234 Copley Place\" City=\"Boston\" Country=\"US\" DayPhone=\"\"  EMailID=\"\" FirstName=\"Lakshmi\" LastName=\"A\" MobilePhone=\"\"  State=\"MA\"  ZipCode=\"02116\"/>\r\n"
				+ "<PersonInfoBillTo AddressLine1=\"234 Copley Place\" City=\"Boston\" Country=\"US\" DayPhone=\"\"  EMailID=\"\" FirstName=\"Lakshmi\" LastName=\"A\" MobilePhone=\"\"  State=\"MA\"  ZipCode=\"02116\"/>\r\n"
				+ "</Order>\r\n" + " ";

		String fileLocation = "D://Reports//CDT//";
		String fileName = "test";
		CDTFileWriter writer = new CDTFileWriter(fileLocation, fileName, fileData);
	}

	private static void toString(Document newDoc, String Type) throws Exception {
		DOMSource domSource = new DOMSource(newDoc);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		System.out.println(Type + "\n" + sw.toString());
	}
}
