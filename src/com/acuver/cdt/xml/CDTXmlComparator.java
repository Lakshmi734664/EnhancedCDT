package com.acuver.cdt.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.constants.CDTConstants;

public class CDTXmlComparator {

	Document inputDoc = null;
	Document outputDoc = null;
	Document processedInsertDeleteDoc = null;
	Document updateDoc = null;
	Document processedUpdateDoc = null;
	Document processedUpdateEnhancedCompareDoc = null;

	String tablePrefix;

	CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();

	TreeMap<Integer, String> uniqueInsertMap = new TreeMap<Integer, String>();
	TreeMap<Integer, String> duplicateInsertMap = new TreeMap<Integer, String>();

	TreeMap<Integer, Node> uniqueDeleteMap = new TreeMap<Integer, Node>();
	TreeMap<Integer, Node> duplicateDeleteMap = new TreeMap<Integer, Node>();

	TreeMap<Integer, ArrayList<String>> subXmlDiffDataMap = new TreeMap<Integer, ArrayList<String>>();
	TreeMap<Integer, Node> duplicateSubXmlDiffDataMap = new TreeMap<Integer, Node>();

	TreeMap<Integer, ArrayList<String>> uniqueDiffDataMap = new TreeMap<Integer, ArrayList<String>>();

	public Document cleanCompareReport(File f) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// An instance of builder to parse the specified xml file
		DocumentBuilder db = null;
		db = dbf.newDocumentBuilder();
		Document doc = null;
		doc = db.parse(f);
		inputDoc = doc;
		Element root = doc.getDocumentElement();
		String parentNodeName = root.getNodeName();
		System.out.println("parentNodeName : " + parentNodeName);

		// DOM Parser
		DocumentBuilder parser = EnhancedCDTMain.factory.newDocumentBuilder();

		// Create a new Processed document with Root Element
		processedInsertDeleteDoc = parser.newDocument();
		Element parentElement = processedInsertDeleteDoc.createElement(parentNodeName);
		processedInsertDeleteDoc.appendChild(parentElement);

		// Getting the Table Prefix Name
		tablePrefix = getTablePrefix(parentNodeName);
		String primaryKeyName = tablePrefix + "Key";
		System.out.println("primaryKeyName : " + primaryKeyName);
		CDTXmlDifferenceEvaluator.setPrimaryKeyName(primaryKeyName);

		// Processing Insert/Delete Tags
		processedInsertDeleteDoc = removeInsertDeleteElementsWithPrimaryIssue(inputDoc);

		// Merge Insert/Delete To Update Doc
		updateDoc = mergeInsertDeleteToUpdate(processedInsertDeleteDoc);

		// Remove False Update
		processedUpdateDoc = removeFalseUpdates(updateDoc);

		// Create a new Processed document for EnhancedCompare with Root Element
		processedUpdateEnhancedCompareDoc = parser.newDocument();
		Element parentElementEnhancedCompare = processedUpdateEnhancedCompareDoc.createElement(parentNodeName);
		processedUpdateEnhancedCompareDoc.appendChild(parentElementEnhancedCompare);

		// Processing Update Doc with EnhancedCompare
		processedUpdateEnhancedCompareDoc = addEnhancedCompareToUpdates(processedUpdateDoc);

