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

    public Element getMatchingUniqueElement(boolean isCompareReport) throws Exception
    {
        Element outEle = null;
        Map<String,String> tblRecordIdentifierMap = getUniqueIdentifier();
        if(!tblRecordIdentifierMap.isEmpty())
        {
            StringBuffer xpathExpr = new StringBuffer("//");
            tblRecordIdentifierMap.forEach((k, v) ->  xpathExpr.append("[@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='"
                    + k +  "']='" + v + "']"));

            outEle = getElementUsingXpath( xpathExpr, isCompareReport);
        }
        return outEle;
    }

    private Map<String,String> getUniqueIdentifier() throws Exception
    {
        Map<String,String> tblRecordIdentifierMap = new HashMap<String,String>();

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
        addRecordIdentifier(attrName, value, tblRecordIdentifierMap);

        if(tblRecordIdentifierMap.isEmpty())
        {
            //read identifiers from config file
            String configValue = EnhancedCDTMain.recordIdentifierMap.get(tableName);
            if(configValue != null && !configValue.trim().isEmpty())
            {

            }
        }
        addRecordIdentifier(CDTConstants.organizationCode, elemToMatch.getAttribute(CDTConstants.organizationCode), tblRecordIdentifierMap);
        addRecordIdentifier(CDTConstants.processTypeKey, elemToMatch.getAttribute(CDTConstants.processTypeKey), tblRecordIdentifierMap);

        return tblRecordIdentifierMap;
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
        String xpath = "@*[translate(name(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + attrNameLower + "']";
        return (String) EnhancedCDTMain.xPath.compile(xpath).evaluate(elemToMatch, XPathConstants.STRING);
    }

    private Element getElementUsingXpath(StringBuffer xpathExpr, boolean isCompareReport) throws Exception {
        Element outEle = null;
        if (isCompareReport) {
            xpathExpr = xpathExpr.insert(2,  CDTConstants.DELETE);
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
