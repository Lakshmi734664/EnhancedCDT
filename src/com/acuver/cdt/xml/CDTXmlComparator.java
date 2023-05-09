package com.acuver.cdt.xml;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.file.CDTFileWriter;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

public class CDTXmlComparator {
    private String tableName;
    private String tablePrefix;
    private String outDir;
    private Document inputDoc;
    private CDTFileReader fileReader;
    private CDTFileWriter fileWriter;
    private CDTXmlDifferenceEvaluator CDTXmlDifferenceEvaluator = new CDTXmlDifferenceEvaluator();
    private CDTHelper cdtHelper = new CDTHelper();

    private RecordIdentifer recordIdentifer = new RecordIdentifer();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public Document getInputDoc() {
        return inputDoc;
    }

    public void setInputDoc(Document inputDoc) {
        this.inputDoc = inputDoc;
    }

    public String getOutDir() {
        return outDir;
    }

    public void setOutDir(String outDir) {
        this.outDir = outDir;
    }

    public CDTFileReader getFileReader() {
        return fileReader;
    }

    public void setFileReader(CDTFileReader fileReader) {
        this.fileReader = fileReader;
    }

    public CDTFileWriter getFileWriter() {
        return fileWriter;
    }

    public void setFileWriter(CDTFileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    public Document merge() throws Exception {

        Element root = inputDoc.getDocumentElement();
        tableName = root.getNodeName();
        System.out.println("tableName : " + tableName);

        // Getting the Table Prefix Name
        tablePrefix = getTablePrefix(tableName);
        String primaryKeyName = tablePrefix + "Key";
        CDTXmlDifferenceEvaluator.setPrimaryKeyName(primaryKeyName);

        recordIdentifer.setTableName(tableName);
        recordIdentifer.setTablePrefix(tablePrefix);

        // Processing Insert/Delete Tags
        inputDoc = processInsertDeleteElements(inputDoc);

        // Remove False Update
        /*
         * inputDoc = removeFalseUpdates(inputDoc);
         *
         * // Processing Update Elements with EnhancedCompare
         * addEnhancedCompareToUpdates(inputDoc);
         *
         * //inputDoc = moveUpdatesToManualReview(inputDoc);
         *
         * inputDoc = removeDeleteTags(inputDoc);
         */

        System.out.println("After Merge OutDoc:" + fileWriter.convertDocumentToString(inputDoc));
        return inputDoc;
    }

    public Document processInsertDeleteElements(Document doc) throws Exception {
        System.out.println("Entering processInsertDeleteElements : ");
        Element rootEle = doc.getDocumentElement();

        String expression = "//" + CDTConstants.INSERT;
        NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        debug(doc, "Before processInsertDeleteElements");
        for (int itr = 0; itr < nodeList.getLength(); itr++) {

            System.out.println("itr " + itr);
            Element insertElement = (Element) nodeList.item(itr);

            recordIdentifer.setDoc(doc);
            recordIdentifer.setElemToMatch(insertElement);
            Element deleteElement = recordIdentifer.getMatchingUniqueElement(true);
            if(deleteElement != null)
            {
                boolean insertHasDifferences = false;
                System.out.println("Comparing insertNode with deleteNode : ");

                Diff diff = DiffBuilder.compare(insertElement).withTest(deleteElement).checkForSimilar()
                        .ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace().normalizeWhitespace()
                        .withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();

                if (diff.hasDifferences()) {
                    Element updateElement = doc.createElement("Update");
                    // Replace Insert element with Update element
                    NamedNodeMap attributes = insertElement.getAttributes();
                    for (int j = 0; j < attributes.getLength(); j++) {
                        Node attribute = attributes.item(j);
                        updateElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());

                    }

                    String primaryKeyName = tablePrefix + "Key";
                    System.out.println(primaryKeyName);
                    String primaryKeyValue = deleteElement.getAttribute(primaryKeyName);
                    System.out.println(primaryKeyValue);

                    updateElement.setAttribute(primaryKeyName, primaryKeyValue);

                    Element oldValuesElement = cdtHelper.createChildElement(updateElement, "OldValues");

                    // Store the difference values in the OldValues element
                    Iterator<Difference> iter = diff.getDifferences().iterator();
                    while (iter.hasNext()) {
                        String difference = iter.next().toString();
                        System.out.println(difference);

                        if (difference != null && !difference.contains("xml version")) {
                            int attrNameStartIndex = difference.indexOf("@") + 1;
                            int attrNameEndIndex = difference.indexOf("to <");

                            System.out.println("attrNameStartIndex: " + attrNameStartIndex);
                            System.out.println("attrNameEndIndex: " + attrNameEndIndex);
                            String attrName = difference.substring(attrNameStartIndex, attrNameEndIndex).trim();

                            System.out.println("UpdateAttrName :" + attrName);

                            int oldAttrValueStartIndex = difference.indexOf("but was '") + 9;
                            int oldAttrValueEndIndex = difference.indexOf("- comparing") - 2;
                            String oldAttrValue = difference.substring(oldAttrValueStartIndex, oldAttrValueEndIndex)
                                    .trim();

                            System.out.println("Old Attribute Value: " + oldAttrValue);

                            oldValuesElement.setAttribute(attrName, oldAttrValue);
                        }
                    }
                    rootEle.appendChild(updateElement);

                } else {
                    System.out.println(
                            "No Difference In insertNode and deleteNode. Both Insert and Delete Nodes are removed.");
                }
                System.out.println("removing insert ele at itr " + itr);

                rootEle.removeChild(insertElement);
                rootEle.removeChild(deleteElement);

            }
        }
        debug(doc, "After processInsertDeleteElements");
        System.out.println("Exiting processInsertDeleteElements : ");
        return doc;
    }

