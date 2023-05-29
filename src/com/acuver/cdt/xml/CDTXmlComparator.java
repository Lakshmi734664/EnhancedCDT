package com.acuver.cdt.xml;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;
import org.w3c.dom.*;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class CDTXmlComparator {
	private String tableName;
	private String tablePrefix;
	private String outDir;
	private Document inputDoc;
	private CDTFileReader fileReader;
	private CDTFileWriter fileWriter;
	private CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();

	private EnhancedCompareGenerator enhancedCompareGenerator = new EnhancedCompareGenerator();
	private RecordIdentifer recordIdentifer = new RecordIdentifer();
	private boolean isXML1 = true;

	public Document merge() throws Exception {

		Element root = inputDoc.getDocumentElement();
		tableName = root.getNodeName();

		CDTHelper.printMsg(tableName);

		// Getting the Table Prefix Name
		tablePrefix = CDTHelper.getTablePrefix(tableName);
		String primaryKeyName = tablePrefix + CDTConstants.key;
		CDTXmlDifferenceEvaluator.setPrimaryKeyName(primaryKeyName);

		recordIdentifer.setTableName(tableName);
		recordIdentifer.setTablePrefix(tablePrefix);
		recordIdentifer.setFileReader(fileReader);

		// Processing Insert/Delete Tags
		NodeList deleteNodeList = inputDoc.getDocumentElement().getElementsByTagName(CDTConstants.DELETE);
		if (deleteNodeList.getLength() > 0) {
			inputDoc = processInsertDeleteElements(inputDoc);
		}

		Document processedUpdateEnhancedCompareDoc = null;
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();
		// Create a new Processed document with Root Element
		processedUpdateEnhancedCompareDoc = parser.newDocument();
		Element parentElement = processedUpdateEnhancedCompareDoc.createElement(tableName);
		processedUpdateEnhancedCompareDoc.appendChild(parentElement);

		enhancedCompareGenerator.setProcessedUpdateEnhancedCompareDoc(processedUpdateEnhancedCompareDoc);

		// Process the Updates
		processUpdates(inputDoc);

		// Moving Update Elements to Manual Folder
		moveUpdatesToManualReview(inputDoc);

		inputDoc = removeDeleteTags(inputDoc);

		return inputDoc;
	}

	public Document processInsertDeleteElements(Document doc) throws Exception {
		Element rootEle = doc.getDocumentElement();
		NodeList nodeList = doc.getDocumentElement().getElementsByTagName(CDTConstants.INSERT);
		debug(doc, "Before processInsertDeleteElements");
		for (int itr = 0; itr < nodeList.getLength(); itr++) {

			Element insertElement = (Element) nodeList.item(itr);

			recordIdentifer.setDoc(doc);
			recordIdentifer.setElemToMatch(insertElement);
			Element deleteElement = recordIdentifer.getMatchingUniqueElement(true);
			if (deleteElement != null) {

				Diff diff = DiffBuilder.compare(insertElement).withTest(deleteElement).checkForSimilar()
						.ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
						.withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

				if (diff.hasDifferences()) {
					Element updateElement = doc.createElement(CDTConstants.update);
					// Replace Insert element with Update element
					NamedNodeMap attributes = insertElement.getAttributes();
					for (int j = 0; j < attributes.getLength(); j++) {
						Node attribute = attributes.item(j);
						updateElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());

					}

					String primaryKeyName = tablePrefix + CDTConstants.key;
					String primaryKeyValue = deleteElement.getAttribute(primaryKeyName);

					updateElement.setAttribute(primaryKeyName, primaryKeyValue);

					Element oldValuesElement = CDTHelper.createChildElement(updateElement, CDTConstants.OLDVALUES);

					// Store the difference values in the OldValues element
					Iterator<Difference> iter = diff.getDifferences().iterator();
					while (iter.hasNext()) {
						String difference = iter.next().toString();

						if (difference != null && !(difference.contains("xml version"))) {
							int attrNameStartIndex = difference.indexOf("@") + 1;
							int attrNameEndIndex = difference.indexOf("to <");

							String attrName = difference.substring(attrNameStartIndex, attrNameEndIndex).trim();

							int oldAttrValueStartIndex = difference.indexOf("but was '") + 9;
							int oldAttrValueEndIndex = difference.indexOf("- comparing") - 2;
							String oldAttrValue = difference.substring(oldAttrValueStartIndex, oldAttrValueEndIndex)
									.trim();

							oldValuesElement.setAttribute(attrName, oldAttrValue);
						}
						if (difference != null && difference.contains("xml version")) {

							NamedNodeMap deleteAttrList = deleteElement.getAttributes();
							for (int attrItr = 0; attrItr < deleteAttrList.getLength(); attrItr++) {
								Node deleteAttr = deleteAttrList.item(attrItr);
								String deleteAttrname = deleteAttr.getNodeName();
								String deleteAttrValue = deleteAttr.getNodeValue();
								if (isSubXML(deleteAttrname, deleteAttrValue)) {
									oldValuesElement.setAttribute(deleteAttrname, deleteAttrValue);
								}
							}
						}
					}
					rootEle.appendChild(updateElement);

				} else {
				}

				rootEle.removeChild(insertElement);
				rootEle.removeChild(deleteElement);

			}
		}
		debug(doc, "After processInsertDeleteElements");
		return doc;
	}

	// Process the Updates
	public Document processUpdates(Document doc) throws Exception {
		debug(doc, "Before processUpdates");
		NodeList nodeList = doc.getDocumentElement().getElementsByTagName(CDTConstants.OLDVALUES);
		if (nodeList != null && nodeList.getLength() > 0) {
			for (int itr = 0; itr < nodeList.getLength(); itr++) {
				Node oldValueNode = nodeList.item(itr);
				Node updateNode = oldValueNode.getParentNode();
				NamedNodeMap attrList = oldValueNode.getAttributes();
				for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
					Node oldValueAttr = attrList.item(attrItr);
					String oldValueAttrName = oldValueAttr.getNodeName();
					String oldValueAttrValue = oldValueAttr.getNodeValue();
					if (isSubXML(oldValueAttrName, oldValueAttrValue)) {
						NamedNodeMap updateAttrList = updateNode.getAttributes();
						Diff diff = null;
						for (int attrItr2 = 0; attrItr2 < updateAttrList.getLength(); attrItr2++) {
							Node updateAttr = updateAttrList.item(attrItr2);
							String updateAttrname = updateAttr.getNodeName();
							String updateAttrValue = updateAttr.getNodeValue();

							if (oldValueAttrName.equalsIgnoreCase(updateAttrname)) {

								diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue)
										.checkForSimilar().ignoreComments().ignoreWhitespace()
										.ignoreElementContentWhitespace().normalizeWhitespace().build();

								if (diff != null && diff.hasDifferences()) {
									enhancedCompareGenerator.createEnhancedCompare(updateAttrname,
											(Element) updateNode);
								} else {
									updateNode.getParentNode().removeChild(updateNode);
								}
								break;
							}
						}
					}
				}
			}
		}
		if (enhancedCompareGenerator.getProcessedUpdateEnhancedCompareDoc().getDocumentElement().getChildNodes()
				.getLength() > 0) {
			String fileName = tableName + CDTConstants.xmlExtension;
			String fullPath = outDir + File.separator + CDTConstants.enhancedCompare;
			fileWriter.writeFile(fullPath, enhancedCompareGenerator.getProcessedUpdateEnhancedCompareDoc(), fileName);
		}
		debug(doc, "After processUpdates");
		return doc;
	}

	// Move Updates Doc to ManualReview
	public void moveUpdatesToManualReview(Document doc) throws Exception {

		NodeList nodeList = doc.getDocumentElement().getElementsByTagName(CDTConstants.UPDATE);

		String primaryKeyName = tablePrefix + CDTConstants.key;

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		// DOM Parser
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();
		Document processedManualReviewDoc = parser.newDocument();
		Element parentElementEnhancedCompare = processedManualReviewDoc.createElement(tableName);
		processedManualReviewDoc.appendChild(parentElementEnhancedCompare);

		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node updateNode = nodeList.item(itr);

			Element updateElement = (Element) nodeList.item(itr);
			String primaryKeyValue = updateElement.getAttribute(primaryKeyName);

			String Xmls1Modifyts = null;
			String Xmls2Modifyts = null;

			// Getting file from Directory CDT_XMLS1
			Document cdtxmls1doc = null;

			if (isXML1) {
				cdtxmls1doc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS1, tableName + ".cdt.xml");
			} else {
				cdtxmls1doc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS2, tableName + ".cdt.xml");
			}

			if (cdtxmls1doc != null) {
				NodeList Xmls1NodeList = cdtxmls1doc.getDocumentElement().getChildNodes();

				for (int Xmls1Nodeitr = 0; Xmls1Nodeitr < Xmls1NodeList.getLength(); Xmls1Nodeitr++) {
					Node xmls1Node = Xmls1NodeList.item(Xmls1Nodeitr);
					if (xmls1Node instanceof Element) {
						Element Xmls1Element = (Element) xmls1Node;
						String Xmls1PrimaryKeyValue = Xmls1Element.getAttribute(primaryKeyName);
						if (Xmls1PrimaryKeyValue.equalsIgnoreCase(primaryKeyValue)) {

							Xmls1Modifyts = Xmls1Element.getAttribute("Modifyts");

							break;
						}
					}
				}
			}

			// Getting file from Directory CDT_XMLS2
			Document cdtxmls2doc = null;

			if (isXML1) {
				cdtxmls2doc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS2, tableName + ".cdt.xml");
			} else {
				cdtxmls2doc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS1, tableName + ".cdt.xml");
			}

			if (cdtxmls2doc != null) {
				recordIdentifer.setDoc(cdtxmls2doc);
				recordIdentifer.setElemToMatch(updateElement);
				Element Xmls2ElementMatch = recordIdentifer.getMatchingUniqueElement(false);
				if (Xmls2ElementMatch != null) {

					Xmls2Modifyts = Xmls2ElementMatch.getAttribute("Modifyts");

				}
			}

			Date Xmls1Modifytsdate = null;
			Date Xmls2Modifytsdate = null;

			if (Xmls1Modifyts != null && !Xmls1Modifyts.isEmpty()) {
				try {
					Xmls1Modifytsdate = formatter.parse(Xmls1Modifyts);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (Xmls2Modifyts != null && !Xmls2Modifyts.isEmpty()) {
				try {
					Xmls2Modifytsdate = formatter.parse(Xmls2Modifyts);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

			if (Xmls1Modifytsdate != null && Xmls2Modifytsdate != null) {
				if (Xmls2Modifytsdate.before(Xmls1Modifytsdate)) {

					Node importedUpdateNode = processedManualReviewDoc.importNode(updateNode, true);
					processedManualReviewDoc.getDocumentElement().appendChild(importedUpdateNode);

					// Removing this update Node from inputDoc
					inputDoc.getDocumentElement().removeChild(updateNode);
				}
			}
		}

		// Writing the File into Output Directory manual Folder
		try {
			if (processedManualReviewDoc.getDocumentElement().getChildNodes().getLength() > 0) {
				String fileName = tableName + CDTConstants.xmlExtension;
				String fullPath = outDir + File.separator + CDTConstants.manual;
				fileWriter.writeFile(fullPath, processedManualReviewDoc, fileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Checking if the Attribute is Sub XML
	private boolean isSubXML(String attrName, String attrValue) {
		return attrValue.startsWith("<?xml");
	}

	// remove delete tag from document
	public Document removeDeleteTags(Document doc) throws Exception {
		debug(doc, "Before removeDeleteTags");
		NodeList deleteNodesList = doc.getElementsByTagName(CDTConstants.DELETE);
		Element rootEle = doc.getDocumentElement();
		int length = deleteNodesList.getLength();
		for (int i = length - 1; i >= 0; i--) {
			Node node = deleteNodesList.item(i);
			rootEle.removeChild(node);
		}
		debug(doc, "Before removeDeleteTags");
		return doc;
	}

	public void debug(Document doc, String s) throws Exception {

		NodeList nodeList = doc.getDocumentElement().getElementsByTagName(CDTConstants.INSERT);
		System.out.println(s + "  Insert nodeList Length() " + nodeList.getLength());

		nodeList = doc.getDocumentElement().getElementsByTagName(CDTConstants.DELETE);
		System.out.println(s + "  Delete nodeList Length() " + nodeList.getLength());

		nodeList = doc.getDocumentElement().getElementsByTagName(CDTConstants.UPDATE);
		System.out.println(s + "  Update nodeList Length() " + nodeList.getLength());
	}

	public void setInputDoc(Document inputDoc) {
		this.inputDoc = inputDoc;
	}

	public void setOutDir(String outDir) {
		this.outDir = outDir;
	}

	public void setFileReader(CDTFileReader fileReader) {
		this.fileReader = fileReader;
	}

	public void setFileWriter(CDTFileWriter fileWriter) {
		this.fileWriter = fileWriter;
	}

	public boolean isXML1() {
		return isXML1;
	}

	public void setXML1(boolean XML1) {
		isXML1 = XML1;
	}

}