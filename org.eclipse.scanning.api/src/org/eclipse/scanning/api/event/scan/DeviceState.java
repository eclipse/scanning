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
package org.eclipse.scanning.api.event.scan;

/**
 * The state that the scanning system device may be in, for instance a MalcolmDevice.
 * 
 * <img src="./doc/device_state.png" />
 * 
 * @author Matthew Gerring
 *
 */
public enum DeviceState {

	RESETTING,IDLE,EDITING,EDITABLE,SAVING,REVERTING,READY,CONFIGURING,RUNNING,POSTRUN,PAUSED,SEEKING,ABORTING,ABORTED,FAULT,DISABLING,DISABLED,OFFLINE;

	private String stringVal;
	
	private DeviceState() {
		stringVal = name().substring(0, 1) + name().substring(1).toLowerCase();
		if (name().endsWith("RUN")) {
			stringVal = stringVal.substring(0, name().indexOf("RUN")) + "Run"; 
		}
	}

	public String toString() {
		return stringVal;
	}
	
	/**
	 * The run method may be called
	 * @return
	 */
	public boolean isRunnable() {
		return this==READY;
	}
	
	public boolean isRunning() {
		return this==RUNNING || this==PAUSED || this==SEEKING || this==POSTRUN;
	}

	/**
	 * Before run means that the state is before running and not in error.
	 * @return
	 */
	public boolean isBeforeRun() {
		return this==READY || this==IDLE || this == CONFIGURING;
	}

	public boolean isRest() {
		return this==FAULT || this==IDLE || this==CONFIGURING || this==READY || this==ABORTED || this==DISABLED;
	}

	public boolean isAbortable() {
		return this==RUNNING || this==CONFIGURING || this==PAUSED || this==SEEKING || this==READY || this==POSTRUN;
	}
	
	public boolean isResetable() {
		return this==FAULT || this==ABORTED || this==DISABLED || this==READY;
	}

	public boolean isTransient() {
		return this==RUNNING || this==CONFIGURING || this==ABORTING || this==SEEKING || this==DISABLING || this==POSTRUN;
	}

	public boolean isRestState() {
		return this==IDLE || this==READY || this==FAULT || this==ABORTED || this==DISABLED;
	}
	
	
}
