package com.acuver.cdt.xml;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;

public class CDTXmlComparator {

	Document inputDoc = null;
	Document outputDoc = null;
	Document processedInsertDeleteDoc = null;
	Document updateDoc = null;
	Document processedUpdateDoc = null;
	Document processedUpdateEnhancedCompareDoc = null;
	Document processedManualReviewDoc = null;

	String tablePrefix;
	String parentNodeName;

	CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();

	TreeMap<Integer, String> uniqueInsertMap = new TreeMap<Integer, String>();
	TreeMap<Integer, String> duplicateInsertMap = new TreeMap<Integer, String>();

	TreeMap<Integer, Node> uniqueDeleteMap = new TreeMap<Integer, Node>();
	TreeMap<Integer, Node> duplicateDeleteMap = new TreeMap<Integer, Node>();

	TreeMap<Integer, ArrayList<String>> subXmlDiffDataMap = new TreeMap<Integer, ArrayList<String>>();
	TreeMap<Integer, Node> duplicateSubXmlDiffDataMap = new TreeMap<Integer, Node>();

	ArrayList<String> mergeInsertDataList = new ArrayList<String>();
	TreeMap<Integer, ArrayList<String>> mergeInsertDataMap = new TreeMap<Integer, ArrayList<String>>();

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
		parentNodeName = root.getNodeName();
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

		// Create a new Processed document for Enhanced Compare with Root Element
		processedUpdateEnhancedCompareDoc = parser.newDocument();
		Element parentElementEnhancedCompare = processedUpdateEnhancedCompareDoc.createElement(parentNodeName);
		processedUpdateEnhancedCompareDoc.appendChild(parentElementEnhancedCompare);

		// Processing Update Doc with EnhancedCompare
		processedUpdateEnhancedCompareDoc = addEnhancedCompareToUpdates(processedUpdateDoc);

		moveUpdatesToManualReview(processedUpdateEnhancedCompareDoc);

		outputDoc = processedManualReviewDoc;

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
		if (nodeList == null || nodeList.getLength() == 0) {
			System.out.println("No Insert Nodes in Input Doc : ");
			processedInsertDeleteDoc = doc;
			return processedInsertDeleteDoc;
		}

