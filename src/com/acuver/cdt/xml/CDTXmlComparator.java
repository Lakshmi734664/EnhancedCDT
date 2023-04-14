package com.acuver.cdt.xml;

import java.io.File;
import java.io.StringWriter;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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

	public void cleanCompareReport(File f) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// an instance of builder to parse the specified xml file
		DocumentBuilder db = null;
		db = dbf.newDocumentBuilder();
		Document doc = null;
		doc = db.parse(f);
		toString(doc, "Input: ");
		// doc.normalizeDocument();
		XPath xPath = XPathFactory.newInstance().newXPath();
		String expression = "//Insert";
		NodeList nodeList = null;
		nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

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
							toString(doc, "Output: ");
						} else {
							System.out.println("primary key issue");
							String nodeName1 = node.getNodeName();
							String nodeName2 = node2.getNodeName();
							int nodeIndex1 = itr;
							int nodeIndex2 = itr2;
							processInsertDeleteTags(doc, nodeName1, nodeName2, nodeIndex1, nodeIndex2);
						}
					}
				}
			}
		}
	}

	// Processing Insert/Delete Tags
	public void processInsertDeleteTags(Document doc, String nodeName1, String nodeName2, int nodeIndex1,
			int nodeIndex2) throws Exception {

		// Removing the Nodes with primary key issue from the Main Document Object

		Element element1 = (Element) doc.getElementsByTagName(nodeName1).item(nodeIndex1);
		// remove the specific node
		element1.getParentNode().removeChild(element1);

		Element element2 = (Element) doc.getElementsByTagName(nodeName2).item(nodeIndex2);
		// remove the specific node
		element2.getParentNode().removeChild(element2);
		doc.normalizeDocument();
		toString(doc, "Output: ");

	}

	// Convert Document object to String
	private static void toString(Document newDoc, String Type) throws Exception {
		DOMSource domSource = new DOMSource(newDoc);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		System.out.println(Type + "\n" + sw.toString());
	}
}
