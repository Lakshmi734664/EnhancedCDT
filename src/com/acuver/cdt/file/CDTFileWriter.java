package com.acuver.cdt.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CDTFileWriter {
	
	
	
	public CDTFileWriter(String fileLocation,String fileName,String fileData){
		   
		   //Creating directory with timeStamp
		   String directoryPath = fileLocation;
		   

		    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		  
		    String fullPath = directoryPath + timeStamp;
		    
		    System.out.println(fullPath);
		    createDirectory(fullPath);
		    
		    createXMLFile(fullPath, fileName ,fileData);
		
	}
   @SuppressWarnings("resource")
public static void createXMLFile(String fileLocation, String fileName, String fileData) throws IllegalArgumentException {
       
           
           
           try {
        	   if (fileData=="") {
       	        throw new Exception("No file data given\r\n"	+ "Please give the filedata.");
       	       }
        	   else if (fileLocation=="") {
        	        throw new Exception("File location is missing.");
        	    }else if(fileName=="") {
        	    	throw new Exception("Filename is missing.");
        	    }else {
        	    	// Create a new file object with the specified file location and name
        	    	   
        	           File file = new File(fileLocation + File.separator + fileName + ".xml");
        	           
        	           // Create a new FileWriter object to write to the file
        	           FileWriter writer = new FileWriter(file);
        	           
        	           // Write the file data to the file
          	    	 writer.write(fileData);
          	    	  // Close the writer
          	        writer.close();
          	        System.out.println("File created successfully!");
        	    }
        	} catch (Exception e) {
        	    System.out.println("Caught an exception: " + e.getMessage());
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
