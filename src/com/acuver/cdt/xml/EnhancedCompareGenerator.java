package com.acuver.cdt.xml;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import com.acuver.cdt.EnhancedCDTMain;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class EnhancedCompareGenerator {

	Document processedUpdateEnhancedCompareDoc;

	public void createEnhancedCompare(String updateAttrName, Element updateElement) throws Exception {

		Element oldValuesElement = (Element) updateElement.getElementsByTagName("OldValues").item(0);

		String subXmlUpdateAttrValue = updateElement.getAttribute(updateAttrName);
		String subXmlOldAttrValue = oldValuesElement.getAttribute(updateAttrName);

		Document oldAttrValueDoc = EnhancedCDTMain.factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(subXmlOldAttrValue)));

		Document newAttrValueDoc = EnhancedCDTMain.factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(subXmlUpdateAttrValue)));

		removeWhiteSpaceNodes(oldAttrValueDoc.getDocumentElement());
		removeWhiteSpaceNodes(newAttrValueDoc.getDocumentElement());

		removeIfEqual(oldAttrValueDoc.getDocumentElement(), newAttrValueDoc.getDocumentElement());

		// cloned the subxml as it will be modified to keep only differences , original
		// subxml will be used for comparison
		Element oldDiffEle = (Element) oldAttrValueDoc.getDocumentElement().cloneNode(true);
		diffNode(oldDiffEle, newAttrValueDoc.getDocumentElement());

		// vice-versa
		Element newDiffEle = (Element) newAttrValueDoc.getDocumentElement().cloneNode(true);
		diffNode(newDiffEle, oldAttrValueDoc.getDocumentElement());

		// import update element and enhanced compare element in
		// processedUpdateEnhancedCompareDoc.
		// Skip this, if both oldDiffEle/newDiffEle has root ele only

		if (oldDiffEle.hasChildNodes() || newDiffEle.getChildNodes().getLength() > 0) {
			Node importedUpdateNode = processedUpdateEnhancedCompareDoc.importNode(updateElement, true);
			processedUpdateEnhancedCompareDoc.getDocumentElement().appendChild(importedUpdateNode);

			Element enhancedCompareEle = processedUpdateEnhancedCompareDoc.createElement("EnhancedCompare");
			Element attributeEle = processedUpdateEnhancedCompareDoc.createElement("Attribute");
			Element oldAttrEle = processedUpdateEnhancedCompareDoc.createElement("Old");
			Element newAttrEle = processedUpdateEnhancedCompareDoc.createElement("New");

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

		// Remove White Space Nodes
		removeWhiteSpaceNodes(oldAttrValueEle);

		NodeList oldAttrNodeList = oldAttrValueEle.getChildNodes();

		List<Node> oldChildEmptyNodesRmvList = new ArrayList<Node>();

		for (int i = 0; i < oldAttrNodeList.getLength(); i++) {
			Node oldAttr = oldAttrNodeList.item(i);

			Element oldAttrValueSubEle = (Element) oldAttr;

			NodeList matchingNodeList = diffAttributes(oldAttrValueSubEle, newAttrValueEle);

			if (matchingNodeList != null && matchingNodeList.getLength() > 0) {

				for (int j = 0; j < matchingNodeList.getLength(); j++) {
					Node matchingAttr = matchingNodeList.item(j);

					Element matchingSubEle = (Element) matchingAttr;

					// if matching node found , remove equal attributes
					oldChildEmptyNodesRmvList = removeAttrIfEqual(oldAttrValueSubEle, matchingSubEle,
							oldChildEmptyNodesRmvList);

					// Get list of all child nodes for oldAttr and iterate through it.
					NodeList subChildElementNodeList = oldAttr.getChildNodes();

					for (int m = 0; m < subChildElementNodeList.getLength(); m++) {
						Node subChildNode = subChildElementNodeList.item(m);
						if (subChildNode.getNodeType() == Node.ELEMENT_NODE) {
							Element subChildElement = (Element) subChildNode;
							// for each subChildElement call diffNode , the first arg is subChildElement and
							// second is newAttrValueEle
							diffNode(subChildElement, newAttrValueEle);
						}
					}

				}

			}
		}

		if (oldChildEmptyNodesRmvList != null && !oldChildEmptyNodesRmvList.isEmpty()) {
			for (int i = 0; i < oldChildEmptyNodesRmvList.size(); i++) {
				Node oldAttr = oldChildEmptyNodesRmvList.get(i);
				oldAttrValueEle.removeChild(oldAttr);
			}
		}

	}

	private List<Node> removeAttrIfEqual(Element oldAttrValueEle, Element newAttrValueEle,
			List<Node> oldChildEmptyNodesRmvList) {

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
				String newAttrName = newAttr.getNodeName();

				// Compare attribute values
				if (oldAttrValue.equalsIgnoreCase(newAttrValue)) {

					// Add the Attribute which needs to be removed in oldChildAttrRmvList
					oldChildAttrRmvList.add(oldAttr);

					// Remove the New attribute
					newAttrValueEle.removeAttributeNode(newAttr);

				}
				if (oldAttrValue.matches("\\d+") && oldAttrValue.length() > 5) {
					// Add the Attribute which needs to be removed in oldChildAttrRmvList

					oldChildAttrRmvList.add(oldAttr);
				}
				if (newAttrValue.matches("\\d+") && newAttrValue.length() > 5) {

					if (newAttrValueEle.hasAttribute(newAttr.getName())) {
						newAttrValueEle.removeAttributeNode(newAttr);
					}
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

		if (!oldAttrValueEle.hasAttributes() && !oldAttrValueEle.hasChildNodes()) {
			oldChildEmptyNodesRmvList.add(oldAttrValueEle);
		}

		return oldChildEmptyNodesRmvList;
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

	private NodeList diffAttributes(Element expectedElement, Element actualElement) throws Exception {

		/*
		 * 1. Get the count of different attributes (add/del/modify) 2. Use the element
		 * with least count 3. Pitfall: No attributes/all count are same , consider all
		 * nodes as matching and continue 4. If attribute name is number and length > 5
		 * , ignore
		 */

		NodeList actualElementNodes = actualElement.getElementsByTagName(expectedElement.getNodeName());

		boolean sameCount = false;
		int minDiffCount = Integer.MAX_VALUE;
		Element matchingElement = null;
		NodeList matchingNodeList = null;

		// Iterate thru each element of actualElement with same nodeName
		for (int itr = 0; itr < actualElementNodes.getLength(); itr++) {
			Node actualElementNode = actualElementNodes.item(itr);

			Element actualEle = (Element) actualElementNode;
			int count = getAttributeDiffCount(expectedElement, actualEle);

			// Update the matching element if it has the least count
			if (count < minDiffCount) {
				minDiffCount = count;
				matchingElement = actualEle;
			} else if (count == minDiffCount) {
				sameCount = true;
			}
		}

		if (sameCount) {

			return matchingNodeList;
		} else {

			// Add the matching element to the matchingNodeList
			if (matchingElement != null) {

				Document document = expectedElement.getOwnerDocument();
				// Create a new NodeList and add the matching element to it
				matchingNodeList = (NodeList) document.createElement("MatchingElements");
				Node importedMatchingElement = document.importNode(matchingElement, true);
				((Node) matchingNodeList).appendChild(importedMatchingElement);
			}

		}

		return matchingNodeList;
	}

	private void removeWhiteSpaceNodes(Element oldAttrValueEle) {

		NodeList oldChildNodes = oldAttrValueEle.getChildNodes();
		for (int itr = 0; itr < oldChildNodes.getLength(); itr++) {
			Node oldChildNode = oldChildNodes.item(itr);
			// condition used to check white spaces
			if (!(oldChildNode.getNodeType() == Node.ELEMENT_NODE)) {
				oldAttrValueEle.removeChild(oldChildNode);
			}
		}
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
				oldAttrValueEle.removeChild(removeNode);
			}
		}
	}

	private List<String> diffAttributes(Node expectedElement, Node actualElement, List<String> diffs) throws Exception {
		// compare attributes
		NamedNodeMap expectedAttrs = expectedElement.getAttributes();
		NamedNodeMap actualAttrs = actualElement.getAttributes();

		for (int i = 0; i < expectedAttrs.getLength(); i++) {
			Attr expectedAttr = (Attr) expectedAttrs.item(i);

			Attr actualAttr = (Attr) actualAttrs.getNamedItem(expectedAttr.getName());

			if (actualAttr == null) {

				diffs.add(expectedAttr.getName() + ": No attribute found:" + expectedAttr);
			}
			if (!expectedAttr.getValue().equals(actualAttr.getValue())) {

				diffs.add(expectedAttr.getName() + ": Attribute values do not match: " + expectedAttr.getValue() + " "
						+ actualAttr.getValue());
			}
		}
		return diffs;
	}

	public Document getProcessedUpdateEnhancedCompareDoc() {
		return processedUpdateEnhancedCompareDoc;
	}

	public void setProcessedUpdateEnhancedCompareDoc(Document processedUpdateEnhancedCompareDoc) {
		this.processedUpdateEnhancedCompareDoc = processedUpdateEnhancedCompareDoc;
	}

}
