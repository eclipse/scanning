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
package org.eclipse.scanning.api.scan.event;

import java.util.EventObject;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;

public class RunEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8344532962994793917L;

	private final IPosition position;
	
	private final DeviceState deviceState;
	private DeviceState oldState;

	public RunEvent(IRunnableDevice<?> device, IPosition position, DeviceState deviceState) {
		super(device);
		this.position = position;
		this.deviceState = deviceState;
	}

	public IRunnableDevice<?> getDevice() {
		return (IRunnableDevice<?>)getSource();
	}
	
	public IPosition getPosition() {
		return position;
	}
	
	public DeviceState getDeviceState() {
		return deviceState;
	}

	public DeviceState getOldState() {
		return oldState;
	}

	public void setOldState(DeviceState oldState) {
		this.oldState = oldState;
	}
	
}
