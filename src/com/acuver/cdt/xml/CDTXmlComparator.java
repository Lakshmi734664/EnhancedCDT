package com.acuver.cdt.xml;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.constants.CDTConstants;
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;

public class CDTXmlComparator {

	Document inputDoc = null;
	Document outputDoc = null;
	Document updateDoc = null;
	Document processedUpdateDoc = null;
	Document processedManualReviewDoc = null;

	String tablePrefix;
	String parentNodeName;

	CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();
	ArrayList<String> mergeInsertDataList = new ArrayList<String>();
	TreeMap<Integer, ArrayList<String>> mergeInsertDataMap = new TreeMap<Integer, ArrayList<String>>();

	public Document cleanCompareReport(File f) throws Exception {

		DocumentBuilder db = null;
		db = EnhancedCDTMain.factory.newDocumentBuilder();
		Document doc = null;
		doc = db.parse(f);
		inputDoc = doc;
		Element root = doc.getDocumentElement();
		parentNodeName = root.getNodeName();
		System.out.println("parentNodeName : " + parentNodeName);

		// Getting the Table Prefix Name
		tablePrefix = getTablePrefix(parentNodeName);
		String primaryKeyName = tablePrefix + "Key";
		System.out.println("primaryKeyName : " + primaryKeyName);
		CDTXmlDifferenceEvaluator.setPrimaryKeyName(primaryKeyName);

		// Processing Insert/Delete Tags
		Document processedInsertDeleteDoc = removeInsertDeleteElementsWithPrimaryIssue(inputDoc);

		// Merge Insert/Delete To Update Doc
		updateDoc = mergeInsertDeleteToUpdate(processedInsertDeleteDoc);

		// Remove False Update
		processedUpdateDoc = removeFalseUpdates(updateDoc);

		// Processing Update Doc with EnhancedCompare
		Document processedUpdateEnhancedCompareDoc = addEnhancedCompareToUpdates(processedUpdateDoc);

		moveUpdatesToManualReview(processedUpdateEnhancedCompareDoc);

		outputDoc = processedManualReviewDoc;

		Document removeDeleteTag = removeDeleteTags(processedInsertDeleteDoc);

		return outputDoc;

	}

	// Get Table Primary Key Name
	public String getTablePrefix(String tableName) {
		int beginIndex = tableName.indexOf("_");
		String name = tableName.substring(beginIndex).toLowerCase();
		name = name.replace("_", " ");
		System.out.println("name: " + name);
		char[] charArray = name.toCharArray();
		boolean foundSpace = true;
		for (int i = 0; i < charArray.length; i++) {
			if (Character.isLetter(charArray[i])) {
				if (foundSpace) {
					charArray[i] = Character.toUpperCase(charArray[i]);
					foundSpace = false;
				}
			} else {
				foundSpace = true;
			}
		}
		String tablePrefix = String.valueOf(charArray);
		tablePrefix = tablePrefix.replaceAll("\\s", "");
		return tablePrefix;
	}

	// Processing Insert/Delete Tags
	public Document removeInsertDeleteElementsWithPrimaryIssue(Document doc) throws Exception {

		Document processedInsertDeleteDoc = null;
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();
		// Create a new Processed document with Root Element
		processedInsertDeleteDoc = parser.newDocument();
		Element parentElement = processedInsertDeleteDoc.createElement(parentNodeName);
		processedInsertDeleteDoc.appendChild(parentElement);

		String expression = "//" + CDTConstants.INSERT;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		if (nodeList == null || nodeList.getLength() == 0) {
			System.out.println("No Insert Nodes in Input Doc : Returning All the Delete Nodes in Output ");
			return doc;
		}

		System.out.println("Insert nodeList Length : " + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Element insertElement = (Element) nodeList.item(itr);

			// Getting Deletes from Insert.
			NodeList deleteNodeList = getDeletesForInsert(doc, insertElement);

			if (deleteNodeList == null || deleteNodeList.getLength() == 0) {
				System.out.println("No Delete Nodes for this Insert Node : " + itr);

				// Adding Unique Insert Node
				processedInsertDeleteDoc = addUniqueElementsToProcessedDoc(processedInsertDeleteDoc, insertElement,
						null);

			} else {

				System.out.println("Delete NodeList length : " + deleteNodeList.getLength());
				for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
					Element deleteElement = (Element) deleteNodeList.item(itr2);
					System.out.println("Comparing insertElement with deleteElement : ");

					Diff diff = DiffBuilder.compare(insertElement).withTest(deleteElement).checkForSimilar()
							.ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
							.withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

					if (diff.hasDifferences()) {
						Iterator<Difference> iter = diff.getDifferences().iterator();
						ArrayList<String> UniqueDiffDataList = new ArrayList<String>();
						while (iter.hasNext()) {
							String datadifference = iter.next().toString();
							if (datadifference != null && !datadifference.contains("xml version")) {
								UniqueDiffDataList.add(datadifference);
							}
						}
						System.out.println("UniqueDiffDataList : " + UniqueDiffDataList.toString());

						// Adding Unique Elements to Output
						processedInsertDeleteDoc = addUniqueElementsToProcessedDoc(processedInsertDeleteDoc,
								insertElement, deleteElement);

					} else {
						System.out.println(
								"No Difference In insertElement and deleteElement.Both Insert and Delete Elements are removed.");
						break;
					}
				}
			}

		}

