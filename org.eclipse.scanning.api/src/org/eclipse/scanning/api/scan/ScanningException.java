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
package org.eclipse.scanning.api.scan;

import org.eclipse.scanning.api.device.IRunnableDevice;

public class ScanningException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2587074494060854407L;
	private IRunnableDevice<?> device;

	public ScanningException() {
		super();
	}

	public ScanningException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ScanningException(String message, Throwable cause) {
		super(message, cause);
    }

	public ScanningException(String message) {
		super(message);
	}
	public ScanningException(IRunnableDevice<?> device, String message) {
		super(message);
		this.device = device;
	}

	public ScanningException(Throwable cause) {
		super(cause);
	}
	public ScanningException(IRunnableDevice<?> device, Throwable cause) {
		super(cause);
		this.device = device;
	}

	/**
	 *  
	 * @return the device, may be null.
	 */
	public IRunnableDevice<?> getDevice() {
		return device;
	}

	public void setDevice(IRunnableDevice<?> device) {
		this.device = device;
	}

}
