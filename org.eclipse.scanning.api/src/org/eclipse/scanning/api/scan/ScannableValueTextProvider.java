/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mark Booth - initial API and implementation and/or initial documentation
 *******************************************************************************/
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
