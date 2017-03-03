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

import org.eclipse.scanning.api.scan.ScanningException;

public interface IPausableDevice<T> extends IRunnableDevice<T> {

	/**
	 * Allowed when the device is in Running state. Will block until the device is in a rest state. 
	 * 
	 * When paused the same thread must call resume() or abort() which has paused or an
	 * IllegalMonitorState Exception will be thrown.
	 */
	void pause() throws ScanningException;
	
	/**
	 * Seek to/from a point number (absolute) in the scan.
	 * @param amount
	 */
	void seek(int stepNumber) throws ScanningException, InterruptedException;
	
	/**
	 * Allowed when the device is in Paused state. Will block until the device is unpaused.
	 * 
	 * When paused the same thread must call resume() or abort() which has paused or an
	 * 
	 * IllegalMonitorState Exception will be thrown.
	 */
	void resume() throws ScanningException;
	

}
