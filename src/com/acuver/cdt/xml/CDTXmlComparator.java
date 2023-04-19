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
	Document updateDoc = null;

	TreeMap<Integer, String> uniqueInsertMap = new TreeMap<Integer, String>();
	TreeMap<Integer, String> uniqueDeleteMap = new TreeMap<Integer, String>();

	TreeMap<Integer, String> duplicateInsertMap = new TreeMap<Integer, String>();
	TreeMap<Integer, String> duplicateDeleteMap = new TreeMap<Integer, String>();

	ArrayList<String> subXmlDiffData = new ArrayList<String>();
	TreeMap<Integer, ArrayList<String>> uniqueDiffDataMap = new TreeMap<Integer, ArrayList<String>>();

	ArrayList<String> UniqueDiffData = new ArrayList<String>();
	TreeMap<Integer, ArrayList<String>> uniqueUpdateMap = new TreeMap<Integer, ArrayList<String>>();

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

		// Create a New Update doc
		updateDoc = parser.newDocument();
		Element parentElement1 = updateDoc.createElement(parentNodeName);
		updateDoc.appendChild(parentElement1);

		// Adding update elements to update doc
		addUniqueElementsToUpdate();

		return updateDoc;
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

						System.out.println("comparing : ");

						Diff diff = DiffBuilder.compare(node).withTest(node2).checkForSimilar().ignoreComments()
								.ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
								.withDifferenceEvaluator(new CDTXmlDifferenceEvaluator()).build();

						if (diff != null && diff.hasDifferences()) {
							Iterator<Difference> iter = diff.getDifferences().iterator();

							while (iter.hasNext()) {
								String datadifference = iter.next().toString();
								if (datadifference != null && !datadifference.contains("xml version")) {
									UniqueDiffData.add(datadifference);
								}
							}
							String nodeName1 = node.getNodeName();
							String nodeName2 = node2.getNodeName();
							int nodeIndex1 = itr;
							int nodeIndex2 = itr2;
							uniqueInsertMap.put(nodeIndex1, nodeName1);
							uniqueDeleteMap.put(nodeIndex2, nodeName2);
							uniqueUpdateMap.put(nodeIndex1, UniqueDiffData);

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
				if (name.endsWith("XML")) {
					System.out.println("name ends With XML : " + name);
					expression = "//Delete";
					System.out.println("\nNode expression In Sub XML : " + expression);
					NodeList deleteNodeList = null;
					deleteNodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

					System.out.println("deleteNodeList length In Sub XML : " + deleteNodeList.getLength());
					for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
						Node node2 = deleteNodeList.item(itr2);

						System.out.println("comparing Sub XML : ");

						// New Code
						Diff diff = null;
						NamedNodeMap deleteAttrList = node2.getAttributes();
						for (int delAttrItr = 0; delAttrItr < deleteAttrList.getLength(); delAttrItr++) {
							Node delAttr = deleteAttrList.item(delAttrItr);
							String delAtrrName = delAttr.getNodeName();
							if (delAtrrName.equalsIgnoreCase(name)) {
								System.out.println("Inside ConfigXML");
								diff = DiffBuilder.compare(attr.getNodeValue()).withTest(delAttr.getNodeValue())
										.checkForSimilar().ignoreComments().ignoreWhitespace()
										.ignoreElementContentWhitespace().normalizeWhitespace()
										.withDifferenceEvaluator(new CDTXmlDifferenceEvaluator()).build();
							}
						}
						if (diff != null && diff.hasDifferences()) {
							Iterator<Difference> iter = diff.getDifferences().iterator();
							while (iter.hasNext()) {
								System.out.println("*****Difference Details in Config XML*********************");
								String subXmldiffData = iter.next().toString();
								if (subXmldiffData != null) {
									subXmlDiffData.add(subXmldiffData);
								}
							}
							int nodeIndex1 = itr;
							uniqueDiffDataMap.put(nodeIndex1, subXmlDiffData);
						} else {
							System.out.println("No Difference in Sub XML : ");
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
		System.out.println("uniqueDiffDataMap : " + uniqueDiffDataMap.keySet());
		System.out.println("uniqueUpdateMap : " + uniqueUpdateMap.keySet());

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
				if (uniqueUpdateMap.size() > 0) {
					for (Entry<Integer, ArrayList<String>> n : uniqueUpdateMap.entrySet()) {
						if (m.getKey() == (n.getKey())) {

							Element oldAttrEle = outputDoc.createElement("OldValues");

							ArrayList<String> valuesList = n.getValue();
							for (int i = 0; i < valuesList.size(); i++) {
								String diffValues = valuesList.get(i);
								System.out.println("diffValues in uniqueUpdateMap : " + diffValues);
								int beginIndex = diffValues.indexOf("@") + 1;
								int endIndex = diffValues.indexOf("to");
								String attrName = diffValues.substring(beginIndex, endIndex).trim();
								System.out.println("UpdateAttrName :" + attrName);
								int oldBeginIndex = diffValues.indexOf("but was '") + 9;
								int oldEndIndex = diffValues.indexOf("- comparing") - 2;
								String oldAttrValue = diffValues.substring(oldBeginIndex, oldEndIndex).trim();
								oldAttrEle.setAttribute(attrName, oldAttrValue);
							}
							importedChild1.appendChild(oldAttrEle);
						}
					}
				}
				if (uniqueDiffDataMap.size() > 0) {
					for (Entry<Integer, ArrayList<String>> n : uniqueDiffDataMap.entrySet()) {
						if (m.getKey() == (n.getKey())) {
							Element enhancedCompareEle = outputDoc.createElement("EnhancedCompare");
							Element attributeEle = outputDoc.createElement("Attribute");
							Element oldAttrEle = outputDoc.createElement("Old");
							Element newAttrEle = outputDoc.createElement("New");
							ArrayList<String> valuesList = n.getValue();
							for (int i = 0; i < valuesList.size(); i++) {
								String diffValues = valuesList.get(i);
								System.out.println("diffValues in uniqueDiffDataMap : " + diffValues);
								int beginIndex = diffValues.indexOf("@") + 1;
								int endIndex = diffValues.indexOf("to");
								String attrName = diffValues.substring(beginIndex, endIndex).trim();
								System.out.println("AttrName in uniqueDiffDataMap :" + attrName);
								int oldBeginIndex = diffValues.indexOf("value '") + 7;
								int oldEndIndex = diffValues.indexOf("' but");
								String oldAttrValue = diffValues.substring(oldBeginIndex, oldEndIndex).trim();
								System.out.println("oldAttrValue in uniqueDiffDataMap :" + oldAttrValue);
								int newBeginIndex = diffValues.indexOf("but was '") + 9;
								int newEndIndex = diffValues.indexOf("- comparing") - 2;
								String newAttrValue = diffValues.substring(newBeginIndex, newEndIndex).trim();
								System.out.println("newAttrValue in uniqueDiffDataMap :" + newAttrValue);
								oldAttrEle.setAttribute(attrName, oldAttrValue);
								newAttrEle.setAttribute(attrName, newAttrValue);
							}
							String attrName = element1.getAttributes().item(0).getNodeName();
							System.out.println("attrName :" + attrName);
							attributeEle.setAttribute("Name", attrName);
							attributeEle.appendChild(oldAttrEle);
							attributeEle.appendChild(newAttrEle);
							enhancedCompareEle.appendChild(attributeEle);
							importedChild1.appendChild(enhancedCompareEle);
						}
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

	// Adding Unique Elements To Update Document
	public void addUniqueElementsToUpdate() {

		NodeList childNodes = outputDoc.getDocumentElement().getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equals("Delete")) {
				outputDoc.getDocumentElement().removeChild(childNode);
			} else if (childNode.getNodeName().equals("Insert")) {

				// Create a new "Update" element with the same attributes as the "Insert"
				// element
				Element updateElement = updateDoc.createElement("Update");

				NamedNodeMap attributes = childNode.getAttributes();

				for (int j = 0; j < attributes.getLength(); j++) {
					Node attribute = attributes.item(j);
					updateElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
				}
				Element oldValues = (Element) ((Element) childNode).getElementsByTagName("OldValues").item(0);

				// Create a new "OldValues" element in the "Update" element with the same
				// attributes
				if (oldValues != null) {
					Element oldValuesElement = updateDoc.createElement("OldValues");
					NamedNodeMap oldValuesAttributes = oldValues.getAttributes();
					for (int j = 0; j < oldValuesAttributes.getLength(); j++) {
						Node oldValuesAttribute = oldValuesAttributes.item(j);
						oldValuesElement.setAttribute(oldValuesAttribute.getNodeName(),
								oldValuesAttribute.getNodeValue());
					}
					updateElement.appendChild(oldValuesElement);
				}

				Element EnhancedCompare = (Element) ((Element) childNode).getElementsByTagName("EnhancedCompare")
						.item(0);
				if (EnhancedCompare != null) {
					Node importedEnhancedCompare = updateDoc.importNode(EnhancedCompare, true);
					updateElement.appendChild(importedEnhancedCompare);
				}
				updateDoc.getDocumentElement().appendChild(updateElement);
			}

		}
	}

}
