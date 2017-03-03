/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.api;

/**
 * 
 * For devices that are monitored they may be monitored per scan or per point.
 * In the former case their values will be read and written to the scan file
 * once at the configure stage of the scan. For the per point type, the monitor
 * will run with the scans IPositioner and read/write value during the same 
 * task which writes the motors.
 * <p>
 * All types of monitor are added to the annotation pool and will have annotations
 * processed with the scan.
 * 
 * @author Matthew Gerring
 *
 */
public enum MonitorRole {
	
	/**
	 * This device should not be used as a monitor and will 
	 * throw an exception if it is.
	 */
	NONE,

	/**
	 * Write device value once at scan start during the configure method.
	 */
	PER_SCAN, 
	
	/**
	 * Write a device value at every point.
	 */
	PER_POINT;
	
	/**
	 * 
	 * @return the label to be used in the user interface.
	 */
	public String getLabel() {
		if (this==NONE) {
			return "Do not monitor";
		} else if (this==PER_SCAN) {
			return "At scan start";
		} else if (this==PER_POINT) {
			return "Every point";
		}
		throw new IllegalArgumentException("Unexcepted enum value of "+getClass().getSimpleName());
	}
}
