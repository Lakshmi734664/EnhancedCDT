package com.acuver.cdt.xml;

import com.acuver.cdt.EnhancedCDTMain;
import com.acuver.cdt.util.CDTConstants;
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

    private Element getMatchingUniqueElement(boolean isCompareReport) throws Exception
    {
        Element outEle = null;
        String primaryKeyName = tablePrefix + "Key";
        String primaryKeyValue = elemToMatch.getAttribute(primaryKeyName);
        StringBuffer xpathExpr = null;
        NodeList nodeList = null;
        if (primaryKeyValue != null && !primaryKeyValue.isEmpty()) {
            xpathExpr = new StringBuffer("//" + "[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
                    + primaryKeyName.toLowerCase() + "']='" + primaryKeyValue + "']");
            outEle = getElementUsingXpath( xpathExpr, isCompareReport);
        }
        if(outEle == null) {
            String tablePrefixLower = tablePrefix.toLowerCase();
            String attrWithName = tablePrefixLower + "name";
            String attrWithId = tablePrefixLower + "id";
            String attrWithCode = tablePrefixLower + "code";
            System.out.println("tablePrefixLower : " + tablePrefixLower);
        }
        return outEle;
    }

    private Map<String,String> getUniqueIdentifier() throws Exception
    {
        Map<String,String> recordIdentifierMap = new HashMap<String,String>();

        String attrName = tablePrefix.toLowerCase();
        String value = getAttributeValue(attrName);
        if(value == null || value.trim().isEmpty())
        {
            attrName = tablePrefix.toLowerCase() + "name";
            value = getAttributeValue(attrName);
            if(value == null || value.trim().isEmpty())
            {
                attrName = tablePrefix.toLowerCase() + "id";
                value = getAttributeValue(attrName );
                if(value == null || value.trim().isEmpty())
                {
                    attrName = tablePrefix.toLowerCase() + "code";
                    value = getAttributeValue(attrName);
                }
            }
        }
        addRecordIdentifier(attrName, value, recordIdentifierMap);

        if(recordIdentifierMap.isEmpty())
        {
            //read identifiers from config file
            String configValue = EnhancedCDTMain.recordIdentifierMap.get(tableName);
            if(configValue != null && !configValue.trim().isEmpty())
            {

            }
        }
        addRecordIdentifier(CDTConstants.organizationCode, elemToMatch.getAttribute(CDTConstants.organizationCode), recordIdentifierMap);
        addRecordIdentifier(CDTConstants.processTypeKey, elemToMatch.getAttribute(CDTConstants.processTypeKey), recordIdentifierMap);

        return recordIdentifierMap;
    }

    private void addRecordIdentifier(String name , String value ,  Map<String,String> recordIdentifierMap)
    {
        if(value != null && !value.trim().isEmpty())
        {
            recordIdentifierMap.put(name,value);
        }
    }
    private String getAttributeValue(String attrNameLower) throws Exception
    {

        String xpath = "//[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
                + attrNameLower + "]";

        String attrValue = (String) EnhancedCDTMain.xPath.compile(xpath).evaluate(elemToMatch, XPathConstants.STRING);
        return attrValue;

    }

    private Element getElementUsingXpath(StringBuffer xpathExpr, boolean isCompareReport) throws Exception {
        Element outEle = null;
        if (isCompareReport) {
            xpathExpr = xpathExpr.insert(1, "//" + CDTConstants.DELETE);
        }

        NodeList nodeList = (NodeList) EnhancedCDTMain.xPath.compile(xpathExpr.toString()).evaluate(doc, XPathConstants.NODESET);
        if (nodeList != null && nodeList.getLength() > 0) {
            System.out.println("\ngetUniqueElement nodeList length :" + nodeList.getLength());
            outEle = (Element) nodeList.item(0);
        }
        return outEle;
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