		System.out.println("Insert nodeList Length()" + nodeList.getLength());
		for (int itr = 0; itr < nodeList.getLength(); itr++) {
			Element insertElement = (Element) nodeList.item(itr);

			// Getting Deletes from Insert.
			NodeList deleteNodeList = getDeletesForInsert(doc, insertElement);

			if (deleteNodeList == null || deleteNodeList.getLength() == 0) {
				System.out.println("No Delete Nodes in Input Doc : ");
				processedInsertDeleteDoc = doc;
				return processedInsertDeleteDoc;
			}

			System.out.println("deleteNodeList length " + deleteNodeList.getLength());
			for (int itr2 = 0; itr2 < deleteNodeList.getLength(); itr2++) {
				Element deleteElement = (Element) deleteNodeList.item(itr2);
				int insertNodeIndex = itr;
				int deleteNodeIndex = itr2;
				String insertNodeName = insertElement.getNodeName();
				System.out.println("Comparing insertNode with deleteNode : ");

				Diff diff = DiffBuilder.compare(insertElement).withTest(deleteElement).checkForSimilar().ignoreComments()
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
					uniqueDeleteMap.put(deleteNodeIndex, deleteElement);
				} else {
					duplicateInsertMap.put(insertNodeIndex, insertNodeName);
					duplicateDeleteMap.put(deleteNodeIndex, deleteElement);
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

	            System.out.println("Comparing insertNode with deleteNode : ");

	            Diff diff = DiffBuilder.compare(insertElement).withTest(deleteElement)
	                .checkForSimilar().ignoreComments().ignoreWhitespace()
	                .ignoreElementContentWhitespace().normalizeWhitespace()
	                .withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

	            if (diff.hasDifferences()) {
	                NodeList oldValuesList = insertElement.getChildNodes();
	                Element oldValuesElement = null;

	                // Look for existing OldValues element and update it
	                for (int i = 0; i < oldValuesList.getLength(); i++) {
	                    Node oldValuesNode = oldValuesList.item(i);
	                    if (oldValuesNode.getNodeName().equals("OldValues")) {
	                        oldValuesElement = (Element) oldValuesNode;
	                        break;
	                    }
	                }

	                // Create new OldValues element if it doesn't exist
	                if (oldValuesElement == null) {
	                    oldValuesElement = doc.createElement("OldValues");
	                    insertElement.appendChild(oldValuesElement);
	                }

	                // Store the difference values in the OldValues element
	                Iterator<Difference> iter = diff.getDifferences().iterator();
	                while (iter.hasNext()) {
	                    String difference = iter.next().toString();
	                    System.out.println(difference);

	                    if (difference != null && !difference.contains("xml version")) {
	                        int attrNameStartIndex = difference.indexOf("@") + 1;
	                        int attrNameEndIndex = difference.indexOf("to");
	                        String attrName = difference.substring(attrNameStartIndex, attrNameEndIndex).trim();
	                        System.out.println("UpdateAttrName :" + attrName);

	                        int oldAttrValueStartIndex = difference.indexOf("but was '") + 9;
	                        int oldAttrValueEndIndex = difference.indexOf("- comparing") - 2;
	                        String oldAttrValue = difference.substring(oldAttrValueStartIndex, oldAttrValueEndIndex).trim();

	                        oldValuesElement.setAttribute(attrName, oldAttrValue);
	                    }
	                }

	            } else {
	            	doc.getDocumentElement().removeChild(insertElement);
	            	doc.getDocumentElement().removeChild(deleteElement);
	                System.out.println("No Difference In insertNode and deleteNode. Both Insert and Delete Nodes are removed.");
	            }
	        }
	    }

	    // Replace Insert elements with Update elements
	    NodeList childNodes = doc.getDocumentElement().getChildNodes();
	    for (int i = 0; i < childNodes.getLength(); i++) {
	        Node childNode = childNodes.item(i);

	        if (childNode.getNodeName().equals("Delete")) {
	            doc.getDocumentElement().removeChild(childNode);

	        } else if (childNode.getNodeName().equals("Insert")) {
	        	 doc.getDocumentElement().removeChild(childNode);

	            Element updateElement = doc.createElement("Update");

	            
	            NamedNodeMap attributes = childNode.getAttributes();
	            
	            					for (int j = 0; j < attributes.getLength(); j++) {
	            						Node attribute = attributes.item(j);
	            						updateElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
	            					}
	            					Element oldValues = (Element) ((Element) childNode).getElementsByTagName("OldValues").item(0);
	            
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
	            					doc.getDocumentElement().appendChild(updateElement);
	            				}
	       
	    }
	    updateDoc=doc;
	    
	    return updateDoc;
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
									System.out.println(subXmlDiffData);
									if (!(subXmlDiffData.contains("Expected attribute name"))) {
										subXmlDiffDataList.add(subXmlDiffData);
									}
								}
							} else {

								if (updateNode.getParentNode() != null) {
									updateNode.getParentNode().removeChild(updateNode);
									System.out.println("No Difference in Sub XML : ");

								}
							}
						}
					}
				}
			}

		}
		processedUpdateDoc=doc;
		return processedUpdateDoc;

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
	public boolean isSubXML(String name, String value) {

		return name.endsWith("XML") || name.endsWith("Xml");
	}

	// Get Delete NodeList for Insert Node
	public NodeList getDeletesForInsert(Document doc, Element insertElement) throws XPathExpressionException {
		System.out.println("\nNode Name :" + insertElement.getNodeName());
		NamedNodeMap attrList = insertElement.getAttributes();
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
