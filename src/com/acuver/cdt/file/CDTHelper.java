package com.acuver.cdt.file;

public class CDTHelper {

	public void printMsg() {
		String message = "properties does'nt exist please add that property";
		System.out.println(message);
	}

	public void formPropertiesFileHelpMsg() {
		String message = "Please create a file enhancedcdt.properties in current folder. The following properties can be configured.\r\n"
				+ "	CDT_REPORT_DIR1     CDT Compare Report1\r\n"
				+ "	CDT_REPORT_DIR2     CDT Compare Report2\r\n"
				+ "	CDT_XMLS1  	    CDT Export XMLs1\r\n"
				+ "	CDT_XMLS2  	    CDT Export XMLs\r\n"
				+ "	YDKPREF1	    ydfprefs.xml (Optional)\r\n"
				+ "        YDKPERF2	    ydfprefs.xml (Optional)\r\n"
				+ "        OUTPUT_DIR 	    Output Folder (Optional)";
		System.out.println(message);
	}
}
