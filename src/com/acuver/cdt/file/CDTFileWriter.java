package com.acuver.cdt.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import com.acuver.cdt.constants.*;

public class CDTFileWriter {

	public CDTFileWriter(String fullPath, String fileName, String fileData)
			throws IllegalArgumentException, IOException {

		createXMLFile(fullPath, fileName, fileData);

	}

	public static String findPathOfDirectory(String location) {

		String fullPath = "";

		String fileLocation = "";

		String timeStamp = new SimpleDateFormat(CDTConstants.dateFormat).format(new Date());

		if (location == null) {

			try {
				fileLocation = new java.io.File(".").getCanonicalPath();

				fullPath = fileLocation + "//" + timeStamp;

				createDirectory(fullPath);

				createDirectory(fullPath + "\\manual");

			} catch (IOException e) {

				e.printStackTrace();
			}

		} else {

			fileLocation = location + "//";

			createDirectory(location);

			fullPath = fileLocation + timeStamp;

			createDirectory(fullPath + "\\manual");
		}

		return fullPath;
	}

	public static void fileWriterMethod(String fullPath, Document outputDoc, Properties prop, File f) throws Exception {
		try {

			final String fileData = convertDocumentToString(outputDoc);
			final String fileName = f.getName();
			CDTFileWriter writer = new CDTFileWriter(fullPath, fileName, fileData);
			System.out.println("OUTPUT DIR:" + fullPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String convertDocumentToString(Document document) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(document), new StreamResult(sw));
		return sw.toString();
	}

	public static void createXMLFile(String fileLocation, String fileName, String fileData)
			throws IllegalArgumentException, IOException {
		FileWriter writer = null;
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
				String fileData1 = xmlFormatter(fileData);
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

	public static String xmlFormatter(String xmlString) throws Exception {
		try {
			// create a new transformer factory and set the formatting properties
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setAttribute("indent-number", 4);

			// create a new transformer and set the formatting properties
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// create a new DOM source from the XML string
			DOMSource source = new DOMSource(DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new StringReader(xmlString))));

			// create a new string writer to store the formatted XML
			StringWriter writer = new StringWriter();

			// transform the DOM source to a string writer with the transformer
			transformer.transform(source, new StreamResult(writer));

			// return the formatted XML as a string
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
		}
	}

	public static void copyFileToDirectory(File sourceFile, String destinationDirectory) throws IOException {
		if (!sourceFile.exists()) {
			throw new IllegalArgumentException("Source file " + sourceFile.getAbsolutePath() + " does not exist.");
		}

		if (!sourceFile.isFile()) {
			throw new IllegalArgumentException("Source " + sourceFile.getAbsolutePath() + " is not a file.");
		}

		Path destinationDirPath = Paths.get(destinationDirectory);
		if (!destinationDirPath.toFile().exists()) {
			Files.createDirectories(destinationDirPath);
		}

		Path destinationPath = Paths.get(destinationDirectory, sourceFile.getName());
		Files.copy(sourceFile.toPath(), destinationPath);
	}

}
