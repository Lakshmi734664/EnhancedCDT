package com.acuver.cdt.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CDTHelper {

	public void printMsg(String msg) {
		System.out.println(msg);
	}

	public void showPropertiesFileHelpMsg() {
		String message = "Please create a file enhancedcdt.properties in current folder. The following properties can be configured.\r\n"
				+ "	CDT_REPORT_DIR1     CDT Compare Report1\r\n" + "	CDT_REPORT_DIR2     CDT Compare Report2\r\n"
				+ "	CDT_XMLS1  	    CDT Export XMLs1\r\n" + "	CDT_XMLS2  	    CDT Export XMLs\r\n"
				+ "	YDKPREF1	    ydfprefs.xml (Optional)\r\n" + "        YDKPERF2	    ydfprefs.xml (Optional)\r\n"
				+ "        OUTPUT_DIR 	    Output Folder (Optional)";
		printMsg(message);
	}

	public Element createChildElement(Element element, String childName) {
		Document doc = element.getOwnerDocument();
		Element childElement = doc.createElement(childName);
		element.appendChild(childElement);
		return childElement;
	}


	public Element getChildElement(Element element, String childName) {
		NodeList childNodes = element.getChildNodes();
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
			Document doc = element.getOwnerDocument();
			childElement = doc.createElement(childName);
			element.appendChild(childElement);
		}

		return childElement;
	}

}
