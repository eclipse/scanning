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
 * Interface for any device with a level. Level is used for instance in scanning to 
 * define the order that devices are moved to in the scan.
 * 
 * @author Matthew Gerring
 *
 */
public interface ILevel extends INameable {

	final int MAXIMUM = 100;

	/**
	 * Used for ordering the operations of Scannables during scans
	 * 
	 * @param level
	 */
	public void setLevel(int level);

	/**
	 * get the operation level of this scannable.
	 * 
	 * @return int - the level
	 */
	public int getLevel();


}