		outputDoc = processedUpdateEnhancedCompareDoc;

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
		String primaryKeyName = String.valueOf(charArray);
		primaryKeyName = primaryKeyName.replaceAll("\\s", "");
		return primaryKeyName;
	}

	// Processing Insert/Delete Tags
	public Document removeInsertDeleteElementsWithPrimaryIssue(Document doc) throws Exception {

		String expression = "//" + CDTConstants.INSERT;
		NodeList nodeList = null;
		nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Insert nodeList Length()" + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node insertNode = nodeList.item(itr);

			// Getting Deletes from Insert.
			NodeList deleteNodeList = getDeletesForInsert(doc, insertNode);

			System.out.println("deleteNodeList length " + deleteNodeList.getLength());
			for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
				Node deleteNode = deleteNodeList.item(itr2);
				int insertNodeIndex = itr;
				int deleteNodeIndex = itr2;
				String insertNodeName = insertNode.getNodeName();
				System.out.println("Comparing insertNode with deleteNode : ");

				Diff diff = DiffBuilder.compare(insertNode).withTest(deleteNode).checkForSimilar().ignoreComments()
						.ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
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
					if (UniqueDiffDataList != null && UniqueDiffDataList.size() > 0) {
						uniqueDiffDataMap.put(insertNodeIndex, UniqueDiffDataList);
					}
					uniqueInsertMap.put(insertNodeIndex, insertNodeName);
					uniqueDeleteMap.put(deleteNodeIndex, deleteNode);
				} else {
					duplicateInsertMap.put(insertNodeIndex, insertNodeName);
					duplicateDeleteMap.put(deleteNodeIndex, deleteNode);
					System.out.println(
							"No Difference In insertNode and deleteNode.Both Insert and Delete Nodes are removed.");
					break;
				}
			}
		}
		addUniqueElementsToProcessedDoc();
		return processedInsertDeleteDoc;

	}

	public void addUniqueElementsToProcessedDoc() {

		if (uniqueInsertMap.size() > 0) {
			for (Entry<Integer, String> m : uniqueInsertMap.entrySet()) {
				Node insertNode = inputDoc.getElementsByTagName(m.getValue()).item(m.getKey());
				Node importedInsertNode = processedInsertDeleteDoc.importNode(insertNode, true);
				if (duplicateInsertMap.size() == 0) {
					processedInsertDeleteDoc.getDocumentElement().appendChild(importedInsertNode);
				} else {
					if (!duplicateInsertMap.containsKey(m.getKey())) {
						processedInsertDeleteDoc.getDocumentElement().appendChild(importedInsertNode);
					}
				}
			}
		}
		if (uniqueDeleteMap.size() > 0) {
			for (Entry<Integer, Node> n : uniqueDeleteMap.entrySet()) {
				Node importedDeleteNode = processedInsertDeleteDoc.importNode(n.getValue(), true);
				if (duplicateDeleteMap.size() == 0) {
					processedInsertDeleteDoc.getDocumentElement().appendChild(importedDeleteNode);
				} else if (!duplicateDeleteMap.containsKey(n.getKey())) {
					processedInsertDeleteDoc.getDocumentElement().appendChild(importedDeleteNode);
				}
			}
		}

		processedInsertDeleteDoc.normalizeDocument();
	}

	// Merge Insert/Delete To Update Doc
	public Document mergeInsertDeleteToUpdate(Document doc) throws Exception {

		return doc;
	}

	// Remove False Update
	public Document removeFalseUpdates(Document doc) throws Exception {

		return doc;
	}

	// Processing Update Doc with EnhancedCompare
	public Document addEnhancedCompareToUpdates(Document doc) throws Exception {

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
					System.out.println("Attribute is Sub XML : " + oldValueAttrName);
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
								ArrayList<String> subXmlDiffDataList = new ArrayList<String>();
								while (iter.hasNext()) {
									String subXmlDiffData = iter.next().toString();
									if (!(subXmlDiffData.contains("Expected attribute name"))) {
										subXmlDiffDataList.add(subXmlDiffData);
									}
								}
								if (subXmlDiffDataList != null && subXmlDiffDataList.size() > 0) {
									subXmlDiffDataMap.put(oldValueIndex, subXmlDiffDataList);
								}
							} else {
								duplicateSubXmlDiffDataMap.put(oldValueIndex, updateNode);
								System.out.println("No Difference in Sub XML : ");

							}
						}
					}
				}
			}
		}
		addEnhancedCompareToProcessedUpdateDoc();
		return processedUpdateEnhancedCompareDoc;
	}

	// Add Enhanced Compare To Processed Update Doc
	public void addEnhancedCompareToProcessedUpdateDoc() {

		if (subXmlDiffDataMap.size() > 0) {
			for (Entry<Integer, ArrayList<String>> n : subXmlDiffDataMap.entrySet()) {

				Node updateNode = processedUpdateDoc.getElementsByTagName(CDTConstants.UPDATE).item(n.getKey());
				Node importedUpdateNode = processedUpdateEnhancedCompareDoc.importNode(updateNode, true);
				processedUpdateEnhancedCompareDoc.getDocumentElement().appendChild(importedUpdateNode);

				Element enhancedCompareEle = processedUpdateEnhancedCompareDoc.createElement("EnhancedCompare");
				Element attributeEle = processedUpdateEnhancedCompareDoc.createElement("Attribute");
				Element oldAttrEle = processedUpdateEnhancedCompareDoc.createElement("Old");
				Element newAttrEle = processedUpdateEnhancedCompareDoc.createElement("New");

				ArrayList<String> valuesList = new ArrayList<String>();
				valuesList = n.getValue();
				System.out.println("valuesList size: " + valuesList.size());

				for (int i = 0; i < valuesList.size(); i++) {
					String diffValues = valuesList.get(i);
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
						System.out.println("updateAttrname :" + updateAttrname);
						attributeEle.setAttribute("Name", updateAttrname);
					}
				}
				attributeEle.appendChild(oldAttrEle);
				attributeEle.appendChild(newAttrEle);
				enhancedCompareEle.appendChild(attributeEle);
				if (!duplicateSubXmlDiffDataMap.containsKey(n.getKey())) {
					importedUpdateNode.appendChild(enhancedCompareEle);
				}
			}
			processedUpdateEnhancedCompareDoc.normalizeDocument();
		}
	}

	// Checking if the Attribute is Sub XML
	public boolean isSubXML(String name, String value) {

		return name.endsWith("XML") || name.endsWith("Xml");
	}

	// Get Delete NodeList for Insert Node
	public NodeList getDeletesForInsert(Document doc, Node insertNode) throws XPathExpressionException {
		System.out.println("\nNode Name :" + insertNode.getNodeName());
		NamedNodeMap attrList = insertNode.getAttributes();
		String expression;
		NodeList deleteNodeList = null;
		for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
			Node attr = attrList.item(attrItr);
			String name = attr.getNodeName();
			if (name.endsWith("Name")) {
				if (attr.getNodeValue().trim().isEmpty()) {
					expression = "//" + CDTConstants.DELETE;
				} else {
					expression = "//" + CDTConstants.DELETE + "[@" + name + "=\'" + attr.getNodeValue() + "\']";
				}
				System.out.println("\nNode expression :" + expression);
				deleteNodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc,
						XPathConstants.NODESET);
			}
		}
		return deleteNodeList;
	}

}
