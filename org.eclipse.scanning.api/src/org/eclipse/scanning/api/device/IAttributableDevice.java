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
package org.eclipse.scanning.api.device;

import java.util.List;

import org.eclipse.scanning.api.scan.ScanningException;

/**
 * An interface for devices that have attributes that can be got and set.
 * 
 * @author Matt Taylor
 * 
 */
public interface IAttributableDevice {

	/**
	 * Gets the an attribute on the device
	 */
	public Object getAttribute(String attribute) throws ScanningException;
	
	/**
	 * Gets a list of all attributes on the device
	 */
	public <A> List<A> getAllAttributes() throws ScanningException;
	
	/**
	 * Gets the value of an attribute on the device
	 */
	public <A> A getAttributeValue(String attribute) throws ScanningException;

}
