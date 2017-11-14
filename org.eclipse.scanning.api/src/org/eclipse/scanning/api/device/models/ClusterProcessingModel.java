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

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

public class ClusterProcessingModel implements INameable, IReflectedModel {

	private String name;

	@FieldDescriptor(device=DeviceType.RUNNABLE, hint="The name of the detector whose output we will process")
	private String detectorName;

	@FieldDescriptor(file=FileType.EXISTING_FILE, hint="The full path of the processing file")
	private String processingFilePath;

	// Note: this field is required by GDA when configuring an existing processing bean so that we
	// know which malcolm device (if any) to use in the acquire scan. It is not required for processing on the cluster.
	@FieldDescriptor(visible=false)
	private String malcolmDeviceName;

	@FieldDescriptor(visible=false)
	private String xmx = "1024m";

	@FieldDescriptor(visible=false)
	private int timeOut = 60000;

	@FieldDescriptor(visible=false)
	private int numberOfCores = 1;

	@FieldDescriptor(visible=false)
	private boolean monitorForOverwrite = false;


	public ClusterProcessingModel() {

	}
	public ClusterProcessingModel(String name, String detectorName, String processingFilePath) {
		this.name               = name;
		this.detectorName       = detectorName;
		this.processingFilePath = processingFilePath;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public String getDetectorName() {
		return detectorName;
	}

	public void setDetectorName(String detectorName) {
		this.detectorName = detectorName;
	}

	public String getProcessingFilePath() {
		return processingFilePath;
	}

	public void setProcessingFilePath(String processingFileName) {
		this.processingFilePath = processingFileName;
	}

	public String getMalcolmDeviceName() {
		return malcolmDeviceName;
	}

	public void setMalcolmDeviceName(String malcolmDeviceName) {
		this.malcolmDeviceName = malcolmDeviceName;
	}

	public String getXmx() {
		return xmx;
	}
	public void setXmx(String xmx) {
		this.xmx = xmx;
	}
	public int getTimeOut() {
		return timeOut;
	}
	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}

	public int getNumberOfCores() {
		return numberOfCores;
	}

	public void setNumberOfCores(int numberOfCores) {
		this.numberOfCores = numberOfCores;
	}

	public boolean isMonitorForOverwrite() {
		return monitorForOverwrite;
	}

	public void setMonitorForOverwrite(boolean monitorForOverwrite) {
		this.monitorForOverwrite = monitorForOverwrite;
	}

	@Override
	public String toString() {
		return "ClusterProcessingModel [name=" + name + ", detectorName=" + detectorName + ", processingFilePath="
				+ processingFilePath + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((detectorName == null) ? 0 : detectorName.hashCode());
		result = prime * result + ((malcolmDeviceName == null) ? 0 : malcolmDeviceName.hashCode());
		result = prime * result + (monitorForOverwrite ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + numberOfCores;
		result = prime * result + ((processingFilePath == null) ? 0 : processingFilePath.hashCode());
		result = prime * result + timeOut;
		result = prime * result + ((xmx == null) ? 0 : xmx.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("squid:S3776")
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClusterProcessingModel other = (ClusterProcessingModel) obj;
		if (detectorName == null) {
			if (other.detectorName != null)
				return false;
		} else if (!detectorName.equals(other.detectorName))
			return false;
		if (malcolmDeviceName == null) {
			if (other.malcolmDeviceName != null)
				return false;
		} else if (!malcolmDeviceName.equals(other.malcolmDeviceName))
			return false;
		if (monitorForOverwrite != other.monitorForOverwrite)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (numberOfCores != other.numberOfCores)
			return false;
		if (processingFilePath == null) {
			if (other.processingFilePath != null)
				return false;
		} else if (!processingFilePath.equals(other.processingFilePath))
			return false;
		if (timeOut != other.timeOut)
			return false;
		if (xmx == null) {
			if (other.xmx != null)
				return false;
		} else if (!xmx.equals(other.xmx))
			return false;
		return true;
	}



}
