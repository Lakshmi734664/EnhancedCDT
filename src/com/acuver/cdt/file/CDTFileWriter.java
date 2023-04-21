package com.acuver.cdt.file;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;

public class CDTFileWriter {
	
	 public static FileWriter writer = null;
	 public static  final String dateFormat = "yyyyMMddHHmmss";
		// Creating directory with timeStamp
	 public static  final String timeStamp = new SimpleDateFormat(dateFormat).format(new Date()) ; 
	 
	public CDTFileWriter(String fileLocation, String fileName, String fileData)
			throws IllegalArgumentException, IOException {

		final String fullPath = fileLocation + timeStamp +"\\manual";

		System.out.println(fullPath);

		createDirectory(fullPath);

		createXMLFile(fullPath, fileName, fileData);

	}

	public static void createXMLFile(String fileLocation, String fileName, String fileData)
			throws IllegalArgumentException, IOException {

		try {
			if (fileData == "") {
				throw new Exception("No file data given\r\n" + "Please give the filedata.");
			} else if (fileLocation == "") {
				throw new Exception("File location is missing.");
			} else if (fileName == "") {
				throw new Exception("Filename is missing.");
			} else {
				// Create a new file object with the specified file location and name
				File file = new File(fileLocation + File.separator + fileName);

				// Create a new FileWriter object to write to the file
				writer = new FileWriter(file);

				// Write the file data to the file
				String fileData1 = covertDocumentToString(fileData);
				writer.write(fileData1);

				System.out.println("File created successfully!");
			}
		} catch (Exception e) {
			System.out.println("Caught an exception: " + e.getMessage());
		} finally {
			// Close the writer
			writer.close();
		}

	}
	
	public static void createDirectory(String path) {

		try {
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
		} catch (Exception e) {
			System.out.println("Caught an exception: " + e.getMessage());
		}
	}
	// input is document

	  public static String covertDocumentToString(String xmlString) throws Exception {
		  try {
		    // create a new transformer factory and set the formatting properties
		    TransformerFactory transformerFactory = TransformerFactory.newInstance();
		    transformerFactory.setAttribute("indent-number", 4);

		    // create a new transformer and set the formatting properties
		    Transformer transformer = transformerFactory.newTransformer();
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		    // create a new DOM source from the XML string
		    DOMSource source = new DOMSource(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xmlString))));

		    // create a new string writer to store the formatted XML
		    StringWriter writer = new StringWriter();

		    // transform the DOM source to a string writer with the transformer
		    transformer.transform(source, new StreamResult(writer));

		    // return the formatted XML as a string
		    return writer.toString();
		  } catch (Exception e)
		  {
			throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
		   }
	  }
	
}
