package com.acuver.cdt.xml;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.file.CDTFileReader;
import com.acuver.cdt.util.CDTConstants;
import com.acuver.cdt.util.CDTHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import java.util.HashMap;
import java.util.Map;

public class RecordIdentifer {
    private String tableName;
    private String tablePrefix;
    private Document doc;
    private Element elemToMatch;
    private CDTFileReader fileReader;

    public Element getMatchingUniqueElement(boolean isCompareReport) throws Exception {
        Element outEle = null;
        //get the element with primary key value
        String primaryKeyName = tablePrefix + CDTConstants.key;
        String primaryKeyValue = elemToMatch.getAttribute(primaryKeyName);
        if (primaryKeyValue != null && !primaryKeyValue.trim().isEmpty()) {
            outEle = getElementUsingXpath(new StringBuffer("//[@" + primaryKeyName + "='" + primaryKeyValue + "']"), isCompareReport, doc);
        }

        if (outEle != null)
            return outEle;
        StringBuffer xpathExpr = new StringBuffer(CDTConstants.forwardSlash);
        Map<String, String> tblRecordIdentifierMap = getUniqueIdentifier(tableName);
        if (!tblRecordIdentifierMap.isEmpty()) {

            tblRecordIdentifierMap.forEach((k, v) -> xpathExpr.append("[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + k + "']='" + v + "']"));

            outEle = getElementUsingXpath(xpathExpr, isCompareReport, doc);
        }
        return outEle;
    }


    private Map<String, String> getUniqueIdentifier(String tableName) throws Exception {
        Map<String, String> tblRecordIdentifierMap = new HashMap<String, String>();

        String attrName = tablePrefix.toLowerCase();
        String value = getAttributeValue(attrName);
        if (value == null || value.trim().isEmpty()) {
            attrName = tablePrefix.toLowerCase() + CDTConstants.id;
            value = getAttributeValue(attrName);
            if (value == null || value.trim().isEmpty()) {
                attrName = tablePrefix.toLowerCase() + CDTConstants.code;
                value = getAttributeValue(attrName);
                if (value == null || value.trim().isEmpty()) {
                    attrName = tablePrefix.toLowerCase() + CDTConstants.name;
                    value = getAttributeValue(attrName);
                }
            }
        }

        addRecordIdentifier(attrName, value, tblRecordIdentifierMap);

        if (tblRecordIdentifierMap.isEmpty()) {
            //read identifiers from config file
            String configValue = EnhancedCDTMain.recordIdentifierMap.get(tableName);
            if (configValue != null && !configValue.trim().isEmpty()) {
                String[] identifiers = configValue.split(",");
                for (String identifier : identifiers) {
                    String attrValue = null;

                    if (identifier.contains("|")) {
                        addParentKeyValue(identifier, tblRecordIdentifierMap);
                    } else {
                        addRecordIdentifier(identifier.toLowerCase(), elemToMatch.getAttribute(identifier), tblRecordIdentifierMap);
                    }
                }
            }
        }
        addRecordIdentifier(CDTConstants.organizationCode.toLowerCase(), elemToMatch.getAttribute(CDTConstants.organizationCode), tblRecordIdentifierMap);
        addRecordIdentifier(CDTConstants.processTypeKey.toLowerCase(), elemToMatch.getAttribute(CDTConstants.processTypeKey), tblRecordIdentifierMap);

        return tblRecordIdentifierMap;
    }


    private void addParentKeyValue(String identifier, Map<String, String> tblRecordIdentifierMap) throws Exception {

        String[] identifierWithParent = identifier.split("\\|");

        String parentTable = identifierWithParent[0];
        String parentKeyAttr = identifierWithParent[1];

        String attrValue = elemToMatch.getAttribute(parentKeyAttr);
        Document parentDoc = fileReader.readFileFromDir(EnhancedCDTMain.CDT_XMLS2, parentTable);
        //get the insert element from parent table with child insert element key

        Element parentInsertEle = getElementUsingXpath(
                new StringBuffer("//[@" + parentKeyAttr + "='" + attrValue + "']"), false, parentDoc);

        if (parentInsertEle != null) {
            String parentTablePrefix = CDTHelper.getTablePrefix(parentTable);
            RecordIdentifer parentRecordIdentifer = new RecordIdentifer();
            parentRecordIdentifer.setTableName(parentTable);
            parentRecordIdentifer.setTablePrefix(parentTablePrefix);
            parentRecordIdentifer.setDoc(parentDoc);
            parentRecordIdentifer.setElemToMatch(parentInsertEle);
            Element deleteElement = parentRecordIdentifer.getMatchingUniqueElement(true);
            if (deleteElement != null) {
                attrValue = deleteElement.getAttribute(parentKeyAttr);
            }
        }
        addRecordIdentifier(parentKeyAttr.toLowerCase(), attrValue, tblRecordIdentifierMap);
    }

    private void addRecordIdentifier(String name, String value, Map<String, String> recordIdentifierMap) {
        if (value != null && !value.trim().isEmpty()) {
            recordIdentifierMap.put(name, value);
        }
    }

    private String getAttributeValue(String attrNameLower) throws Exception {
        String xpath = "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + attrNameLower + "']";
        return (String) EnhancedCDTMain.xPath.compile(xpath).evaluate(elemToMatch, XPathConstants.STRING);
    }

    private Element getElementUsingXpath(StringBuffer xpathExpr, boolean isCompareReport, Document doc) throws Exception {
        Element outEle = null;
        if (isCompareReport) {
            xpathExpr = xpathExpr.insert(2, CDTConstants.DELETE);
        } else {
            xpathExpr = xpathExpr.insert(2, "*");
        }

        NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(xpathExpr.toString()).evaluate(doc, XPathConstants.NODESET);

        if (nodeList != null && nodeList.getLength() > 0) {
            System.out.println("\ngetUniqueElement nodeList length :" + nodeList.getLength());
            outEle = (Element) nodeList.item(0);
        }
        return outEle;
    }


    public void setFileReader(CDTFileReader fileReader) {
        this.fileReader = fileReader;
    }


    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public void setElemToMatch(Element elemToMatch) {
        this.elemToMatch = elemToMatch;
    }
}
