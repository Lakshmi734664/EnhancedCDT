package com.acuver.cdt.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.acuver.cdt.constants.CDTConstants;

public class CDTFileWriter {

	public String fullPath = "";

	public String findPathOfDirectory(String location) throws IOException {

		String timeStamp = new SimpleDateFormat(CDTConstants.dateFormat).format(new Date());

		if (location == null) {

			fullPath = CDTConstants.currentDirectory + "//" + timeStamp;

			createDirectory(fullPath + "\\manual");

		} else {

			fullPath = location + "//" + timeStamp;

			createDirectory(fullPath + "\\manual");
		}

		return fullPath;
	}

	// Writing File to a Directory
	public void fileWriterMethod(String fullPath, Document outputDoc, String fileName) throws Exception {
		try {
			final String fileData = convertDocumentToString(outputDoc);
			CDTFileWriter fileWriter = new CDTFileWriter();
			fileWriter.createXMLFile(fullPath, fileName, fileData);
			System.out.println("OUTPUT DIR:" + fullPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String convertDocumentToString(Document document) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(document), new StreamResult(sw));
		return sw.toString();
	}

	public void createXMLFile(String fileLocation, String fileName, String fileData)
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

	public void createDirectory(String path) {

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

	public String xmlFormatter(String xmlString) throws Exception {
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

	public void copyFileToDirectory(File sourceFile, String destinationDirectory) throws IOException {
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

	public void mergeAfterReview(String manualFolderName, String parentFolderName) {
		File manualFolder = new File(manualFolderName);
		File parentFolder = new File(parentFolderName);

		if (!manualFolder.isDirectory()) {
			System.err.println("Error: Manual folder name is not a directory.");
			return;
		}

		if (!parentFolder.isDirectory()) {
			System.err.println("Error: Parent folder name is not a directory.");
			return;
		}

		for (File sourceFile : manualFolder.listFiles()) {
			if (sourceFile.isFile()) {
				File destFile = new File(parentFolder, sourceFile.getName());
				if (destFile.exists()) {
					try {
						Path sourceFilePath = Paths.get(sourceFile.getPath());
						Path destFilePath = Paths.get(destFile.getPath());

						appendToFile(sourceFilePath, destFilePath);
						System.out.println("Copied " + sourceFile.getPath() + " to " + destFile.getPath());
					} catch (Exception e) {
						System.err.println("Error copying file: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void appendToFile(Path sourceFilePath, Path destFilePath) {
		File sourceFile = sourceFilePath.toFile();
		File destFile = destFilePath.toFile();

		if (!sourceFile.exists()) {
			System.err.println("Error: Source file does not exist.");
			return;
		}

		if (!destFile.exists()) {
			System.err.println("Error: Destination file does not exist.");
			return;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(destFile, true))) {
			String line;
			while ((line = reader.readLine()) != null) {

				writer.write(line);
				writer.newLine();

			}
		} catch (IOException e) {
			System.err.println("Error reading or writing file: " + e.getMessage());
			e.printStackTrace();
		}
	}

}