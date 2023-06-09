package com.acuver.cdt.xml;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

import com.acuver.cdt.util.CDTConstants;

public class CDTXmlDifferenceEvaluator implements DifferenceEvaluator {

	public String primaryKeyName;

	@Override
	public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {

		switch (comparison.getType()) {
		case ATTR_VALUE:
			Attr attr = (Attr) comparison.getControlDetails().getTarget();

			if (attr.getName().equalsIgnoreCase(primaryKeyName) || attr.getName().equalsIgnoreCase(CDTConstants.LockID)
					|| attr.getName().equalsIgnoreCase(CDTConstants.ID)) {
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
			if (trgtAttrName.equalsIgnoreCase(CDTConstants.LockID)
					|| testAttrName.equalsIgnoreCase(CDTConstants.LockID)) {
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
			if (trgtElemName.equalsIgnoreCase(CDTConstants.INSERT)
					&& testElemName.equalsIgnoreCase(CDTConstants.DELETE)) {
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
		return outcome;

	}

	// Get Primary Key Name
	public String getPrimaryKeyName() {
		return primaryKeyName;
	}

	// Set Primary Key Name
	public void setPrimaryKeyName(String primaryKeyName) {
		this.primaryKeyName = primaryKeyName;
	}

}