package com.acuver.cdt.xml;

import java.io.File;
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

public class CDTXmlComparator {

	Document inputDoc = null;
	Document outputDoc = null;
	Document processedInsertDeleteDoc = null;
	Document updateDoc = null;
	Document processedUpdateDoc = null;
	Document processedUpdateEnhancedCompareDoc = null;

	String primaryKeyName;
	XPath xPath = XPathFactory.newInstance().newXPath();

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

		// Create a new Output document with Root Element
		outputDoc = parser.newDocument();
		Element parentElement = outputDoc.createElement(parentNodeName);
		outputDoc.appendChild(parentElement);

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

	// get Table Primary Key Name
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

		return doc;
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
