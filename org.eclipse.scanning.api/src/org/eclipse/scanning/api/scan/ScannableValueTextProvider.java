package org.eclipse.scanning.api.scan;

/**
 * Provide an interface to allow Scannables to return a Value which is nicely formatted for the user
 * interface. Usually toString() is inappropriate as it includes decorations such as the class name and
 * characters to denote that the values are containing in an array or map etc.
 */
public interface ScannableValueTextProvider {
	/**
	 * @return a representation of the scannable value which is suitable for presenting in a GUI.
	 */
	String getText();
}
