package com.acuver.cdt.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CDTFileWriter {
	

   public static void main(String[] args) {

	   
	   //Creating directory
	   String directory = "D://Reports";
	   createDirectory(directory);
	   
	   //Creating Sub - directory
	   String subDirectory = "D://Reports//CDT";
	   createDirectory(subDirectory);
	   
	   //Creating directory with timeStamp
	   String directoryPath = "D://Reports//CDT//";
		 
	    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	  
	    String fullPath = directoryPath + timeStamp;
	    
	    System.out.println(fullPath);
	    createDirectory(fullPath);
	    
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
	    createXMLFile(fullPath, "test",fileData);
  }
   public static void createXMLFile(String fileLocation, String fileName, String fileData) {
       try {
           // Create a new file object with the specified file location and name
           File file = new File(fileLocation + File.separator + fileName + ".xml");
           
       
           // Create a new FileWriter object to write to the file
           FileWriter writer = new FileWriter(file);

           // Write the file data to the file
           writer.write(fileData);

           // Close the writer
           writer.close();

           System.out.println("File created successfully!");
       } catch (IOException e) {
           System.out.println("Error creating file: " + e.getMessage());
       }
   }
   
	  public static void createDirectory(String path) {
		    File directory = new File(path);

		    // Create the directory if it doesn't exist
		    if (!directory.exists()) {
		      boolean success = directory.mkdirs();
		      if (success) {
		        System.out.println("Directory created successfully: " + path);
		      } else {
		        System.out.println("Failed to create directory: " + path);
		      }
		    }
		  }

}
