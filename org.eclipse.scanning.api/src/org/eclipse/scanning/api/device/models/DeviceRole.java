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
package org.eclipse.scanning.api.device.models;

/**
 * 
 * Each runnable device has a role to be used when building
 * UI or constucting scans or for validation. For instance 
 * and when the user interface provides drop downs of devices 
 * it will inspect role to see if a given device is applicable.
 * 
 * @author Matthew Gerring
 *
 */
public enum DeviceRole {
	
    /**
     * For instance in memory devices which are always
     * created new, like the AcquisitionDevice
     * Virtual devices can never be created 
     */
	VIRTUAL, 
	
	/**
	 * The device is backed by a malcolm connection 
	 */
	MALCOLM,
	
	/**
	 * The device is either backed by a GDA hardware connection
	 * through EPICS or DAServer or is an in memory device which
	 * should be treated like a hardware one, for instance the MandelbrotDetector.
	 * The device writes its data at each point in the GDA software scan 
	 */
	HARDWARE,
	
	/**
	 * This device is for processing the values of other devices. For instance
	 * the same class could have multiple instances with different models to 
	 * do different processing. 
	 */
	PROCESSING;
	
	public boolean isVirtual() {
		return this==VIRTUAL;
	}
	
	public boolean isMalcolm() {
		return this==MALCOLM;
	}
	
	public boolean isDetector() {
		return this==MALCOLM || this==HARDWARE;
	}

	public boolean isProcessing() {
		return this==PROCESSING;
	}
}
