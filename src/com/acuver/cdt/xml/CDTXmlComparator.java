package com.acuver.cdt.xml;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
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

	Document outputDoc = null;
	Document inputDoc = null;

	TreeMap<Integer, String> uniqueInsertMap = new TreeMap<Integer, String>();
	TreeMap<Integer, String> uniqueDeleteMap = new TreeMap<Integer, String>();

	TreeMap<Integer, String> duplicateInsertMap = new TreeMap<Integer, String>();
	TreeMap<Integer, String> duplicateDeleteMap = new TreeMap<Integer, String>();

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

		/* Create DOM Parser */
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder(); // DOM Parser

		// Create a new Output document
		outputDoc = parser.newDocument();
		Element parentElement = outputDoc.createElement(parentNodeName);
		outputDoc.appendChild(parentElement);

		// Processing Insert/Delete Tags
		processInsertDeleteTags(doc);

		// Adding Unique Elements To Output
		addUniqueElementsToOutput();

		return outputDoc;
	}

	// Processing Insert/Delete Tags
	public void processInsertDeleteTags(Document doc) throws Exception {

		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = "//Insert";
		NodeList nodeList = null;
		nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
		System.out.println("Insert nodeList Length()" + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Node node = nodeList.item(itr);
			System.out.println("\nNode Name :" + node.getNodeName());
			NamedNodeMap attrList = node.getAttributes();

			for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
				Node attr = attrList.item(attrItr);
				String name = attr.getNodeName();

				if (name.endsWith("Name")) {
					if (attr.getNodeValue().trim().isEmpty())
						expression = "//Delete";

					else
						expression = "//Delete[@" + name + "=\'" + attr.getNodeValue() + "\']";

					System.out.println("\nNode expression :" + expression);
					NodeList deleteNodeList = null;
					deleteNodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

					System.out.println("deleteNodeList length " + deleteNodeList.getLength());
					for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
						Node node2 = deleteNodeList.item(itr2);

						System.out.println("comparing");

						Diff diff = DiffBuilder.compare(node).withTest(node2).checkForSimilar().ignoreComments()
								.ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
								.withDifferenceEvaluator(new CDTXmlDifferenceEvaluator()).build();

						if (diff.hasDifferences()) {
							Iterator<Difference> iter = diff.getDifferences().iterator();
							while (iter.hasNext()) {
								System.out.println(iter.next().toString());
							}
							String nodeName1 = node.getNodeName();
							String nodeName2 = node2.getNodeName();
							int nodeIndex1 = itr;
							int nodeIndex2 = itr2;
							uniqueInsertMap.put(nodeIndex1, nodeName1);
							uniqueDeleteMap.put(nodeIndex2, nodeName2);

						} else {
							System.out.println("primary key issue");
							String nodeName1 = node.getNodeName();
							String nodeName2 = node2.getNodeName();
							int nodeIndex1 = itr;
							int nodeIndex2 = itr2;
							duplicateInsertMap.put(nodeIndex1, nodeName1);
							duplicateDeleteMap.put(nodeIndex2, nodeName2);
						}
					}
				}
			}
		}

	}

	// Adding Unique Elements To Output Document
	public void addUniqueElementsToOutput() {

		System.out.println("uniqueInsertMap : " + uniqueInsertMap.keySet());
		System.out.println("uniqueDeleteMap : " + uniqueDeleteMap.keySet());
		System.out.println("duplicateInsertMap : " + duplicateInsertMap.keySet());
		System.out.println("duplicateDeleteMap : " + duplicateDeleteMap.keySet());

		if (uniqueInsertMap.size() > 0) {
			for (Entry<Integer, String> m : uniqueInsertMap.entrySet()) {
				Node element1 = inputDoc.getElementsByTagName(m.getValue()).item(m.getKey());
				Node importedChild1 = outputDoc.importNode(element1, true);
				if (duplicateInsertMap.size() == 0) {
					outputDoc.getDocumentElement().appendChild(importedChild1);
				} else {
					if (!duplicateInsertMap.containsKey(m.getKey())) {
						outputDoc.getDocumentElement().appendChild(importedChild1);
					}
				}
			}
		}
		if (uniqueDeleteMap.size() > 0) {
			for (Entry<Integer, String> m : uniqueDeleteMap.entrySet()) {
				Node element2 = inputDoc.getElementsByTagName(m.getValue()).item(m.getKey());
				Node importedChild2 = outputDoc.importNode(element2, true);
				if (duplicateDeleteMap.size() == 0) {
					outputDoc.getDocumentElement().appendChild(importedChild2);
				} else {
					if (!duplicateDeleteMap.containsKey(m.getKey())) {
						outputDoc.getDocumentElement().appendChild(importedChild2);
					}
				}
			}
		}
		outputDoc.normalizeDocument();
	}

}
