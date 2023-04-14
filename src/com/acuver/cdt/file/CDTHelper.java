package com.acuver.cdt.file;

import javax.swing.JOptionPane;

public class CDTHelper {
        
        public static String printmsg(String message) {
            JOptionPane.showMessageDialog(null, message);
			return message;
        }
        
        public static void formpropertiesfilehelpmsg() {
            String message = "This is a help message for the form properties file.";
            JOptionPane.showMessageDialog(null, message, "Form Properties File Help", JOptionPane.INFORMATION_MESSAGE);
        }
}