		// Adding Unique Delete Elements to Output
		processedInsertDeleteDoc = addUniqueDeleteElementsToProcessedDoc(processedInsertDeleteDoc, doc);

		return processedInsertDeleteDoc;

	}

	// Add Unique Elements To Processed Doc
	public Document addUniqueElementsToProcessedDoc(Document processedInsertDeleteDoc, Node insertNode,
			Node deleteNode) {
		if (insertNode != null) {
			Node importedInsertNode = processedInsertDeleteDoc.importNode(insertNode, true);
			processedInsertDeleteDoc.getDocumentElement().appendChild(importedInsertNode);
		}
		if (deleteNode != null) {
			Node importedDeleteNode = processedInsertDeleteDoc.importNode(deleteNode, true);
			processedInsertDeleteDoc.getDocumentElement().appendChild(importedDeleteNode);
		}
		return processedInsertDeleteDoc;
	}

	// Add Unique Delete Elements To Processed Doc
	public Document addUniqueDeleteElementsToProcessedDoc(Document processedInsertDeleteDoc, Document doc)
			throws XPathExpressionException {
		NodeList deleteNodeList = null;
		String expression = "//" + CDTConstants.DELETE;
		deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		for (int itr = 0; itr < deleteNodeList.getLength(); itr++) {
			Element deleteElement = (Element) deleteNodeList.item(itr);
			NodeList InsertNodeList = getInsertsForDelete(doc, deleteElement);
			if (InsertNodeList == null || InsertNodeList.getLength() == 0) {
				System.out
						.println("No Insert Nodes for this Delete Node : Hence this is a Unique Delete Element " + itr);
				processedInsertDeleteDoc = addUniqueElementsToProcessedDoc(processedInsertDeleteDoc, null,
						deleteElement);
			}
		}
		return processedInsertDeleteDoc;
	}

	// Get Inserts For Delete Element
	public NodeList getInsertsForDelete(Document doc, Element deleteElement) throws XPathExpressionException {

		System.out.println("\nNode Name in getInsertsForDelete(): " + deleteElement.getNodeName());
		String expression = null;
		NodeList insertNodeList = null;
		String tablePrefixLower = tablePrefix.toLowerCase();
		String attrWithName = tablePrefixLower + "name";
		String attrWithId = tablePrefixLower + "id";
		String attrWithCode = tablePrefixLower + "code";
		System.out.println("tablePrefixLower : " + tablePrefixLower);
		String primaryKeyName = tablePrefix + "Key";
		String primaryKeyValue = deleteElement.getAttribute(primaryKeyName);
		System.out.println("primaryKeyValue in getInsertsForDelete() " + primaryKeyValue);
		if (primaryKeyValue != null && !primaryKeyValue.isEmpty()) {
			String expressionForPrimaryKey = "//" + CDTConstants.INSERT
					+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
					+ primaryKeyName.toLowerCase() + "']='" + primaryKeyValue + "']";
			System.out.println("\nNode expression For Primary Key :" + expressionForPrimaryKey);
			insertNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expressionForPrimaryKey).evaluate(doc,
					XPathConstants.NODESET);
		}
		if (insertNodeList == null || insertNodeList.getLength() == 0) {
			System.out.println("No Insert Elements are not present for the Primary Key of Delete Element : ");
			String organizationCode = "OrganizationCode";
			String organizationCodeValue = deleteElement.getAttribute(organizationCode);
			String processTypeKey = "ProcessTypeKey";
			String processTypeKeyValue = deleteElement.getAttribute(processTypeKey);
			NamedNodeMap attrList = deleteElement.getAttributes();
			for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
				Node attr = attrList.item(attrItr);
				String attrName = attr.getNodeName();
				String attrValue = attr.getNodeValue();
				if (attrName.equalsIgnoreCase(tablePrefixLower)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";

						}
						System.out.println("\nNode expression :" + expression);
						insertNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				} else if (attrName.equalsIgnoreCase(attrWithName)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";
						}
						System.out.println("\nNode expression :" + expression);
						insertNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				} else if (attrName.equalsIgnoreCase(attrWithId)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";

						}
						System.out.println("\nNode expression :" + expression);
						insertNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				} else if (attrName.equalsIgnoreCase(attrWithCode)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.INSERT
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";
						}
						System.out.println("\nNode expression :" + expression);
						insertNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				}
			}

		}
		System.out.println("Insert NodeList Length in getInsertsForDelete() : " + insertNodeList.getLength());

		return insertNodeList;

	}

	// Merge Insert/Delete To Update Doc
	public Document mergeInsertDeleteToUpdate(Document doc) throws Exception {

		String expression = "//" + CDTConstants.INSERT;
		NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Insert nodeList Length()" + nodeList.getLength());

		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Element insertElement = (Element) nodeList.item(itr);

			// Getting Deletes from Insert.
			NodeList deleteNodeList = getDeletesForInsert(doc, insertElement);

			System.out.println("deleteNodeList length " + deleteNodeList.getLength());
			for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
				Element deleteElement = (Element) deleteNodeList.item(itr2);

				boolean insertHasDifferences = false;
				System.out.println("Comparing insertNode with deleteNode : ");

				Diff diff = DiffBuilder.compare(insertElement).withTest(deleteElement).checkForSimilar()
						.ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
						.withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

				if (diff.hasDifferences()) {
					Element oldValuesElement = getChildElement(insertElement, "OldValues");

					// Store the difference values in the OldValues element
					Iterator<Difference> iter = diff.getDifferences().iterator();
					while (iter.hasNext()) {
						String difference = iter.next().toString();
						System.out.println(difference);

						if (difference != null && !difference.contains("xml version")) {
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
					}
					insertHasDifferences = true;

				} else {
					System.out.println(
							"No Difference In insertNode and deleteNode. Both Insert and Delete Nodes are removed.");
				}

				if (insertHasDifferences) {
					// Replace Insert element with Update element
					Element updateElement = doc.createElement("Update");

					NamedNodeMap attributes = insertElement.getAttributes();
					for (int j = 0; j < attributes.getLength(); j++) {
						Node attribute = attributes.item(j);
						updateElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());

					}

					String primaryKeyName = tablePrefix + "Key";
					System.out.println(primaryKeyName);
					String primaryKeyValue = deleteElement.getAttribute(primaryKeyName);
					System.out.println(primaryKeyValue);

					updateElement.setAttribute(primaryKeyName, primaryKeyValue);

					doc.getDocumentElement().removeChild(insertElement);
					doc.getDocumentElement().removeChild(deleteElement);
					doc.getDocumentElement().appendChild(updateElement);

					Element oldValues = (Element) ((Element) insertElement).getElementsByTagName("OldValues").item(0);

					// Create a new "OldValues" element in the "Update" element with the same
					if (oldValues != null) {
						Element oldValuesElement = doc.createElement("OldValues");
						NamedNodeMap oldValuesAttributes = oldValues.getAttributes();
						for (int j = 0; j < oldValuesAttributes.getLength(); j++) {
							Node oldValuesAttribute = oldValuesAttributes.item(j);
							oldValuesElement.setAttribute(oldValuesAttribute.getNodeName(),
									oldValuesAttribute.getNodeValue());
						}
						updateElement.appendChild(oldValuesElement);
					}

				}
			}
		}
		updateDoc = doc;

		return updateDoc;
	}

	public Element getChildElement(Element insert, String childName) {
		NodeList childNodes = insert.getChildNodes();
		Element childElement = null;
		// Look for existing child element and update it
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getNodeName().equals(childName)) {
				childElement = (Element) childNode;
				break;
			}
		}
		// Create new child element if it doesn't exist
		if (childElement == null) {
			Document doc = insert.getOwnerDocument();
			childElement = doc.createElement(childName);
			insert.appendChild(childElement);
		}

		return childElement;
	}

	// Remove False Update
	public Document removeFalseUpdates(Document doc) throws Exception {

		String expression = "//" + CDTConstants.UPDATE + "//" + CDTConstants.OLDVALUES;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Old Values nodeList Length()" + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node oldValueNode = nodeList.item(itr);
			System.out.println("\nNode Name :" + oldValueNode.getNodeName());
			NamedNodeMap attrList = oldValueNode.getAttributes();
			int oldValueIndex = itr;
			for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
				Node oldValueAttr = attrList.item(attrItr);
				String oldValueAttrName = oldValueAttr.getNodeName();
				String oldValueAttrValue = oldValueAttr.getNodeValue();
				if (isSubXML(oldValueAttrName, oldValueAttrValue)) {
					System.out.println("The Attribute Value is a SUB-XML : " + oldValueAttrName);
					Node updateNode = oldValueNode.getParentNode();
					NamedNodeMap updateAttrList = updateNode.getAttributes();
					Diff diff = null;
					for (int attrItr2 = 0; attrItr2 < updateAttrList.getLength(); attrItr2++) {
						Node updateAttr = updateAttrList.item(attrItr2);
						String updateAttrname = updateAttr.getNodeName();
						String updateAttrValue = updateAttr.getNodeValue();
						if (oldValueAttrName.equalsIgnoreCase(updateAttrname)) {
							System.out.println("Inside Sub XML Diff : ");
							diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue).checkForSimilar()
									.ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace()
									.normalizeWhitespace().withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

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
		processedUpdateDoc = doc;
		return processedUpdateDoc;

	}

	// Processing Update Doc with EnhancedCompare
	public Document addEnhancedCompareToUpdates(Document doc) throws Exception {

		Document processedUpdateEnhancedCompareDoc = doc;
		String expression = "//" + CDTConstants.UPDATE + "//" + CDTConstants.OLDVALUES;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

		if (nodeList == null || nodeList.getLength() == 0) {

			return doc;

		} else {

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
								diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue)
										.checkForSimilar().ignoreComments().ignoreWhitespace()
										.ignoreElementContentWhitespace().normalizeWhitespace()
										.withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

								if (diff != null && diff.hasDifferences()) {
									Iterator<Difference> iter = diff.getDifferences().iterator();
									ArrayList<String> subXmlDiffDataList = new ArrayList<String>();
									while (iter.hasNext()) {
										String subXmlDiffData = iter.next().toString();
										if (!(subXmlDiffData.contains("Expected attribute name"))) {
											subXmlDiffDataList.add(subXmlDiffData);
										}
									}
									if (subXmlDiffDataList != null && subXmlDiffDataList.size() > 0) {
										processedUpdateEnhancedCompareDoc = addEnhancedCompareToProcessedUpdateDoc(
												processedUpdateEnhancedCompareDoc, updateNode, subXmlDiffDataList);
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

		// Can Add Method here if Unique Insert Delete Elements are there along with
		// Update Elements

		return processedUpdateEnhancedCompareDoc;
	}

	// Add Enhanced Compare Elements To Processed Update Doc
	public Document addEnhancedCompareToProcessedUpdateDoc(Document processedUpdateEnhancedCompareDoc, Node updateNode,
			ArrayList<String> subXmlDiffDataList) {

		Element enhancedCompareEle = processedUpdateEnhancedCompareDoc.createElement("EnhancedCompare");
		Element attributeEle = processedUpdateEnhancedCompareDoc.createElement("Attribute");
		Element oldAttrEle = processedUpdateEnhancedCompareDoc.createElement("Old");
		Element newAttrEle = processedUpdateEnhancedCompareDoc.createElement("New");

		for (int i = 0; i < subXmlDiffDataList.size(); i++) {
			String diffValues = subXmlDiffDataList.get(i);
			System.out.println("diffValues in subXmlDiffDataMap : " + diffValues);
			int beginIndex = diffValues.indexOf("@") + 1;
			int endIndex = diffValues.indexOf("to");
			String attrName = diffValues.substring(beginIndex, endIndex).trim();
			System.out.println("AttrName in subXmlDiffDataMap :" + attrName);
			int oldBeginIndex = diffValues.indexOf("value '") + 7;
			int oldEndIndex = diffValues.indexOf("' but");
			String oldAttrValue = diffValues.substring(oldBeginIndex, oldEndIndex).trim();
			System.out.println("oldAttrValue in subXmlDiffDataMap :" + oldAttrValue);
			int newBeginIndex = diffValues.indexOf("but was '") + 9;
			int newEndIndex = diffValues.indexOf("- comparing") - 2;
			String newAttrValue = diffValues.substring(newBeginIndex, newEndIndex).trim();
			System.out.println("newAttrValue in subXmlDiffDataMap :" + newAttrValue);
			oldAttrEle.setAttribute(attrName, oldAttrValue);
			newAttrEle.setAttribute(attrName, newAttrValue);
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
		updateNode.appendChild(enhancedCompareEle);

		return processedUpdateEnhancedCompareDoc;
	}

	// Move Updates Doc to ManualReview
	public void moveUpdatesToManualReview(Document doc) throws Exception {
		String expression = "//" + CDTConstants.UPDATE;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Update nodeList Length : " + nodeList.getLength());
		String primaryKeyName = tablePrefix + "Key";
		CDTFileWriter fileWriter = new CDTFileWriter();
		CDTFileReader fileReader = new CDTFileReader();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		// DOM Parser
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();
		processedManualReviewDoc = parser.newDocument();
		String parentNodeName = doc.getDocumentElement().getNodeName();
		Element parentElementEnhancedCompare = processedManualReviewDoc.createElement(parentNodeName);
		processedManualReviewDoc.appendChild(parentElementEnhancedCompare);

		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node updateNode = nodeList.item(itr);
			System.out.println("\nNode Name :" + updateNode.getNodeName());
			Element updateElement = (Element) nodeList.item(itr);
			String primaryKeyValue = updateElement.getAttribute(primaryKeyName);
			System.out.println("primaryKeyValue of Update Node : " + primaryKeyValue);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			// An instance of builder to parse the specified xml file
			DocumentBuilder db = null;
			db = dbf.newDocumentBuilder();
			Document cdtxmls1doc = null;
			Document cdtxmls2doc = null;
			Element Xmls1Element = null;
			Element Xmls2Element = null;
			String Xmls1Modifyts = null;
			String Xmls2Modifyts = null;

			// Getting file from Directory
			String file1Data = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS1, parentNodeName);
			if (file1Data != null && !file1Data.isEmpty()) {
				cdtxmls1doc = db.parse(file1Data);
				NodeList Xmls1NodeList = cdtxmls1doc.getDocumentElement().getChildNodes();
				System.out.println("Xmls1NodeList Length : " + Xmls1NodeList.getLength());
				for (int Xmls1Nodeitr = 0; Xmls1Nodeitr < Xmls1NodeList.getLength(); Xmls1Nodeitr++) {
					Node xmls1Node = Xmls1NodeList.item(Xmls1Nodeitr);
					if (xmls1Node instanceof Element) {
						Xmls1Element = (Element) xmls1Node;
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

			// Getting file from Directory
			String file2Data = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS2, parentNodeName);
			if (file2Data != null && !file2Data.isEmpty()) {
				cdtxmls2doc = db.parse(file2Data);
				NodeList Xmls2NodeList = cdtxmls2doc.getDocumentElement().getChildNodes();
				System.out.println("Xmls2NodeList Length : " + Xmls2NodeList.getLength());
				for (int Xmls2Nodeitr = 0; Xmls2Nodeitr < Xmls2NodeList.getLength(); Xmls2Nodeitr++) {
					Node xmls2Node = Xmls2NodeList.item(Xmls2Nodeitr);
					if (xmls2Node instanceof Element) {
						Xmls2Element = (Element) xmls2Node;
						String Xmls2PrimaryKeyValue = Xmls2Element.getAttribute(primaryKeyName);
						if (Xmls2PrimaryKeyValue.equalsIgnoreCase(primaryKeyValue)) {
							System.out.println("Xmls2PrimaryKeyValue : " + Xmls2PrimaryKeyValue);
							Xmls2Modifyts = Xmls2Element.getAttribute("Modifyts");
							System.out.println("Xmls2Modifyts for CDT_XMLS2 : " + Xmls2Modifyts);
							break;
						}
					}
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
				}
			}
		}

		// Writing the File into Output Directory
		try {
			if (processedManualReviewDoc.getChildNodes().getLength() > 0) {
				String fileName = parentNodeName + ".xml";
				fileWriter.fileWriterMethod(EnhancedCDTMain.CDT_REPORT_DIR1_OUT, processedManualReviewDoc, fileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Checking if the Attribute is Sub XML
	public boolean isSubXML(String attrName, String attrValue) {

		return attrValue.startsWith("<?xml");
	}

	// Get Delete NodeList for Insert Node
	public NodeList getDeletesForInsert(Document doc, Element insertElement) throws XPathExpressionException {
		System.out.println("\nNode Name :" + insertElement.getNodeName());
		String expression = null;
		NodeList deleteNodeList = null;
		String tablePrefixLower = tablePrefix.toLowerCase();
		String attrWithName = tablePrefixLower + "name";
		String attrWithId = tablePrefixLower + "id";
		String attrWithCode = tablePrefixLower + "code";
		System.out.println("tablePrefixLower : " + tablePrefixLower);
		String primaryKeyName = tablePrefix + "Key";
		String primaryKeyValue = insertElement.getAttribute(primaryKeyName);
		System.out.println("primaryKeyValue in getDeletesForInsert() " + primaryKeyValue);
		if (primaryKeyValue != null && !primaryKeyValue.isEmpty()) {
			String expressionForPrimaryKey = "//" + CDTConstants.DELETE
					+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
					+ primaryKeyName.toLowerCase() + "']='" + primaryKeyValue + "']";
			System.out.println("\nNode expression For Primary Key :" + expressionForPrimaryKey);
			deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expressionForPrimaryKey).evaluate(doc,
					XPathConstants.NODESET);
		}
		if (deleteNodeList == null || deleteNodeList.getLength() == 0) {
			System.out.println("No Delete Elements are not present for the Primary Key of Insert Element : ");
			String organizationCode = "OrganizationCode";
			String organizationCodeValue = insertElement.getAttribute(organizationCode);
			String processTypeKey = "ProcessTypeKey";
			String processTypeKeyValue = insertElement.getAttribute(processTypeKey);
			NamedNodeMap attrList = insertElement.getAttributes();
			for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
				Node attr = attrList.item(attrItr);
				String attrName = attr.getNodeName();
				String attrValue = attr.getNodeValue();
				if (attrName.equalsIgnoreCase(tablePrefixLower)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";

						}
						System.out.println("\nNode expression :" + expression);
						deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				} else if (attrName.equalsIgnoreCase(attrWithName)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";
						}
						System.out.println("\nNode expression :" + expression);
						deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				} else if (attrName.equalsIgnoreCase(attrWithId)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";

						}
						System.out.println("\nNode expression :" + expression);
						deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				} else if (attrName.equalsIgnoreCase(attrWithCode)) {
					if (!(attrValue.trim().isEmpty())) {
						if (organizationCodeValue != null && !organizationCodeValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ organizationCode.toLowerCase() + "']='" + organizationCodeValue + "']";

						} else if (processTypeKeyValue != null && !processTypeKeyValue.isEmpty()) {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "' and "
									+ "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ processTypeKey.toLowerCase() + "']='" + processTypeKeyValue + "']";

						} else {
							expression = "//" + CDTConstants.DELETE
									+ "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
									+ attrName.toLowerCase() + "']='" + attrValue + "']";
						}
						System.out.println("\nNode expression :" + expression);
						deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
								XPathConstants.NODESET);
					}
					break;
				}
			}

		}
		System.out.println("Delete NodeList Length in getDeletesForInsert() " + deleteNodeList.getLength());
		return deleteNodeList;
	}

	// remove delete tag from document
	public Document removeDeleteTags(Document doc) throws Exception {

		NodeList nodes = doc.getElementsByTagName("Delete");

		for (int i = nodes.getLength() - 1; i >= 0; i--) {
			Node node = nodes.item(i);
			node.getParentNode().removeChild(node);
		}

		// Create a transformer to serialize the document to a string
		Transformer transformer = EnhancedCDTMain.tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(new DOMSource(doc), result);

		// Remove all blank lines from the serialized string
		String xmlString = writer.toString();
		Pattern pattern = Pattern.compile("(?m)^\\s*$[\n\r]{1,}", Pattern.MULTILINE);
		xmlString = pattern.matcher(xmlString).replaceAll("");

		// Parse the modified string back into a DOM document
		DocumentBuilder builder = EnhancedCDTMain.factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new StringReader(xmlString));
		Document doc1 = builder.parse(inputSource);
		return doc1;

	}

}
