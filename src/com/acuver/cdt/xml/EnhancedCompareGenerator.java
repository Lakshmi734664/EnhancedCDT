package com.acuver.cdt.xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;

public class EnhancedCompareGenerator {

	Document processedUpdateEnhancedCompareDoc;

	String rootName = null;

	public void createEnhancedCompare(String updateAttrName, Element updateElement) throws Exception {

		Element oldValuesElement = (Element) updateElement.getElementsByTagName(CDTConstants.OLDVALUES).item(0);

		String subXmlUpdateAttrValue = updateElement.getAttribute(updateAttrName);
		String subXmlOldAttrValue = oldValuesElement.getAttribute(updateAttrName);

		Document oldAttrValueDoc = EnhancedCDTMain.factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(subXmlOldAttrValue)));

		Document newAttrValueDoc = EnhancedCDTMain.factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(subXmlUpdateAttrValue)));

		Element oldAttrValueEle = removeWhiteSpaceNodes(oldAttrValueDoc.getDocumentElement());
		Element newAttrValueEle = removeWhiteSpaceNodes(newAttrValueDoc.getDocumentElement());

		rootName = oldAttrValueEle.getNodeName();

		removeIfEqual(oldAttrValueEle, newAttrValueEle);

		// cloned the subxml as it will be modified to keep only differences , original
		// subxml will be used for comparison
		Element oldDiffEle = (Element) oldAttrValueEle.cloneNode(true);

		diffNode(oldDiffEle, newAttrValueEle);

		// vice-versa
		Element newDiffEle = (Element) newAttrValueEle.cloneNode(true);

		diffNode(newDiffEle, oldAttrValueEle);

		// import update element and enhanced compare element in
		// processedUpdateEnhancedCompareDoc.
		// Skip this, if both oldDiffEle/newDiffEle has root ele only

		if (oldDiffEle.hasChildNodes() || newDiffEle.hasChildNodes()) {
			Node importedUpdateNode = processedUpdateEnhancedCompareDoc.importNode(updateElement, true);
			processedUpdateEnhancedCompareDoc.getDocumentElement().appendChild(importedUpdateNode);

			Element enhancedCompareEle = processedUpdateEnhancedCompareDoc.createElement("EnhancedCompare");
			Element attributeEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.attribute);
			Element oldAttrEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.old);
			Element newAttrEle = processedUpdateEnhancedCompareDoc.createElement(CDTConstants.New);

			attributeEle.setAttribute("Name", updateAttrName);

			if (oldDiffEle.hasChildNodes()) {
				Node importedOldDiffEle = processedUpdateEnhancedCompareDoc.importNode(oldDiffEle, true);
				oldAttrEle.appendChild(importedOldDiffEle);
			}

			if (newDiffEle.hasChildNodes()) {
				Node importedNewDiffEle = processedUpdateEnhancedCompareDoc.importNode(newDiffEle, true);
				newAttrEle.appendChild(importedNewDiffEle);
			}

			attributeEle.appendChild(oldAttrEle);
			attributeEle.appendChild(newAttrEle);
			enhancedCompareEle.appendChild(attributeEle);
			importedUpdateNode.appendChild(enhancedCompareEle);
		}

	}

	private void diffNode(Element oldAttrValueEle, Element newAttrValueEle) throws Exception {
		String nodeName = oldAttrValueEle.getNodeName();
		if (nodeName.equals("Points")) {
			if (oldAttrValueEle.getParentNode() != null)
				oldAttrValueEle.getParentNode().removeChild(oldAttrValueEle);
			return;
		} else if (nodeName.equals("Template")) {

			Diff diff = DiffBuilder.compare(oldAttrValueEle)
					.withTest(CDTHelper.getChildElement(newAttrValueEle, nodeName)).checkForSimilar().ignoreComments()
					.ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace().build();
			if (!(diff.hasDifferences())) {
				oldAttrValueEle.getParentNode().removeChild(oldAttrValueEle);
				return;
			}

		}
		Element matchingSubEle = null;
		if (oldAttrValueEle.getNodeName().equalsIgnoreCase(rootName)) {
			matchingSubEle = newAttrValueEle;
		} else
			matchingSubEle = diffAttributes(oldAttrValueEle, newAttrValueEle);

		if (matchingSubEle != null) {

			// if matching node found , remove equal attributes
			removeAttrIfEqual(oldAttrValueEle, matchingSubEle);

			// Get list of all child nodes for oldAttr and iterate through it.
			NodeList subChildElementNodeList = oldAttrValueEle.getChildNodes();

			for (int m = subChildElementNodeList.getLength() - 1; m >= 0; m--) {
				// for (int m = 0; m < subChildElementNodeList.getLength() ; m++) {
				Node subChildNode = subChildElementNodeList.item(m);
				if (subChildNode.getNodeType() == Node.ELEMENT_NODE) {
					Element subChildElement = (Element) subChildNode;
					// for each subChildElement call diffNode , the first arg is subChildElement and
					// second is newAttrValueEle
					diffNode(subChildElement, matchingSubEle);
				}
			}
			// }

			removeWhiteSpaceNodes(oldAttrValueEle);

			if (!(oldAttrValueEle.hasAttributes()) && !(oldAttrValueEle.hasChildNodes())) {
				if (oldAttrValueEle.getParentNode() != null) {
					oldAttrValueEle.getParentNode().removeChild(oldAttrValueEle);
				}
			}
		}
	}

	private void removeAttrIfEqual(Element oldAttrValueEle, Element newAttrValueEle) {
		NamedNodeMap oldAttributes = oldAttrValueEle.getAttributes();
		NamedNodeMap newAttributes = newAttrValueEle.getAttributes();
		List<Attr> oldChildAttrRmvList = new ArrayList<Attr>();

		// Iterate over the attributes of the old element
		for (int i = 0; i < oldAttributes.getLength(); i++) {
			Node oldAttrNode = oldAttributes.item(i);

			Attr oldAttr = (Attr) oldAttrNode;
			String oldAttrName = oldAttr.getName();
			String oldAttrValue = oldAttr.getValue().trim();

			// Check if the new element has the same attribute
			if (newAttributes.getNamedItem(oldAttrName) != null) {
				Attr newAttr = (Attr) newAttributes.getNamedItem(oldAttrName);
				String newAttrValue = newAttr.getValue().trim();

				// Compare attribute values
				if (oldAttrValue.equals(newAttrValue)) {

					// Add the Attribute which needs to be removed in oldChildAttrRmvList
					oldChildAttrRmvList.add(oldAttr);
				} else if (oldAttrValue.matches("\\d+") && oldAttrValue.length() > 5) {
					// Add the Attribute which needs to be removed in oldChildAttrRmvList

					oldChildAttrRmvList.add(oldAttr);
				}

			}
		}

		if (!oldChildAttrRmvList.isEmpty()) {
			for (int i = 0; i < oldChildAttrRmvList.size(); i++) {
				Attr removeAttr = oldChildAttrRmvList.get(i);
				if (oldAttrValueEle.hasAttribute(removeAttr.getName())) {
					oldAttrValueEle.removeAttributeNode(removeAttr);
				}
			}
		}
	}

	private int getAttributeDiffCount(Element oldAttrValueEle, Element newAttrValueEle) {

		int count = 0;

		NamedNodeMap oldAttributes = oldAttrValueEle.getAttributes();
		NamedNodeMap newAttributes = newAttrValueEle.getAttributes();

		// Iterate over the attributes of the old element
		for (int i = 0; i < oldAttributes.getLength(); i++) {
			Node oldAttr = oldAttributes.item(i);
			String oldAttrName = oldAttr.getNodeName();
			String oldAttrValue = oldAttr.getNodeValue();

			// Check if the new element has the same attribute
			if (newAttributes.getNamedItem(oldAttrName) != null) {
				Node newAttr = newAttributes.getNamedItem(oldAttrName);
				String newAttrValue = newAttr.getNodeValue();

				// Compare attribute values
				if (!oldAttrValue.equals(newAttrValue)) {
					if (oldAttrValue.matches("\\d+") && oldAttrValue.length() > 5 && newAttrValue.matches("\\d+")
							&& newAttrValue.length() > 5) {
					} else
						count++;
				}
			} else {
				// If the new element doesn't have the attribute, consider it as different
				count++;
			}
		}

		// Iterate over the attributes of the new element

		for (int i = 0; i < newAttributes.getLength(); i++) {
			Node newAttr = newAttributes.item(i);
			String newAttrName = newAttr.getNodeName();

			// Check if the old element has the same attribute
			if (oldAttributes.getNamedItem(newAttrName) == null) {
				// If the old element doesn't have the attribute, consider it as different
				count++;
			}
		}
		return count;

	}

	private Element diffAttributes(Element expectedElement, Element actualElement) throws Exception {

		/*
		 * 1. Get the count of different attributes (add/del/modify) 2. Use the element
		 * with least count 3. Pitfall: No attributes/all count are same , consider all
		 * nodes as matching and continue 4. If attribute name is number and length > 5
		 * , ignore
		 */
		Element matchingElement = null;
		NodeList actualElementNodes = null;
		String nodeName = expectedElement.getNodeName();
		String xpath = null;
		boolean customAPI = false;
		if (nodeName.equals("Node")) {
			Element propEle = CDTHelper.getChildElement(expectedElement, "Properties");
			if (propEle != null) {

				String nodeType = expectedElement.getAttribute("NodeType");
				switch (nodeType) {
				case "API":
					if ("Custom".equals(propEle.getAttribute("APIType"))) {
						String className = propEle.getAttribute("ClassName");
						String methodName = propEle.getAttribute("MethodName");
						xpath = "//Node/Properties[@ClassName='" + className + "' and @MethodName='" + methodName
								+ "']";
						customAPI = true;
					} else {
						String apiName = propEle.getAttribute("APIName");
						xpath = "//Node[Properties/@APIName='" + apiName + "']";
					}
					break;
				case "Condition":
					String name = propEle.getAttribute("ConditionKey_ConditionName");
					if (!name.isEmpty()) {
						xpath = "//Node[Properties/@ConditionKey_ConditionName='" + name + "']";
					}
					break;
				case "CompositeFlow":
					NodeList flowList = expectedElement.getElementsByTagName("Flow");
					// Node[Properties/FlowList/Flow/@FlowName='SGTPostEmailReminderDB' or
					// @FlowName='SGTPostEmailReminderMsgToHybrisQ']
					if (flowList.getLength() > 0) {
						StringBuffer xpathStr = new StringBuffer("//Node[Properties/FlowList/Flow/");
						for (int itr = 0; itr < flowList.getLength(); itr++) {
							Element flowEle = (Element) flowList.item(itr);
							if (itr > 0)
								xpathStr.append(" or ");
							xpathStr.append("@FlowName='" + flowEle.getAttribute("FlowName") + "'");

						}
						xpathStr.append("]");
						xpath = xpathStr.toString();
					}
					break;
				case "TextTranslator":
					String schemaName = propEle.getAttribute("SchemaName");
					xpath = "//Node[Properties/@SchemaName='" + schemaName + "']";
					break;
				case "XSL_Translator":
					String XSLName = propEle.getAttribute("XSLName");
					xpath = "//Node[Properties/@XSLName='" + XSLName + "']";
					break;
				case "DTE":
					String XmlName = propEle.getAttribute("XmlName");
					xpath = "//Node[Properties/@XmlName='" + XmlName + "']";
					break;
				case "Router":
					String DocumentId = propEle.getAttribute("DocumentId");
					xpath = "//Node[Properties/@DocumentId='" + DocumentId + "']";
					break;
				case "Alert":
					String ExceptionType = propEle.getAttribute("ExceptionType");
					xpath = "//Node[Properties/@ExceptionType='" + ExceptionType + "']";
					break;
				case "SendMail":
					String Subject = propEle.getAttribute("Subject");
					xpath = "//Node[Properties/@Subject='" + Subject + "']";
					break;
				case "Defaulting":
					NodeList OverrideList = expectedElement.getElementsByTagName("Override");
					if (OverrideList.getLength() > 0) {
						Element overrideEle = (Element) OverrideList.item(0);
						String attr = overrideEle.getAttribute("Attribute");

						xpath = "//Node[Properties/CustomOverrides/Override/@Attribute='" + attr + "']";

					}
					break;

				case "JasperPrint":
					String JasperReportName = propEle.getAttribute("JasperReportName");
					xpath = "//Node[Properties/@JasperReportName='" + JasperReportName + "']";
					break;
				case "Start":
				case "End":
				case "DataSecurity":
					xpath = "//Node[@NodeType='" + nodeType + "']";

				}

			}
		} else if (nodeName.equals("Link")) {
			Element propEle = CDTHelper.getChildElement(expectedElement, "Properties");
			if (propEle != null) {
				String transportType = expectedElement.getAttribute("TransportType");
				switch (transportType) {
				case "DB":
					String TableName = propEle.getAttribute("TableName");
					xpath = "//Link[Properties/@TableName='" + TableName + "']";
					break;
				case "JMS":
				case "GJMS":
				case "TOPICJMS":
					String QName = propEle.getAttribute("QName");
					xpath = "//Link[Properties/@QName='" + QName + "']";
					break;
				case "SGJMS":
					String name = propEle.getAttribute("QName");
					xpath = "//Link[Properties/RequestProperties/@QName='" + name + "']";
					break;
				case "WEBSERVICE":
					String MethodName = propEle.getAttribute("MethodName");
					xpath = "//Link[Properties/@MethodName='" + MethodName + "']";

				}
			}
		} else if (nodeName.equals("Flow")) {
			xpath = "//Flow[@FlowName='" + expectedElement.getAttribute("FlowName") + "']";
		}

		if (xpath != null) {
			actualElementNodes = (NodeList) EnhancedCDTMain.xPath.compile(xpath).evaluate(actualElement,
					XPathConstants.NODESET);
		} else
			actualElementNodes = actualElement.getElementsByTagName(nodeName);

		int minDiffCount = Integer.MAX_VALUE;

		ArrayList<Integer> countList = new ArrayList<Integer>();

		// Iterate thru each element of actualElement with same nodeName
		for (int itr = 0; itr < actualElementNodes.getLength(); itr++) {
			Node actualElementNode = actualElementNodes.item(itr);

			Element actualEle = (Element) actualElementNode;
			if (customAPI)
				actualEle = (Element) actualElementNode.getParentNode();
			int count = getAttributeDiffCount(expectedElement, actualEle);

			countList.add(count);
			// Update the matching element if it has the least count
			if (count < minDiffCount) {
				minDiffCount = count;
				matchingElement = actualEle;

			}
		}

		if (countList.size() > 1) {
			int min = Collections.min(countList);
			int max = Collections.max(countList);

			if (min == max && expectedElement.getAttribute("NodeType").equals("API")) {
				Element expTemplateEle = (Element) expectedElement.getElementsByTagName("Template").item(0);
				if (expTemplateEle != null) {
					for (int itr = 0; itr < actualElementNodes.getLength(); itr++) {
						Node actualElementNode = actualElementNodes.item(itr);
						Element actualEle = (Element) actualElementNode;
						Element actualTemplateEle = (Element) actualEle.getElementsByTagName("Template").item(0);

						try {
							Diff diff = DiffBuilder.compare(expTemplateEle).withTest(actualTemplateEle)
									.checkForSimilar().ignoreComments().ignoreWhitespace()
									.ignoreElementContentWhitespace().normalizeWhitespace().build();
							if (!(diff.hasDifferences())) {
								if (customAPI)
									actualEle = (Element) actualElementNode.getParentNode();
								return actualEle;
							}
						} catch (Exception e) {
							//throw e;
						}

					}
				}
			}
		}
		return matchingElement;
	}

	private Element removeWhiteSpaceNodes(Element oldAttrValueEle) {
		List<Node> oldChildEmptyNodesRmvList = new ArrayList<Node>();
		NodeList oldChildNodes = oldAttrValueEle.getChildNodes();
		for (int itr = 0; itr < oldChildNodes.getLength(); itr++) {
			Node oldChildNode = oldChildNodes.item(itr);
			// condition used to check white spaces
			if (!(oldChildNode.getNodeType() == Node.ELEMENT_NODE)) {
				oldChildEmptyNodesRmvList.add(oldChildNode);
			}
		}

		if (oldChildEmptyNodesRmvList != null && !oldChildEmptyNodesRmvList.isEmpty()) {
			for (int i = 0; i < oldChildEmptyNodesRmvList.size(); i++) {
				Node oldAttr = oldChildEmptyNodesRmvList.get(i);
				oldAttrValueEle.removeChild(oldAttr);
			}
		}

		NodeList childNodes = oldAttrValueEle.getElementsByTagName("Points");
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			childNode.getParentNode().removeChild(childNode);
		}

		return oldAttrValueEle;
	}

	private void removeIfEqual(Element oldAttrValueEle, Element newAttrValueEle) {

		NodeList oldChildNodes = oldAttrValueEle.getChildNodes();
		List<Node> oldChildNodesRmvList = new ArrayList<Node>();
		for (int itr = 0; itr < oldChildNodes.getLength(); itr++) {
			Node oldChildNode = oldChildNodes.item(itr);

			NodeList newChildNodes = newAttrValueEle.getElementsByTagName(oldChildNode.getNodeName());
			for (int itr2 = 0; itr2 < newChildNodes.getLength(); itr2++) {
				Node newChildEle = newChildNodes.item(itr2);
				if (newChildEle.isEqualNode(oldChildNode)) {
					// matching element - remove
					oldChildNodesRmvList.add(oldChildNode);
					newAttrValueEle.removeChild(newChildEle);
					break;
				}

			}
		}

		if (!oldChildNodesRmvList.isEmpty()) {
			for (int i = 0; i < oldChildNodesRmvList.size(); i++) {
				Node removeNode = oldChildNodesRmvList.get(i);
				removeNode.getParentNode().removeChild(removeNode);
			}
		}
	}

	public Document getProcessedUpdateEnhancedCompareDoc() {
		return processedUpdateEnhancedCompareDoc;
	}

	public void setProcessedUpdateEnhancedCompareDoc(Document processedUpdateEnhancedCompareDoc) {
		this.processedUpdateEnhancedCompareDoc = processedUpdateEnhancedCompareDoc;
	}

}