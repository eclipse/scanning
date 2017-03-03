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
package org.eclipse.scanning.api.malcolm.event;

import java.util.Arrays;

import org.eclipse.scanning.api.event.scan.DeviceState;


/**
 * This bean is used to disseminate messages about what has happened
 * to the scan while it is being written.
 * 
 * Do not extend this class to allow arbitrary information to be sent.
 * The event encapsulated by this bean should be sending just the information
 * defined here, metadata that cannot circumvent the nexus file. 
 * 
 * For instance adding a dynamic set of information, a map perhaps, would
 * allow information which should be saved in the Nexus file to circumvent
 * the file and be set in the event. It was decided in various meetings
 * that doing this could mean that some data is not recorded as it should be
 * in nexus.
 * 
 * @author Matthew Gerring
 *
 */
public final class MalcolmEventBean {
	

	// General Information
	private String  deviceName;
	private String  beamline;
	private double  percentComplete;
	private String  message;
	
	// State information
	private DeviceState   deviceState;
	private DeviceState   previousState;
	
	// Dataset information
	private String  filePath;
	private String  datasetPath;
    private int[]   oldShape;
    private int[]   newShape;
		
	public MalcolmEventBean() {

	}
	
	public MalcolmEventBean(DeviceState state) {
		this.deviceState     = state;
	}

	public MalcolmEventBean(DeviceState state, String message) {
		this(state);
		this.message = message;
	}
	
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getDatasetPath() {
		return datasetPath;
	}
	public void setDatasetPath(String datasetPath) {
		this.datasetPath = datasetPath;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beamline == null) ? 0 : beamline.hashCode());
		result = prime * result + ((datasetPath == null) ? 0 : datasetPath.hashCode());
		result = prime * result + ((deviceName == null) ? 0 : deviceName.hashCode());
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + Arrays.hashCode(newShape);
		result = prime * result + Arrays.hashCode(oldShape);
		long temp;
		temp = Double.doubleToLongBits(percentComplete);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((previousState == null) ? 0 : previousState.hashCode());
		result = prime * result + ((deviceState == null) ? 0 : deviceState.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MalcolmEventBean other = (MalcolmEventBean) obj;
		if (beamline == null) {
			if (other.beamline != null)
				return false;
		} else if (!beamline.equals(other.beamline))
			return false;
		if (datasetPath == null) {
			if (other.datasetPath != null)
				return false;
		} else if (!datasetPath.equals(other.datasetPath))
			return false;
		if (deviceName == null) {
			if (other.deviceName != null)
				return false;
		} else if (!deviceName.equals(other.deviceName))
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (!Arrays.equals(newShape, other.newShape))
			return false;
		if (!Arrays.equals(oldShape, other.oldShape))
			return false;
		if (Double.doubleToLongBits(percentComplete) != Double.doubleToLongBits(other.percentComplete))
			return false;
		if (previousState != other.previousState)
			return false;
		if (deviceState != other.deviceState)
			return false;
		return true;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getBeamline() {
		return beamline;
	}

	public void setBeamline(String beanline) {
		this.beamline = beanline;
	}

	public double getPercentComplete() {
		return percentComplete;
	}

	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}

	public DeviceState getDeviceState() {
		return deviceState;
	}

	public void setDeviceState(DeviceState state) {
		this.deviceState = state;
	}

	public int[] getOldShape() {
		return oldShape;
	}

	public void setOldShape(int[] oldShape) {
		this.oldShape = oldShape;
	}

	public int[] getNewShape() {
		return newShape;
	}

	public void setNewShape(int[] newShape) {
		this.newShape = newShape;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "MalcolmEventBean [filePath=" + filePath + ", deviceName="
				+ deviceName + ", beanline=" + beamline + ", percentComplete="
				+ percentComplete + ", message=" + message + ", state=" + deviceState
				+ ", datasetPath=" + datasetPath + ", oldShape="
				+ Arrays.toString(oldShape) + ", newShape="
				+ Arrays.toString(newShape) + "]";
	}

	public DeviceState getPreviousState() {
		return previousState;
	}

	public void setPreviousState(DeviceState previousState) {
		this.previousState = previousState;
	}

	public boolean isScanStart() {
		return getDeviceState()==DeviceState.RUNNING && getDeviceState()!=getPreviousState();
	}
	
	public boolean isScanEnd() {
		return getDeviceState()!=getPreviousState() && getPreviousState()== DeviceState.RUNNING;
	}
}
