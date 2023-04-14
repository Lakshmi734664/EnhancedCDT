package com.acuver.cdt;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTHelper;

public class EnhancedCDTMain {
	public static void main(String args[]) throws IOException {

		Properties prop = null;
		try {
			File configFile = new File("C:\\Users\\Admin\\git\\EnhancedCDT\\resources\\config.properties");
			if (!configFile.exists()) {
				// Display message and exit if config file does not exist
				JOptionPane.showMessageDialog(null, "Config file does not exist");
				System.exit(1);
			}
			prop = CDTFileReader.readPropertiesFile(configFile.getPath());

			if (prop != null) {
				System.out.println(CDTHelper.printmsg("Config file exists"));
			}

			System.out.println("Properties : " + "\n" + prop + "\n");
			String CDT_COMPARE_REPORT1 = prop.getProperty("CDT_COMPARE_REPORT1");
			System.out.println("CDT_COMPARE_REPORT1: " + CDT_COMPARE_REPORT1);

			String CDT_COMPARE_REPORT2 = prop.getProperty("CDT_COMPARE_REPORT2");
			System.out.println("CDT_COMPARE_REPORT2: " + CDT_COMPARE_REPORT2);

			String CDT_EXPORT_XML1 = prop.getProperty("CDT_EXPORT_XML1");
			System.out.println("CDT_EXPORT_XML1: " + CDT_EXPORT_XML1);

			String CDT_EXPORT_XML2 = prop.getProperty("CDT_EXPORT_XML2");
			System.out.println("CDT_EXPORT_XML2: " + CDT_EXPORT_XML2);

			String OUTPUT_DIR = prop.getProperty("OUTPUT_DIR");
			System.out.println("OUTPUT_DIR: " + OUTPUT_DIR);
			CDTFileReader fileReader = new CDTFileReader();
			File[] filesList = fileReader.readFilesFromDir(CDT_COMPARE_REPORT1);
			for (File file : filesList) {
				System.out.println("The files in the CDT_COMPARE_REPORT1 folder are " + file.getName());
			}
			CDTFileReader fileReader1 = new CDTFileReader();
			File[] filesList1 = fileReader.readFilesFromDir(CDT_COMPARE_REPORT2);
			for (File file : filesList1) {
				System.out.println("The files in the CDT_COMPARE_REPORT2 are " + file.getName());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
