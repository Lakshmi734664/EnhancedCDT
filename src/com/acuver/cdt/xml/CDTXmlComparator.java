package com.acuver.cdt.xml;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathConstants;
import java.io.File;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CDTXmlComparator {
	private String tableName;
	private String tablePrefix;
	private String outDir;
	private Document inputDoc;
	private CDTFileReader fileReader;
	private CDTFileWriter fileWriter;
	private CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();
	private RecordIdentifer recordIdentifer = new RecordIdentifer();
	private boolean isXML1 = true;

	public Document merge() throws Exception {

		Element root = inputDoc.getDocumentElement();
		tableName = root.getNodeName();
		System.out.println("tableName : " + tableName);

		// Getting the Table Prefix Name
		tablePrefix = CDTHelper.getTablePrefix(tableName);
		String primaryKeyName = tablePrefix + CDTConstants.key;
		CDTXmlDifferenceEvaluator.setPrimaryKeyName(primaryKeyName);

		recordIdentifer.setTableName(tableName);
		recordIdentifer.setTablePrefix(tablePrefix);
		recordIdentifer.setFileReader(fileReader);

		// Processing Insert/Delete Tags
		inputDoc = processInsertDeleteElements(inputDoc);

		// Remove False Update
		inputDoc = removeFalseUpdates(inputDoc);

		// Processing Update Elements with EnhancedCompare
		addEnhancedCompareToUpdates(inputDoc);

		// Moving Update Elements to Manual Folder
		moveUpdatesToManualReview(inputDoc);

		// inputDoc = removeDeleteTags(inputDoc);

		System.out.println("After Merge OutDoc:"
				+ CDTHelper.convertDocumentToString(fileWriter.prettyPrintXml(inputDoc)));
		return inputDoc;
	}

	public Document processInsertDeleteElements(Document doc) throws Exception {
		System.out.println("Entering processInsertDeleteElements : ");
		Element rootEle = doc.getDocumentElement();

		String expression = CDTConstants.forwardSlash + CDTConstants.INSERT;
		NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		debug(doc, "Before processInsertDeleteElements");
		for (int itr = 0; itr < nodeList.getLength(); itr++) {

			System.out.println("itr " + itr);
			Element insertElement = (Element) nodeList.item(itr);

			recordIdentifer.setDoc(doc);
			recordIdentifer.setElemToMatch(insertElement);
			Element deleteElement = recordIdentifer.getMatchingUniqueElement(true);
			if (deleteElement != null) {

				System.out.println("Comparing insertElement with deleteElement : ");

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
					System.out.println(primaryKeyName);
					String primaryKeyValue = deleteElement.getAttribute(primaryKeyName);
					System.out.println(primaryKeyValue);

					updateElement.setAttribute(primaryKeyName, primaryKeyValue);

					Element oldValuesElement = CDTHelper.createChildElement(updateElement, CDTConstants.OLDVALUES);

					// Store the difference values in the OldValues element
					Iterator<Difference> iter = diff.getDifferences().iterator();
					while (iter.hasNext()) {
						String difference = iter.next().toString();
						System.out.println(difference);

						if (difference != null && !(difference.contains("xml version"))) {
							int attrNameStartIndex = difference.indexOf("@") + 1;
							int attrNameEndIndex = difference.indexOf("to <");

							System.out.println("attrNameStartIndex: " + attrNameStartIndex);
							System.out.println("attrNameEndIndex: " + attrNameEndIndex);
							String attrName = difference.substring(attrNameStartIndex, attrNameEndIndex).trim();

							System.out.println("UpdateAttrName :" + attrName);

							int oldAttrValueStartIndex = difference.indexOf("but was '") + 9;
							int oldAttrValueEndIndex = difference.indexOf("- comparing") - 2;
							String oldAttrValue = difference.substring(oldAttrValueStartIndex, oldAttrValueEndIndex)
									.trim();

							System.out.println("Old Attribute Value: " + oldAttrValue);
							oldValuesElement.setAttribute(attrName, oldAttrValue);
						}
						if (difference != null && difference.contains("xml version")) {
							System.out
									.println("difference Contains xml version having SubXml differences:" + difference);

							NamedNodeMap deleteAttrList = deleteElement.getAttributes();
							for (int attrItr = 0; attrItr < deleteAttrList.getLength(); attrItr++) {
								Node deleteAttr = deleteAttrList.item(attrItr);
								String deleteAttrname = deleteAttr.getNodeName();
								String deleteAttrValue = deleteAttr.getNodeValue();
								if (isSubXML(deleteAttrname, deleteAttrValue)) {
									System.out.println("The Attribute Value is a SUB-XML : " + deleteAttrname);
									System.out.println("Old Attribute Value of SubXml: " + deleteAttrValue);
									oldValuesElement.setAttribute(deleteAttrname, deleteAttrValue);
								}
							}
						}
					}
					rootEle.appendChild(updateElement);

				} else {
					System.out.println(
							"No Difference In insertNode and deleteNode. Both Insert and Delete Nodes are removed.");
				}
				System.out.println("removing insert ele at itr " + itr);

				rootEle.removeChild(insertElement);
				rootEle.removeChild(deleteElement);

			}
		}
		debug(doc, "After processInsertDeleteElements");
		System.out.println("Exiting processInsertDeleteElements : ");
		return doc;
	}

	// Remove False Update
	public Document removeFalseUpdates(Document doc) throws Exception {
		System.out.println("Entering removeFalseUpdates : ");
		debug(doc, "Before removeFalseUpdates");
		String expression = CDTConstants.forwardSlash + CDTConstants.UPDATE + CDTConstants.forwardSlash + CDTConstants.OLDVALUES;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Old Values nodeList Length()" + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node oldValueNode = nodeList.item(itr);
			Node updateNode = oldValueNode.getParentNode();
			System.out.println("\nNode Name :" + oldValueNode.getNodeName());
			NamedNodeMap attrList = oldValueNode.getAttributes();
			for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
				Node oldValueAttr = attrList.item(attrItr);
				String oldValueAttrName = oldValueAttr.getNodeName();
				String oldValueAttrValue = oldValueAttr.getNodeValue();
				if (isSubXML(oldValueAttrName, oldValueAttrValue)) {
					System.out.println("The Attribute Value is a SUB-XML : " + oldValueAttrName);
					NamedNodeMap updateAttrList = updateNode.getAttributes();
					Diff diff = null;
					for (int attrItr2 = 0; attrItr2 < updateAttrList.getLength(); attrItr2++) {
						Node updateAttr = updateAttrList.item(attrItr2);
						String updateAttrname = updateAttr.getNodeName();
						String updateAttrValue = updateAttr.getNodeValue();

						if (oldValueAttrName.equalsIgnoreCase(updateAttrname)) {
							System.out.println("Inside Sub XML Diff : ");
							System.out.println("updateAttrValue : " + updateAttrValue);
							System.out.println("oldValueAttrValue : " + oldValueAttrValue);

							diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue).checkForSimilar()
									.ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace()
									.normalizeWhitespace().build();

							if (diff != null && diff.hasDifferences()) {
								Iterator<Difference> iter = diff.getDifferences().iterator();
								while (iter.hasNext()) {
									String subXmlDiffData = iter.next().toString();
									System.out.println(subXmlDiffData);
								}
							} else {
								updateNode.getParentNode().removeChild(updateNode);
								System.out.println("No Difference in Sub XML : ");
							}
						}
					}
				}
			}
		}
		debug(doc, "After removeFalseUpdates");
		System.out.println("Exiting removeFalseUpdates : ");
		return doc;

	}

	// Processing Update Doc with EnhancedCompare
	public void addEnhancedCompareToUpdates(Document doc) throws Exception {
		System.out.println("Entering addEnhancedCompareToUpdates : ");
		Document processedUpdateEnhancedCompareDoc = null;
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();
		String parentNodeName = doc.getDocumentElement().getNodeName();
		// Create a new Processed document with Root Element
		processedUpdateEnhancedCompareDoc = parser.newDocument();
		Element parentElement = processedUpdateEnhancedCompareDoc.createElement(parentNodeName);
		processedUpdateEnhancedCompareDoc.appendChild(parentElement);

		String expression = CDTConstants.forwardSlash + CDTConstants.UPDATE + CDTConstants.forwardSlash + CDTConstants.OLDVALUES;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

		if (nodeList != null && nodeList.getLength() > 0) {

			System.out.println("Old Values nodeList Length()" + nodeList.getLength());
			for (int itr = 0; itr < nodeList.getLength(); itr++) {
				Node oldValueNode = nodeList.item(itr);
				Node updateNode = oldValueNode.getParentNode();
				System.out.println("\nNode Name :" + oldValueNode.getNodeName());
				NamedNodeMap attrList = oldValueNode.getAttributes();
				for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
					Node oldValueAttr = attrList.item(attrItr);
					String oldValueAttrName = oldValueAttr.getNodeName();
					String oldValueAttrValue = oldValueAttr.getNodeValue();
					if (isSubXML(oldValueAttrName, oldValueAttrValue)) {
						System.out.println("The Attribute Value is a SUB-XML : " + oldValueAttrName);
						NamedNodeMap updateAttrList = updateNode.getAttributes();
						Diff diff = null;
						for (int attrItr2 = 0; attrItr2 < updateAttrList.getLength(); attrItr2++) {
							Node updateAttr = updateAttrList.item(attrItr2);
							String updateAttrname = updateAttr.getNodeName();
							String updateAttrValue = updateAttr.getNodeValue();
							if (oldValueAttrName.equalsIgnoreCase(updateAttrname)) {
								System.out.println("Inside Sub XML Diff : ");
								System.out.println("oldValueAttrValue : " + oldValueAttrValue);
								System.out.println("updateAttrValue : " + updateAttrValue);

								diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue)
										.checkForSimilar().ignoreComments().ignoreWhitespace()
										.ignoreElementContentWhitespace().normalizeWhitespace().build();

								if (diff != null && diff.hasDifferences()) {
									Iterator<Difference> iter = diff.getDifferences().iterator();
									ArrayList<String> subXmlDiffDataList = new ArrayList<String>();
									while (iter.hasNext()) {
										String subXmlDiffData = iter.next().toString();
										subXmlDiffDataList.add(subXmlDiffData);
									}
									if (subXmlDiffDataList != null && subXmlDiffDataList.size() > 0) {
										processedUpdateEnhancedCompareDoc = addEnhancedCompareToProcessedUpdateDoc(
												processedUpdateEnhancedCompareDoc, updateNode, subXmlDiffDataList,
												oldValueAttrValue, updateAttrValue);
									}
								} else {
									System.out.println("No Difference in Sub XML : ");

								}
							}
						}
					}
				}
			}
		}

		System.out.println("processedUpdateEnhancedCompareDoc length :  "
				+ processedUpdateEnhancedCompareDoc.getDocumentElement().getChildNodes().getLength());
		if (processedUpdateEnhancedCompareDoc.getDocumentElement().getChildNodes().getLength() > 0) {
			String fileName = parentNodeName + CDTConstants.xmlExtension;
			String fullPath = outDir + File.separator + CDTConstants.enhancedCompare;
			fileWriter.writeFile(fullPath, processedUpdateEnhancedCompareDoc, fileName);
		}
		System.out.println("Exiting addEnhancedCompareToUpdates : ");
	}

	// Add Enhanced Compare Elements To Processed Update Doc
	public Document addEnhancedCompareToProcessedUpdateDoc(Document processedUpdateEnhancedCompareDoc, Node updateNode,
			ArrayList<String> subXmlDiffDataList, String subXmlOldAttrValue, String subXmlUpdateAttrValue)
			throws Exception {

		System.out.println("Entering addEnhancedCompareToProcessedUpdateDoc : ");
		Node importedUpdateNode = processedUpdateEnhancedCompareDoc.importNode(updateNode, true);
		processedUpdateEnhancedCompareDoc.getDocumentElement().appendChild(importedUpdateNode);

		Element enhancedCompareEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.enhancedCompare);
		Element attributeEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.attribute);
		Element oldAttrEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.old);
		Element newAttrEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.New);

		Document oldAttrSubxmlDoc = EnhancedCDTMain.factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(subXmlOldAttrValue)));
		System.out.println("oldAttrSubxmlDoc:"
				+ CDTHelper.convertDocumentToString(fileWriter.prettyPrintXml(oldAttrSubxmlDoc)));

		Document updateAttrSubxmlDoc = EnhancedCDTMain.factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(subXmlUpdateAttrValue)));
		System.out.println("updateAttrSubxmlDoc:"
				+ CDTHelper.convertDocumentToString(fileWriter.prettyPrintXml(updateAttrSubxmlDoc)));

		for (int i = 0; i < subXmlDiffDataList.size(); i++) {
			String diffValues = subXmlDiffDataList.get(i);
			if (diffValues.startsWith("Expected attribute value")) {
				System.out.println("diffValues in Expected attribute value : " + diffValues);

				// Regular expression pattern to match attributes starting with '@'
				String regex = "@\\w+";
				String attrName = "";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(diffValues);
				// Find and print attribute names
				while (matcher.find()) {
					attrName = matcher.group();
					attrName = attrName.replace("@", "");
					System.out.println("attrName in Expected attribute value : " + attrName);
				}

				int oldBeginIndex = diffValues.indexOf("value '") + 7;
				int oldEndIndex = diffValues.indexOf("' but");
				String oldAttrValue = diffValues.substring(oldBeginIndex, oldEndIndex).trim();
				System.out.println("oldAttrValue in Expected attribute value :" + oldAttrValue);
				int newBeginIndex = diffValues.indexOf("but was '") + 9;
				int newEndIndex = diffValues.indexOf("- comparing") - 2;
				String newAttrValue = diffValues.substring(newBeginIndex, newEndIndex).trim();
				System.out.println("newAttrValue in Expected attribute value :" + newAttrValue);
				oldAttrEle.setAttribute(attrName, oldAttrValue);
				newAttrEle.setAttribute(attrName, newAttrValue);

			} else if (diffValues.startsWith("Expected attribute name")) {
				System.out.println("diffValues in Expected attribute name: " + diffValues);

				// Regular expression pattern to match attributes starting with '@'
				String regex = "@\\w+";
				String attrName = "";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(diffValues);
				// Find and print attribute names
				while (matcher.find()) {
					attrName = matcher.group();
					attrName = attrName.replace("@", "");
					System.out.println("attrName in Expected attribute name : " + attrName);
				}

				// Regular expression pattern to match digits within square brackets
				String regex1 = "\\[(\\d+)\\]";

				Pattern pattern1 = Pattern.compile(regex1);
				Matcher matcher1 = pattern1.matcher(diffValues);

				// Find and print the last digit within the last square brackets
				String lastDigit = "";
				while (matcher1.find()) {
					lastDigit = matcher1.group(1);

				}

				System.out.println("Last Digit: " + lastDigit);
				int oldAttributeIndex = Integer.parseInt(lastDigit);
				System.out.println("oldAttributeIndex in Expected attribute name : " + oldAttributeIndex);

				// Regular expression pattern to match attribute name and last digit within
				// square brackets
				String regex2 = "/([^/\\[]+)\\[\\d+\\]";

				Pattern pattern2 = Pattern.compile(regex2);
				Matcher matcher2 = pattern2.matcher(diffValues);

				// Find and print the attribute names without the last digit
				String elementName = "";
				while (matcher2.find()) {
					elementName = matcher2.group(1);
				}
				System.out.println("Sub Element Name: " + elementName);
				Element desiredElement = (Element) oldAttrSubxmlDoc.getElementsByTagName(elementName)
						.item(oldAttributeIndex - 1);
				if (desiredElement != null && desiredElement.hasAttribute(attrName)) {
					System.out.println("desiredElement has this Attribute : " + attrName);
					String attrValue = desiredElement.getAttribute(attrName);
					System.out.println("Desired Element in Expected attribute name : " + attrValue);
					oldAttrEle.setAttribute(attrName, attrValue);

				} else {
					Element updateDesiredElement = (Element) updateAttrSubxmlDoc.getElementsByTagName(elementName)
							.item(oldAttributeIndex - 1);
					if (updateDesiredElement != null) {
						String attrValue = updateDesiredElement.getAttribute(attrName);
						newAttrEle.setAttribute(attrName, attrValue);
						System.out.println(
								"New Attribute in updateDesiredElement: " + updateDesiredElement.getNodeName());
					}
				}

			} else if (diffValues.startsWith("Expected child")
					&& !diffValues.startsWith("Expected child nodelist length")) {

				System.out.println("diffValues in Expected child : " + diffValues);
				// Regular expression pattern to match digits within square brackets
				String regex1 = "\\[(\\d+)\\]";

				Pattern pattern1 = Pattern.compile(regex1);
				Matcher matcher1 = pattern1.matcher(diffValues);

				// Find and print the last digit within the last square brackets
				String lastDigit = "";
				while (matcher1.find()) {
					lastDigit = matcher1.group(1);

				}
				System.out.println("Last Digit: " + lastDigit);
				int oldAttributeIndex = Integer.parseInt(lastDigit);
				System.out.println("oldAttributeIndex in Expected child : " + oldAttributeIndex);

				// Regular expression pattern to match attribute name and last digit within
				// square brackets
				String regex2 = "/([^/\\[]+)\\[\\d+\\]";

				Pattern pattern2 = Pattern.compile(regex2);
				Matcher matcher2 = pattern2.matcher(diffValues);

				// Find and print the attribute names without the last digit
				String elementName = "";
				while (matcher2.find()) {
					elementName = matcher2.group(1);
				}
				System.out.println("Sub Element Name: " + elementName);

				Element desiredElement = (Element) oldAttrSubxmlDoc.getElementsByTagName(elementName)
						.item(oldAttributeIndex - 1);
				if (desiredElement != null) {
					System.out.println("Desired Element in Expected child : " + desiredElement.getNodeName());
					Node importedElementNode = processedUpdateEnhancedCompareDoc.importNode(desiredElement, true);
					oldAttrEle.appendChild(importedElementNode);
				} else {
					Element updateDesiredElement = (Element) updateAttrSubxmlDoc.getElementsByTagName(elementName)
							.item(oldAttributeIndex - 1);
					if (updateDesiredElement != null) {
						System.out.println(
								"Update Desired Element in Expected child : " + updateDesiredElement.getNodeName());
						Node importedElementNode = processedUpdateEnhancedCompareDoc.importNode(updateDesiredElement,
								true);
						newAttrEle.appendChild(importedElementNode);
					}
				}
			}
		}

		NamedNodeMap updateAttrList = updateNode.getAttributes();
		for (int attrItr = 0; attrItr < updateAttrList.getLength(); attrItr++) {
			Node updateAttr = updateAttrList.item(attrItr);
			String updateAttrname = updateAttr.getNodeName();
			String updateAttrValue = updateAttr.getNodeValue();
			if (isSubXML(updateAttrname, updateAttrValue)) {
				System.out.println("The Attribute Value is a SUB-XML : " + updateAttrname);
				System.out.println("updateAttrname :" + updateAttrname);
				attributeEle.setAttribute("Name", updateAttrname);
			}
		}
		attributeEle.appendChild(oldAttrEle);
		attributeEle.appendChild(newAttrEle);
		enhancedCompareEle.appendChild(attributeEle);
		importedUpdateNode.appendChild(enhancedCompareEle);
		System.out.println("Exiting addEnhancedCompareToProcessedUpdateDoc : ");
		return processedUpdateEnhancedCompareDoc;
	}

	// Move Updates Doc to ManualReview
	public void moveUpdatesToManualReview(Document doc) throws Exception {
		String expression = CDTConstants.forwardSlash + CDTConstants.UPDATE;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Update nodeList Length : " + nodeList.getLength());
		String primaryKeyName = tablePrefix + CDTConstants.key;

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		// DOM Parser
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();
		Document processedManualReviewDoc = parser.newDocument();
		String parentNodeName = doc.getDocumentElement().getNodeName();
		Element parentElementEnhancedCompare = processedManualReviewDoc.createElement(parentNodeName);
		processedManualReviewDoc.appendChild(parentElementEnhancedCompare);

		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node updateNode = nodeList.item(itr);
			System.out.println("\nNode Name :" + updateNode.getNodeName());
			Element updateElement = (Element) nodeList.item(itr);
			String primaryKeyValue = updateElement.getAttribute(primaryKeyName);
			System.out.println("primaryKeyValue of Update Node : " + primaryKeyValue);

			String Xmls1Modifyts = null;
			String Xmls2Modifyts = null;

			// Getting file from Directory CDT_XMLS1
			Document cdtxmls1doc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS1, parentNodeName + ".cdt.xml");
			if (cdtxmls1doc != null) {
				NodeList Xmls1NodeList = cdtxmls1doc.getDocumentElement().getChildNodes();
				System.out.println("Xmls1NodeList Length : " + Xmls1NodeList.getLength());
				for (int Xmls1Nodeitr = 0; Xmls1Nodeitr < Xmls1NodeList.getLength(); Xmls1Nodeitr++) {
					Node xmls1Node = Xmls1NodeList.item(Xmls1Nodeitr);
					if (xmls1Node instanceof Element) {
						Element Xmls1Element = (Element) xmls1Node;
						String Xmls1PrimaryKeyValue = Xmls1Element.getAttribute(primaryKeyName);
						if (Xmls1PrimaryKeyValue.equalsIgnoreCase(primaryKeyValue)) {
							System.out.println("Xmls1PrimaryKeyValue : " + Xmls1PrimaryKeyValue);
							Xmls1Modifyts = Xmls1Element.getAttribute("Modifyts");
							System.out.println("Xmls1Modifyts for CDT_XMLS1 : " + Xmls1Modifyts);
							break;
						}
					}
				}
			}

			// Getting file from Directory CDT_XMLS2
			Document cdtxmls2doc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS2, parentNodeName + ".cdt.xml");
			if (cdtxmls2doc != null) {
				recordIdentifer.setDoc(cdtxmls2doc);
				recordIdentifer.setElemToMatch(updateElement);
				Element Xmls2ElementMatch = recordIdentifer.getMatchingUniqueElement(false);
				if (Xmls2ElementMatch != null) {
					System.out.println("Xmls2ElementMatch Node Name : " + Xmls2ElementMatch.getNodeName());
					Xmls2Modifyts = Xmls2ElementMatch.getAttribute("Modifyts");
					System.out.println("Xmls2Modifyts  for CDT_XMLS2 : " + Xmls2Modifyts);
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
				if (Xmls1Modifytsdate.before(Xmls2Modifytsdate)) {
					System.out.println("Xmls1Modifytsdate is less than Xmls2Modifytsdate");
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
				String fileName = parentNodeName + CDTConstants.xmlExtension;
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
		for (int i = length-1; i >=0; i--) {
			Node node = deleteNodesList.item(i);
			rootEle.removeChild(node);
		}
		debug(doc, "Before removeDeleteTags");
		return doc;
	}

	public void debug(Document doc, String s) throws Exception {
		String expression = CDTConstants.forwardSlash + CDTConstants.INSERT;
		NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println(s + "  Insert nodeList Length() " + nodeList.getLength());

		expression = CDTConstants.forwardSlash + CDTConstants.DELETE;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println(s + "  Delete nodeList Length() " + nodeList.getLength());

		expression = CDTConstants.forwardSlash + CDTConstants.UPDATE;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
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