package com.acuver.cdt.util;

import com.acuver.cdt.EnhancedCDTMain;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class CDTHelper {

	public static void printMsg(String msg) {
		System.out.println(msg);
	}

	public static void showPropertiesFileHelpMsg() {
		String message = "Please create a file enhancedcdt.properties in the current folder. The following properties can be configured.\r\n"
				+ "	CDT_REPORT_DIR1                 Source Env-1 and Target Env-2 CDT Compare Report Folder\r\n"
				+ "	CDT_REPORT_DIR2 	        Source Env-2 and Target Env-1 CDT Compare Report Folder\r\n"
				+ "	CDT_XMLS1  			Env-1 CDT Export XMLs Folder\r\n"
				+ "	CDT_XMLS2  			Env-2 CDT Export XMLs Folder\r\n"
				+ "	YDKPREF1			Env-1 ydfprefs.xml (Optional) \r\n"
				+ "        YDKPERF2	                Env-2 ydfprefs.xml (Optional) \r\n"
				+ "        OUTPUT_DIR 			Output Folder(Optional)\r\n"
				+ "	CDT_REPORT_OUT_DIR1             Used with mergeManualReview option. Source Env-1 and Target Env-2 Enhanced CDT Compare Report Folder\r\n"
				+ "	CDT_REPORT_OUT_DIR2             Used with mergeManualReview option. Source Env-2 and Target Env-1 Enhanced CDT Compare Report Folder";

		printMsg(message);
	}

	// Get Table Primary Key Name
	public static String getTablePrefix(String tableName) {
		int beginIndex = tableName.indexOf(CDTConstants.hyphen);
		String name = tableName.substring(beginIndex).toLowerCase();
		name = name.replace(CDTConstants.hyphen, " ");
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
		String tablePrefix = String.valueOf(charArray);
		tablePrefix = tablePrefix.replaceAll(CDTConstants.spaces, "");
		return tablePrefix;
	}

	public static Element createChildElement(Element element, String childName) {
		Document doc = element.getOwnerDocument();
		Element childElement = doc.createElement(childName);
		element.appendChild(childElement);
		return childElement;
	}

	public static Element getChildElement(Element element, String childName) {
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
		return childElement;
	}

	public static String convertDocumentToString(Document document) {
		try {

			Transformer transformer = EnhancedCDTMain.tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
			document.setXmlStandalone(true);

			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

			return stringWriter.toString().replaceAll("\\n\\s*\\n", "\n");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}