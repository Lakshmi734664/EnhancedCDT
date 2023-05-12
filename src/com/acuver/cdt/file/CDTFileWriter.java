package com.acuver.cdt.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;

public class CDTFileWriter {

	public static String fullPath = "";

	public void appendXmlFile(File sourceFile, File destFile) {
		try {

			// Create a new DocumentBuilderFactory

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

			System.out.println("XML file appended successfully.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String createOutDir(String location) throws IOException {

		String timeStamp = new SimpleDateFormat(CDTConstants.dateFormat).format(new Date());

		if (location == null) {

			fullPath = CDTConstants.currentDirectory + File.separator + timeStamp;

			createDirectory(fullPath + File.separator+CDTConstants.manual);
			createDirectory(fullPath + File.separator + CDTConstants.enhancedCompare);
		} else {

			fullPath = location + File.separator + timeStamp;

			createDirectory(fullPath + File.separator+CDTConstants.manual);
			createDirectory(fullPath + File.separator + CDTConstants.enhancedCompare);
		}

		return fullPath;
	}

	// Writing File to a Directory
	public void writeFile(String fullPath, Document outputDoc, String fileName) throws Exception {
		try {
			String fileData = convertDocumentToString(outputDoc);
			createXMLFile(fullPath, fileName, fileData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String convertDocumentToString(Document document) throws Exception {

		Transformer transformer = EnhancedCDTMain.tf.newTransformer();
		StringWriter sw = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(sw));

		try {
			// create a new transformer factory and set the formatting properties
			EnhancedCDTMain.tf.setAttribute("indent-number", 4);

			// create a new transformer and set the formatting properties
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// create a new DOM source from the XML string
			DOMSource source = new DOMSource(EnhancedCDTMain.factory.newInstance().newDocumentBuilder()
					.parse(new InputSource(new StringReader(sw.toString()))));

			// create a new string writer to store the formatted XML
			StringWriter writer = new StringWriter();

			// transform the DOM source to a string writer with the transformer
			transformer.transform(source, new StreamResult(writer));

			// return the formatted XML as a string
			return writer.toString().replaceAll("\\n\\s*\\n", "\n");
		} catch (Exception e) {
			throw new RuntimeException("Error occurs when pretty-printing xml:\n" + sw.toString(), e);
		}
	}

	/*
	 * public String convertDocumentToString(Document document) throws Exception {
	 * Transformer trans = EnhancedCDTMain.tf.newTransformer(); StringWriter sw =
	 * new StringWriter(); trans.transform(new DOMSource(document), new
	 * StreamResult(sw)); return sw.toString().replaceAll("\\n\\s*\\n", "\n"); }
	 */
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

				// If file doesn't exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}

				// Create a new FileWriter object to write to the file
				writer = new FileWriter(file);

				// Write the file data to the file
				writer.write(fileData);

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

		String manualFolderName = parentFolderName  +File.separator+CDTConstants.manual;
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

						System.out.println("Copied " + sourceFile.getPath() + " to " + destFile.getPath());
					} catch (Exception e) {
						System.err.println("Error copying file: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void readDataFromExcelSheet() {
		// specify input and output file paths
		String excelFilePath = "D:\\Table.xlsx";
		String outputFile = "D:\\EnhancedCDT\\recordIdentifier.config";

		try {

			// create input stream for input file
			FileInputStream inputStream = new FileInputStream(new File(excelFilePath));

			// create workbook object for input file
			XSSFWorkbook Workbook = new XSSFWorkbook(inputStream);

			// get the first sheet of the workbook
			XSSFSheet sheet = Workbook.getSheetAt(0);

			// create FileWriter object for output file
			FileWriter writer = new FileWriter(outputFile);

			int rows = sheet.getLastRowNum();
			int cols = sheet.getRow(0).getLastCellNum();

			for (int r = 0; r <= rows; r++) {

				if (r == 0) {
					continue;
				}

				XSSFRow row = sheet.getRow(r);

				for (int c = 0; c < cols; c++) {

					if (c == 1) {
						continue;
					}

					XSSFCell cell = row.getCell(c);

					if (cell == null) {
						continue;
					}

					if (c == 0) {
						System.out.print(cell.getStringCellValue() + "=");
						writer.write(cell.getStringCellValue() + "=");
					} else if (c==2) {
						System.out.print(cell.getStringCellValue()+"|");
						writer.write(cell.getStringCellValue()+"|");
					} else {
						String input = cell.getStringCellValue();

						// Create a StringBuilder to build the modified string
						StringBuilder builder = new StringBuilder(input);

						// Iterate through each character in the StringBuilder
						for (int i = 0; i < builder.length(); i++) {
							// Check if the character is '+'
							if (builder.charAt(i) == '+') {
								// Replace '+' with ','
								builder.setCharAt(i, ',');
							}
						}

						// Convert the StringBuilder back to a string
						String output = builder.toString();
						System.out.print(output);
						writer.write(output);
					}

				}
				// move to the next line in the output file
				writer.write("\n");
				System.out.println();
			}
			// close input stream and writer
			inputStream.close();
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}