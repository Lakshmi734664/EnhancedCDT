package com.acuver.cdt.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CDTFileWriter {
	
	
	
	public CDTFileWriter(String fileLocation,String fileName,String fileData){
		
		   String []directories = fileLocation.split("//");
		 
		 //Creating directory
		   String directory = directories[0]+"//"+directories[1];
		   createDirectory(directory);
		   
		   //Creating Sub - directory
		   String subDirectory = directories[0]+"//"+directories[1]+"//"+directories[2];
		   createDirectory(subDirectory);
		   
		   
		   //Creating directory with timeStamp
		   String directoryPath = directories[0]+"//"+directories[1]+"//"+directories[2]+"//";
		   
		   
			 
		    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		  
		    String fullPath = directoryPath + timeStamp;
		    
		    System.out.println(fullPath);
		    createDirectory(fullPath);
		    
		    createXMLFile(fullPath, fileName ,fileData);
		
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
