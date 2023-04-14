package com.acuver.cdt;

import com.acuver.cdt.file.CDTFileWriter;

public class EnhancedCDTMain {
<<<<<<< HEAD
	public static void main(String args[]) throws IOException {

		Properties prop = null;
		try {
			File configFile = new File("enhancedcdt.properties");
			if (!configFile.exists()) {
				// Display message and exit if config file does not exist
				
				System.exit(1);
			}
			prop = CDTFileReader.readPropertiesFile(configFile.getPath());

			if (prop != null) {
				System.out.println(CDTHelper.printmsg("Config file exists"));
			}

			System.out.println("Properties : " + "\n" + prop + "\n");
			String CDT_REPORT_DIR1 = prop.getProperty("CDT_REPORT_DIR1");
			System.out.println("CDT_REPORT_DIR1: " + CDT_REPORT_DIR1);

			String CDT_REPORT_DIR2 = prop.getProperty("CDT_REPORT_DIR2");
			System.out.println("CDT_REPORT_DIR2: " + CDT_REPORT_DIR2);

			String CDT_XMLS1 = prop.getProperty("CDT_XMLS1");
			System.out.println("CDT_XMLS1: " + CDT_XMLS1);

			String CDT_XMLS2 = prop.getProperty("CDT_XMLS2");
			System.out.println("CDT_XMLS2: " + CDT_XMLS2);

			String OUTPUT_DIR = prop.getProperty("OUTPUT_DIR");
			System.out.println("OUTPUT_DIR: " + OUTPUT_DIR);
			CDTFileReader fileReader = new CDTFileReader();
			if (CDT_REPORT_DIR1 != null && !CDT_REPORT_DIR1.isEmpty()) {

				File[] filesList = fileReader.readFilesFromDir(CDT_REPORT_DIR1);
				for (File file : filesList) {
					System.out.println("The files in the CDT_REPORT_DIR1 are " + file.getName());
				}
			} else {
				System.out.println("you don't have the directory for CDT_REPORT_DIR1  please add that");
			}

			if (CDT_REPORT_DIR2 != null && !CDT_REPORT_DIR2.isEmpty()) {
				CDTFileReader fileReader1 = new CDTFileReader();
				File[] filesList1 = fileReader.readFilesFromDir(CDT_REPORT_DIR2);
				for (File file : filesList1) {
					System.out.println("The files in the CDT_REPORT_DIR2 are " + file.getName());
				}
			} else {
				System.out.println("you don't have the directory for CDT_REPORT_DIR2 please add that");
			}

			if (CDT_XMLS1 != null && !CDT_XMLS1.isEmpty()) {

				File[] filesList = fileReader.readFilesFromDir(CDT_XMLS1);
				for (File file : filesList) {
					System.out.println("The files in the CDT_XMLS1  are " + file.getName());
				}
			} else {
				System.out.println("you don't have the directory for CDT_XMLS1 please add that");
			}

			if (CDT_XMLS2 != null && !CDT_XMLS2.isEmpty()) {

				File[] filesList = fileReader.readFilesFromDir(CDT_XMLS2);
				for (File file : filesList) {
					System.out.println("The files in the CDT_XMLS2  are " + file.getName());
				}
			} else {
				System.out.println("you don't have the directory for CDT_XMLS2 please add that");
			}

			if (OUTPUT_DIR != null && !OUTPUT_DIR.isEmpty()) {
				File[] filesList = fileReader.readFilesFromDir(OUTPUT_DIR);
				for (File file : filesList) {
					System.out.println("The files in the OUTPUT_DIR are " + file.getName());
				}
			} else {
				System.out.println("you don't have the directory for OUTPUT_DIR please add that");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}   
        String fileData = "<Order  DocumentType=\"0001\"  EnterpriseCode=\"Matrix\"  >\r\n"
        		+ "<OrderHoldTypes>\r\n"
        		+ "<OrderHoldType HoldType=\"FRAUD_HOLD\"/>\r\n"
        		+ "</OrderHoldTypes>\r\n"
        		+ "\r\n"
        		+ "<OrderLines>\r\n"
        		+ "<OrderLine OrderedQty=\"1\" >\r\n"
        		+ "<Item ItemID=\"100013\" UnitCost=\"10.0\" UnitOfMeasure=\"EACH\"/>\r\n"
        		+ "</OrderLine>\r\n"
        		+ "</OrderLines>\r\n"
        		+ "<PersonInfoShipTo AddressLine1=\"234 Copley Place\" City=\"Boston\" Country=\"US\" DayPhone=\"\"  EMailID=\"\" FirstName=\"Lakshmi\" LastName=\"A\" MobilePhone=\"\"  State=\"MA\"  ZipCode=\"02116\"/>\r\n"
        		+ "<PersonInfoBillTo AddressLine1=\"234 Copley Place\" City=\"Boston\" Country=\"US\" DayPhone=\"\"  EMailID=\"\" FirstName=\"Lakshmi\" LastName=\"A\" MobilePhone=\"\"  State=\"MA\"  ZipCode=\"02116\"/>\r\n"
        		+ "</Order>\r\n"
        		+ " ";
        
        
        String fileLocation = "D://Reports//CDT//";
        String fileName = "test";
        CDTFileWriter writer  = new CDTFileWriter(fileLocation,fileName,fileData);
}
}
