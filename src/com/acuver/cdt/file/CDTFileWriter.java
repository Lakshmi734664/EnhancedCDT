package com.acuver.cdt.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CDTFileWriter {
	
	
	
	public CDTFileWriter(String fileLocation,String fileName,String fileData) throws IllegalArgumentException, IOException{
		   
		   //Creating directory with timeStamp

		    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		  
		    String fullPath = fileLocation + timeStamp;
		    
		    System.out.println(fullPath);
		    
		    createDirectory(fullPath);
		    
		    createXMLFile(fullPath, fileName ,fileData);
		
	}

public static void createXMLFile(String fileLocation, String fileName, String fileData) throws IllegalArgumentException, IOException {
       
	   FileWriter writer = null;
           
           try {
        	   if (fileData=="") {
       	        throw new Exception("No file data given\r\n"	+ "Please give the filedata.");
       	       }
        	    if (fileLocation=="") {
        	        throw new Exception("File location is missing.");
        	    }else if(fileName=="") {
        	    	throw new Exception("Filename is missing.");
        	    }else {
        	    	// Create a new file object with the specified file location and name
        	    	   
        	           File file = new File(fileLocation + File.separator + fileName );
        	           
        	           // Create a new FileWriter object to write to the file
        	            writer = new FileWriter(file);
        	           
        	           // Write the file data to the file
        	            
        	           String fileData1 = printInProperFormat(fileData,3,true);
          	    	 writer.write(fileData1);
          	    	 
          	    	 
          	    	
          	        System.out.println("File created successfully!");
        	    }
        	} catch (Exception e) {
        	    System.out.println("Caught an exception: " + e.getMessage());
        	}finally {
        		  // Close the writer
      	        writer.close();
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
	  
	  public static String printInProperFormat(String xmlString, int indent, boolean ignoreDeclaration) {

		   try {
		        InputSource src = new InputSource(new StringReader(xmlString));
		        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

		        TransformerFactory transformerFactory = TransformerFactory.newInstance();
		        transformerFactory.setAttribute("indent-number", indent);
		        Transformer transformer = transformerFactory.newTransformer();
		        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
		        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		        StringWriter writer = new StringWriter();
		        StreamResult result = new StreamResult(writer);
		        DOMSource source = new DOMSource(document);

		        if (!ignoreDeclaration) {
		            // Remove the extra line by setting the transformer output property
		            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
		        }
		        
		        transformer.transform(source, result);

		        return writer.toString();
		    } catch (Exception e) {
		        throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
		    }
		}
	  
	

}
