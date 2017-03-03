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
package org.eclipse.scanning.api.malcolm;

import org.eclipse.scanning.api.scan.ScanningException;

public class MalcolmDeviceException extends ScanningException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2394321108005036591L;

	private final IMalcolmDevice<?> device;

	public MalcolmDeviceException(String message) {
		this(null, message);
	}
	public MalcolmDeviceException(IMalcolmDevice<?> device) {
		this(device, (String)null);
	}
	public MalcolmDeviceException(IMalcolmDevice<?> device, String message) {
		this(device, message, null);
	}
	public MalcolmDeviceException(IMalcolmDevice<?> device, String message, Throwable original) {
		super(message, original);
		this.device = device;
	}

	public MalcolmDeviceException(IMalcolmDevice<?> device, Exception e) {
		super(e);
		this.device = device;
	}


	public IMalcolmDevice<?> getDevice() {
		return device;
	}

}
