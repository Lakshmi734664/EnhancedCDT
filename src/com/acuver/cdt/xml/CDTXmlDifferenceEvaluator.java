package com.acuver.cdt.xml;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

public class CDTXmlDifferenceEvaluator implements DifferenceEvaluator {

	@Override
	public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {

		switch (comparison.getType()) {
		case ATTR_VALUE:
			Attr attr = (Attr) comparison.getControlDetails().getTarget();

			if (attr.getName().endsWith("Key") || attr.getName().equalsIgnoreCase("Lockid")
					|| attr.getName().equalsIgnoreCase("__ID__")) {
				return ComparisonResult.EQUAL;
			}
			break;
		case ATTR_NAME_LOOKUP:
			String trgtAttrName = "";
			String testAttrName = "";

			if (comparison.getControlDetails().getValue() != null) {
				trgtAttrName = ((QName) comparison.getControlDetails().getValue()).getLocalPart();
			}
			if (comparison.getTestDetails().getValue() != null) {
				testAttrName = ((QName) comparison.getTestDetails().getValue()).getLocalPart();
			}
			if (trgtAttrName.equalsIgnoreCase("Lockid") || testAttrName.equalsIgnoreCase("Lockid")) {
				return ComparisonResult.EQUAL;
			}
			break;
		case ELEMENT_TAG_NAME:
			String trgtElemName = "";
			String testElemName = "";

			if (comparison.getControlDetails().getValue() != null) {
				trgtElemName = ((String) comparison.getControlDetails().getValue());
			}
			if (comparison.getTestDetails().getValue() != null) {
				testElemName = ((String) comparison.getTestDetails().getValue());
			}
			if (trgtElemName.equalsIgnoreCase("Insert") && testElemName.equalsIgnoreCase("Delete")) {
				return ComparisonResult.EQUAL;
			}
			break;
		case CHILD_NODELIST_LENGTH:
			return ComparisonResult.SIMILAR;
		case ELEMENT_NUM_ATTRIBUTES:
			return ComparisonResult.SIMILAR;
		case CHILD_LOOKUP:
			return ComparisonResult.SIMILAR;

		}
		if (outcome.equals(ComparisonResult.DIFFERENT)) {
			outcome = outcome;
		}
		return outcome;

	}

}
