package com.acuver.cdt.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

public class CDTXmlComparator {

	Document inputDoc = null;
	Document outputDoc = null;
	Document processedInsertDeleteDoc = null;
	Document updateDoc = null;
	Document processedUpdateDoc = null;
	Document processedUpdateEnhancedCompareDoc = null;
	String primaryKeyName;
	XPath xPath = XPathFactory.newInstance().newXPath();

	ArrayList<String> subXmlDiffDataList = new ArrayList<String>();
	TreeMap<Integer, ArrayList<String>> subXmlDiffDataMap = new TreeMap<Integer, ArrayList<String>>();

	ArrayList<String> UniqueDiffDataList = new ArrayList<String>();
	TreeMap<Integer, ArrayList<String>> uniqueDiffDataMap = new TreeMap<Integer, ArrayList<String>>();

	public Document cleanCompareReport(File f) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// an instance of builder to parse the specified xml file
		DocumentBuilder db = null;
		db = dbf.newDocumentBuilder();
		Document doc = null;
		doc = db.parse(f);
		inputDoc = doc;
		Element root = doc.getDocumentElement();
		String parentNodeName = root.getNodeName();
		System.out.println("parentNodeName : " + parentNodeName);

		/* Create DOM Parser */
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder(); // DOM Parser

		// Create a new Processed document with Root Element
		processedInsertDeleteDoc = parser.newDocument();
		Element parentElement = processedInsertDeleteDoc.createElement(parentNodeName);
		processedInsertDeleteDoc.appendChild(parentElement);

		// Getting the Table Prefix Name
		primaryKeyName = getTablePrefix(parentNodeName) + "Key";
		System.out.println("primaryKeyName : " + primaryKeyName);
		CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();
		CDTXmlDifferenceEvaluator.setPrimaryKeyName(primaryKeyName);

		// Processing Insert/Delete Tags
		processedInsertDeleteDoc = removeInsertDeleteElementsWithPrimaryIssue(inputDoc);

		// Merge Insert/Delete To Update Doc
		updateDoc = mergeInsertDeleteToUpdate(processedInsertDeleteDoc);

		// Remove False Update
		processedUpdateDoc = removeFalseUpdates(updateDoc);

		// Processing Update Doc with EnhancedCompare
		processedUpdateEnhancedCompareDoc = addEnhancedCompareToUpdates(processedUpdateDoc);

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

		String expression = "//Insert";
		NodeList nodeList = null;
		nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Insert nodeList Length()" + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node insertNode = nodeList.item(itr);
			System.out.println("\nNode Name :" + insertNode.getNodeName());
			NamedNodeMap attrList = insertNode.getAttributes();

