package com.acuver.cdt.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;

public class CDTFileWriter {

	public void appendXmlFile(File sourceFile, File destFile) {
		try {

			// Use the factory to create a new DocumentBuilder
			DocumentBuilder builder = EnhancedCDTMain.factory.newDocumentBuilder();

			// Parse the source file to create a new Document object
			Document sourceDoc = builder.parse(sourceFile);

			// Parse the destination file to create a new Document object
			Document destDoc = builder.parse(destFile);

			// Find the root element of the destination document
			Element destRootElement = destDoc.getDocumentElement();

			// Import the nodes from the source document into the destination document
			NodeList sourceNodes = sourceDoc.getDocumentElement().getChildNodes();
			for (int i = 0; i < sourceNodes.getLength(); i++) {
				Node importedNode = destDoc.importNode(sourceNodes.item(i), true);
				destRootElement.appendChild(importedNode);
			}

			// Write the modified document back to the destination file
			Transformer transformer = EnhancedCDTMain.tf.newTransformer();
			DOMSource source = new DOMSource(destDoc);
			StreamResult result = new StreamResult(destFile);
			transformer.transform(source, result);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String createOutDir(String location) throws IOException {

		String fullPath = "";
		String timeStamp = new SimpleDateFormat(CDTConstants.dateFormat).format(new Date());

		if (location == null) {

			fullPath = CDTConstants.currentDirectory + File.separator + timeStamp;

			createDirectory(fullPath + File.separator + CDTConstants.manual);
			createDirectory(fullPath + File.separator + CDTConstants.enhancedCompare);
		} else {

			fullPath = location + File.separator + timeStamp;

			createDirectory(fullPath + File.separator + CDTConstants.manual);
			createDirectory(fullPath + File.separator + CDTConstants.enhancedCompare);
		}

		return fullPath;
	}

	// Writing File to a Directory
	public void writeFile(String fullPath, Document outputDoc, String fileName) throws Exception {
		String fileData = CDTHelper.convertDocumentToString(outputDoc);
		createXMLFile(fullPath, fileName, fileData);
	}

	public void createXMLFile(String fileLocation, String fileName, String fileData) throws IOException {
		if (fileData == "") {
			throw new IllegalArgumentException("No file data given\r\n" + "Please give the file data.");
		} else if (fileLocation == "") {
			throw new IllegalArgumentException("File location is missing.");
		} else if (fileName == "") {
			throw new IllegalArgumentException("Filename is missing.");
		}

		// Create a new file object with the specified file location and name
		File file = new File(fileLocation + File.separator + fileName);

		// If the file doesn't exist, create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter writer = new FileWriter(file);
		// Write the file data to the file
		writer.write(fileData);

		writer.close();

	}

	public void createDirectory(String path) throws SecurityException {

		File directory = new File(path);
		directory.mkdirs();
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

	public void mergeAfterReview(String parentFolderName) {

		String manualFolderName = parentFolderName + File.separator + CDTConstants.manual;
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

						appendXmlFile(sourceFilePath.toFile(), destFilePath.toFile());

						sourceFile.delete();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}