    // Remove False Update
    public Document removeFalseUpdates(Document doc) throws Exception {
        System.out.println("Entering removeFalseUpdates : ");
        debug(doc, "Before removeFalseUpdates");
        String expression = "//" + CDTConstants.UPDATE + "//" + CDTConstants.OLDVALUES;
        NodeList nodeList = null;
        nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        System.out.println("Old Values nodeList Length()" + nodeList.getLength());
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node oldValueNode = nodeList.item(itr);
            Node updateNode = oldValueNode.getParentNode();
            System.out.println("\nNode Name :" + oldValueNode.getNodeName());
            NamedNodeMap attrList = oldValueNode.getAttributes();
            for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
                Node oldValueAttr = attrList.item(attrItr);
                String oldValueAttrName = oldValueAttr.getNodeName();
                String oldValueAttrValue = oldValueAttr.getNodeValue();
                if (isSubXML(oldValueAttrName, oldValueAttrValue)) {
                    System.out.println("The Attribute Value is a SUB-XML : " + oldValueAttrName);
                    NamedNodeMap updateAttrList = updateNode.getAttributes();
                    Diff diff = null;
                    for (int attrItr2 = 0; attrItr2 < updateAttrList.getLength(); attrItr2++) {
                        Node updateAttr = updateAttrList.item(attrItr2);
                        String updateAttrname = updateAttr.getNodeName();
                        String updateAttrValue = updateAttr.getNodeValue();

                        if (oldValueAttrName.equalsIgnoreCase(updateAttrname)) {
                            System.out.println("Inside Sub XML Diff : ");
                            System.out.println("updateAttrValue : " + updateAttrValue);
                            System.out.println("oldValueAttrValue : " + oldValueAttrValue);

                            diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue).checkForSimilar()
                                    .ignoreComments().ignoreWhitespace().ignoreElementContentWhitespace()
                                    .normalizeWhitespace().build();

                            if (diff != null && diff.hasDifferences()) {
                                Iterator<Difference> iter = diff.getDifferences().iterator();
                                while (iter.hasNext()) {
                                    String subXmlDiffData = iter.next().toString();
                                    System.out.println(subXmlDiffData);
                                }
                            } else {
                                updateNode.getParentNode().removeChild(updateNode);
                                System.out.println("No Difference in Sub XML : ");
                            }
                        }
                    }
                }
            }
        }
        debug(doc, "After removeFalseUpdates");
        System.out.println("Exiting removeFalseUpdates : ");
        return doc;

    }

    // Processing Update Doc with EnhancedCompare
    public Document addEnhancedCompareToUpdates(Document doc) throws Exception {

        Document processedUpdateEnhancedCompareDoc = doc;
        String expression = "//" + CDTConstants.UPDATE + "//" + CDTConstants.OLDVALUES;
        NodeList nodeList = null;
        nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

        if (nodeList == null || nodeList.getLength() == 0) {

            return doc;

        } else {

            System.out.println("Old Values nodeList Length()" + nodeList.getLength());
            for (int itr = 0; itr < nodeList.getLength(); itr++) {
                Node oldValueNode = nodeList.item(itr);
                Node updateNode = oldValueNode.getParentNode();
                System.out.println("\nNode Name :" + oldValueNode.getNodeName());
                NamedNodeMap attrList = oldValueNode.getAttributes();
                for (int attrItr = 0; attrItr < attrList.getLength(); attrItr++) {
                    Node oldValueAttr = attrList.item(attrItr);
                    String oldValueAttrName = oldValueAttr.getNodeName();
                    String oldValueAttrValue = oldValueAttr.getNodeValue();
                    if (isSubXML(oldValueAttrName, oldValueAttrValue)) {
                        System.out.println("The Attribute Value is a SUB-XML : " + oldValueAttrName);
                        NamedNodeMap updateAttrList = updateNode.getAttributes();
                        Diff diff = null;
                        for (int attrItr2 = 0; attrItr2 < updateAttrList.getLength(); attrItr2++) {
                            Node updateAttr = updateAttrList.item(attrItr2);
                            String updateAttrname = updateAttr.getNodeName();
                            String updateAttrValue = updateAttr.getNodeValue();
                            if (oldValueAttrName.equalsIgnoreCase(updateAttrname)) {
                                System.out.println("Inside Sub XML Diff : ");
                                System.out.println("oldValueAttrValue : " + oldValueAttrValue);
                                System.out.println("updateAttrValue : " + updateAttrValue);
                                diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue)
                                        .checkForSimilar().ignoreComments().ignoreWhitespace()
                                        .ignoreElementContentWhitespace().normalizeWhitespace().build();

                                /*
                                 * diff = DiffBuilder.compare(oldValueAttrValue).withTest(updateAttrValue)
                                 * .checkForSimilar().ignoreComments().ignoreWhitespace()
                                 * .ignoreElementContentWhitespace().normalizeWhitespace()
                                 * .withDifferenceEvaluator(CDTXmlDifferenceEvaluator).build();
                                 */

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
                                        processedUpdateEnhancedCompareDoc = addEnhancedCompareToProcessedUpdateDoc(
                                                processedUpdateEnhancedCompareDoc, updateNode, subXmlDiffDataList,
                                                oldValueAttrValue);
                                    }
                                } else {
                                    System.out.println("No Difference in Sub XML : ");

                                }
                            }
                        }
                    }
                }
            }
        }
        return processedUpdateEnhancedCompareDoc;
    }

    // Add Enhanced Compare Elements To Processed Update Doc
    public Document addEnhancedCompareToProcessedUpdateDoc(Document processedUpdateEnhancedCompareDoc, Node updateNode,
                                                           ArrayList<String> subXmlDiffDataList, String subXmlOldAttrValue) throws Exception {

        Element enhancedCompareEle = processedUpdateEnhancedCompareDoc.createElement("EnhancedCompare");
        Element attributeEle = processedUpdateEnhancedCompareDoc.createElement("Attribute");
        Element oldAttrEle = processedUpdateEnhancedCompareDoc.createElement("Old");
        Element newAttrEle = processedUpdateEnhancedCompareDoc.createElement("New");

        for (int i = 0; i < subXmlDiffDataList.size(); i++) {
            String diffValues = subXmlDiffDataList.get(i);
            if (diffValues.startsWith("Expected attribute value")) {
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
            } else if (diffValues.startsWith("Expected child nodelist length")) {
                System.out.println("diffValues : " + diffValues);
                int beginIndex = diffValues.indexOf("length") + 8;
                int endIndex = diffValues.indexOf("but was") - 2;
                System.out.println("beginIndex: " + beginIndex + "endIndex : " + endIndex);
                String oldAttributeSize = diffValues.substring(beginIndex, endIndex);
                System.out.println("oldAttributeSize : " + oldAttributeSize);
                int updateBeginIndex = diffValues.indexOf("but was") + 9;
                int updateEndIndex = diffValues.indexOf("- comparing") - 2;
                String updateAttributeSize = diffValues.substring(updateBeginIndex, updateEndIndex);
                System.out.println("updateAttributeSize : " + updateAttributeSize);
                Document subxmlDoc = EnhancedCDTMain.factory.newDocumentBuilder()
                        .parse(new InputSource(new StringReader(subXmlOldAttrValue)));
                System.out.println("Sub-XML found in attribute : " + subXmlOldAttrValue);
                Element subxmlElem = subxmlDoc.getDocumentElement();
                System.out.println("Sub-XML Element Name : " + subxmlElem.getNodeName());

            }

        }
        NamedNodeMap updateAttrList = updateNode.getAttributes();
        for (int attrItr = 0; attrItr < updateAttrList.getLength(); attrItr++) {
            Node updateAttr = updateAttrList.item(attrItr);
            String updateAttrname = updateAttr.getNodeName();
            String updateAttrValue = updateAttr.getNodeValue();
            if (isSubXML(updateAttrname, updateAttrValue)) {
                System.out.println("The Attribute Value is a SUB-XML : " + updateAttrname);
                System.out.println("updateAttrname :" + updateAttrname);
                attributeEle.setAttribute("Name", updateAttrname);
            }
        }
        attributeEle.appendChild(oldAttrEle);
        attributeEle.appendChild(newAttrEle);
        enhancedCompareEle.appendChild(attributeEle);
        updateNode.appendChild(enhancedCompareEle);

        return processedUpdateEnhancedCompareDoc;
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
        Document processedManualReviewDoc = parser.newDocument();
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
                fileWriter.writeFile(EnhancedCDTMain.CDT_REPORT_DIR1_OUT, processedManualReviewDoc, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Checking if the Attribute is Sub XML
    private boolean isSubXML(String attrName, String attrValue) {
        return attrValue.startsWith("<?xml");
    }

    // Get Table Primary Key Name
    private String getTablePrefix(String tableName) {
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
        String tablePrefix = String.valueOf(charArray);
        tablePrefix = tablePrefix.replaceAll("\\s", "");
        return tablePrefix;
    }

    // remove delete tag from document
    public Document removeDeleteTags(Document doc) throws Exception {

        NodeList nodes = doc.getElementsByTagName("Delete");

        for (int i = nodes.getLength() - 1; i >= 0; i--) {
            Node node = nodes.item(i);
            node.getParentNode().removeChild(node);
        }

        // Create a transformer to serialize the document to a string
        Transformer transformer = EnhancedCDTMain.tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(new DOMSource(doc), result);

        // Remove all blank lines from the serialized string
        String xmlString = writer.toString();
        Pattern pattern = Pattern.compile("(?m)^\\s*$[\n\r]{1,}", Pattern.MULTILINE);
        xmlString = pattern.matcher(xmlString).replaceAll("");

        // Parse the modified string back into a DOM document
        DocumentBuilder builder = EnhancedCDTMain.factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlString));
        Document doc1 = builder.parse(inputSource);
        return doc1;

    }

    public void debug(Document doc, String s) throws Exception {
        String expression = "//" + CDTConstants.INSERT;
        NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        System.out.println(s + "  Insert nodeList Length() " + nodeList.getLength());

        expression = "//" + CDTConstants.DELETE;
        nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        System.out.println(s + "  Delete nodeList Length() " + nodeList.getLength());

        expression = "//" + CDTConstants.UPDATE;
        nodeList = (NodeList) EnhancedCDTMain.xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        System.out.println(s + "  Update nodeList Length() " + nodeList.getLength());
    }

}