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
 * A nameable is any device with a name.
 * 
 * @author Matthew Gerring
 *
 */
public interface INameable {

	/**
	 * Name of the scannable
	 * @return
	 */
	public String getName();
	
	/**
	 * Set the name of the device.
	 * @param name
	 */
	public void setName(String name);

}