			for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
				Node attr = attrList.item(attrItr);
				String InsertAttrName = attr.getNodeName();
				if (InsertAttrName.endsWith("Name")) {
					if (attr.getNodeValue().trim().isEmpty())
						expression = "//Delete";

					else
						expression = "//Delete[@" + InsertAttrName + "=\'" + attr.getNodeValue() + "\']";

					System.out.println("\nNode expression :" + expression);
					NodeList deleteNodeList = null;
					deleteNodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
					System.out.println("deleteNodeList length " + deleteNodeList.getLength());
					for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
						Node deleteNode = deleteNodeList.item(itr2);
						int insertNodeIndex = itr;
						System.out.println("Comparing insertNode with deleteNode : ");

						Diff diff = DiffBuilder.compare(insertNode).withTest(deleteNode).checkForSimilar()
								.ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace()
								.normalizeWhitespace().withDifferenceEvaluator(new CDTXmlDifferenceEvaluator()).build();

						if (diff.hasDifferences()) {
							Iterator<Difference> iter = diff.getDifferences().iterator();
							System.out.println(
									"Delete Attribute name:" + deleteNode.getAttributes().item(attrItr).getNodeValue());
							while (iter.hasNext()) {
								String datadifference = iter.next().toString();
								if (datadifference != null && !datadifference.contains("xml version")) {
									UniqueDiffDataList.add(datadifference);
								}
							}
							if (UniqueDiffDataList != null && UniqueDiffDataList.size() > 0) {
								uniqueDiffDataMap.put(insertNodeIndex, UniqueDiffDataList);
							}
							addUniqueElementsToProcessedDoc(insertNode, deleteNode, insertNodeIndex);
							break;
						} else {
							System.out.println(
									"No Difference In insertNode and deleteNode.Both Insert and Delete Nodes are removed.");
						}
					}
				}
				if (isSubXML(InsertAttrName, attr.getNodeValue())) {
					System.out.println("name ends With XML : " + InsertAttrName);
					expression = "//Delete";
					System.out.println("\nNode expression In Sub XML : " + expression);
					NodeList deleteNodeList = null;
					deleteNodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
					System.out.println("deleteNodeList length In Sub XML : " + deleteNodeList.getLength());
					for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
						Node node2 = deleteNodeList.item(itr2);
						System.out.println("comparing Sub XML : ");
						Diff diff = null;
						NamedNodeMap deleteAttrList = node2.getAttributes();
						for (int delAttrItr = 0; delAttrItr < deleteAttrList.getLength(); delAttrItr++) {
							Node delAttr = deleteAttrList.item(delAttrItr);
							String delAtrrName = delAttr.getNodeName();
							if (delAtrrName.equalsIgnoreCase(InsertAttrName)) {
								System.out.println("Inside Sub XML : ");

								diff = DiffBuilder.compare(attr.getNodeValue()).withTest(delAttr.getNodeValue())
										.checkForSimilar().ignoreComments().ignoreWhitespace()
										.ignoreElementContentWhitespace().normalizeWhitespace()
										.withDifferenceEvaluator(new CDTXmlDifferenceEvaluator()).build();
							}
						}
						if (diff != null && diff.hasDifferences()) {
							Iterator<Difference> iter = diff.getDifferences().iterator();
							while (iter.hasNext()) {
								System.out.println("*****Difference Details in Sub XML*********************");
								String subXmldiffData = iter.next().toString();
								if (subXmldiffData != null) {
									subXmlDiffDataList.add(subXmldiffData);
								}
							}
							int insertNodeIndex = itr;
							if (subXmlDiffDataList != null && subXmlDiffDataList.size() > 0) {
								subXmlDiffDataMap.put(insertNodeIndex, subXmlDiffDataList);
							}
							break;
						} else {
							System.out.println("No Difference in Sub XML : ");
						}
					}

				}
			}
		}
		return processedInsertDeleteDoc;

	}

	public void addUniqueElementsToProcessedDoc(Node insertNode, Node deleteNode, int insertNodeIndex) {

		Node importedInsertNode = processedInsertDeleteDoc.importNode(insertNode, true);
		processedInsertDeleteDoc.getDocumentElement().appendChild(importedInsertNode);

		Node importedDeleteNode = processedInsertDeleteDoc.importNode(deleteNode, true);
		processedInsertDeleteDoc.getDocumentElement().appendChild(importedDeleteNode);

		if (subXmlDiffDataMap.size() > 0) {
			for (Entry<Integer, ArrayList<String>> n : subXmlDiffDataMap.entrySet()) {
				if (insertNodeIndex == (n.getKey())) {
					Element enhancedCompareEle = processedInsertDeleteDoc.createElement("EnhancedCompare");
					Element attributeEle = processedInsertDeleteDoc.createElement("Attribute");
					Element oldAttrEle = processedInsertDeleteDoc.createElement("Old");
					Element newAttrEle = processedInsertDeleteDoc.createElement("New");
					ArrayList<String> valuesList = n.getValue();
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
					String attrName = insertNode.getAttributes().item(0).getNodeName();
					System.out.println("attrName :" + attrName);
					attributeEle.setAttribute("Name", attrName);
					attributeEle.appendChild(oldAttrEle);
					attributeEle.appendChild(newAttrEle);
					enhancedCompareEle.appendChild(attributeEle);
					importedInsertNode.appendChild(enhancedCompareEle);
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

		return doc;
	}

	// Checking if the Attribute is Sub XML
	public boolean isSubXML(String name, String value) {

		return name.endsWith("XML");
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
					expression = "//Delete";
				} else {
					expression = "//Delete[@" + name + "=\'" + attr.getNodeValue() + "\']";
				}
				System.out.println("\nNode expression :" + expression);
				deleteNodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			}
		}
		return deleteNodeList;
	}

}
