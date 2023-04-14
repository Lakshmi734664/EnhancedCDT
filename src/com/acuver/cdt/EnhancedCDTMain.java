package com.acuver.cdt;

import com.acuver.cdt.file.CDTFileWriter;

public class EnhancedCDTMain {
    public static void main(String args[]) {
  
        
        String fileData = "<Order  DocumentType=\"0001\"  EnterpriseCode=\"Matrix\"  >\r\n"
        		+ "<OrderHoldTypes>\r\n"
        		+ "<OrderHoldType HoldType=\"FRAUD_HOLD\"/>\r\n"
        		+ "</OrderHoldTypes>\r\n"
        		+ "\r\n"
        		+ "<OrderLines>\r\n"
        		+ "<OrderLine OrderedQty=\"1\" >\r\n"
        		+ "<Item ItemID=\"100013\" UnitCost=\"10.0\" UnitOfMeasure=\"EACH\"/>\r\n"
        		+ "</OrderLine>\r\n"
        		+ "</OrderLines>\r\n"
        		+ "<PersonInfoShipTo AddressLine1=\"234 Copley Place\" City=\"Boston\" Country=\"US\" DayPhone=\"\"  EMailID=\"\" FirstName=\"Lakshmi\" LastName=\"A\" MobilePhone=\"\"  State=\"MA\"  ZipCode=\"02116\"/>\r\n"
        		+ "<PersonInfoBillTo AddressLine1=\"234 Copley Place\" City=\"Boston\" Country=\"US\" DayPhone=\"\"  EMailID=\"\" FirstName=\"Lakshmi\" LastName=\"A\" MobilePhone=\"\"  State=\"MA\"  ZipCode=\"02116\"/>\r\n"
        		+ "</Order>\r\n"
        		+ " ";
        
        
        String fileLocation = "D://Reports//CDT//";
        String fileName = "test";
        CDTFileWriter writer  = new CDTFileWriter(fileLocation,fileName,fileData);
        
        
    }
}
