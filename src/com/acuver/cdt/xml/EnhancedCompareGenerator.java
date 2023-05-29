package com.acuver.cdt.xml;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnhancedCompareGenerator {

	Document processedUpdateEnhancedCompareDoc;

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

		removeIfEqual(oldAttrValueEle, newAttrValueEle);

		// cloned the subxml as it will be modified to keep only differences , original
		// subxml will be used for comparison
		Element oldDiffEle = (Element) oldAttrValueEle.cloneNode(true);

		diffNode(oldDiffEle, newAttrValueEle);

		// vice-versa
		Element newDiffEle = (Element) newAttrValueEle.cloneNode(true);

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

		if (oldAttrValueEle.getNodeName().equalsIgnoreCase("Points")) {
			if (oldAttrValueEle.getParentNode() != null)
				oldAttrValueEle.getParentNode().removeChild(oldAttrValueEle);
			return;
		}

		Element matchingSubEle = diffAttributes(oldAttrValueEle, newAttrValueEle);

		if (matchingSubEle != null) {

			// if matching node found , remove equal attributes
			removeAttrIfEqual(oldAttrValueEle, matchingSubEle);

			// Get list of all child nodes for oldAttr and iterate through it.
			NodeList subChildElementNodeList = oldAttrValueEle.getChildNodes();

			for (int m = 0; m < subChildElementNodeList.getLength(); m++) {
				Node subChildNode = subChildElementNodeList.item(m);
				if (subChildNode.getNodeType() == Node.ELEMENT_NODE) {
					Element subChildElement = (Element) subChildNode;
					// for each subChildElement call diffNode , the first arg is subChildElement and
					// second is newAttrValueEle
					diffNode(subChildElement, matchingSubEle);
				}
			}

		}

		removeWhiteSpaceNodes(oldAttrValueEle);

		if (!(oldAttrValueEle.hasAttributes()) && !(oldAttrValueEle.hasChildNodes())) {
			if (oldAttrValueEle.getParentNode() != null)
				oldAttrValueEle.getParentNode().removeChild(oldAttrValueEle);
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

		NodeList actualElementNodes = actualElement.getElementsByTagName(expectedElement.getNodeName());

		int minDiffCount = Integer.MAX_VALUE;
		Element matchingElement = null;

		ArrayList<Integer> countList = new ArrayList<Integer>();

		// Iterate thru each element of actualElement with same nodeName
		for (int itr = 0; itr < actualElementNodes.getLength(); itr++) {
			Node actualElementNode = actualElementNodes.item(itr);

			Element actualEle = (Element) actualElementNode;
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

			if (min == max)
				return null;
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