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

import org.eclipse.scanning.api.ITimeoutable;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

public class ProcessingModel implements ITimeoutable {

	@FieldDescriptor(file=FileType.EXISTING_FILE)
	private String   dataFile;
	
	/**
	 * The name of the detector whose output we will be processing
	 * 
	 * This is used to figure out which part of the nexus file
	 * to look at when processing.
	 */
	@FieldDescriptor(device=DeviceType.RUNNABLE, hint="The name of the detector whose output we will process.")
	private String detectorName;
	private String name;
	
	@FieldDescriptor(file=FileType.EXISTING_FILE, hint="A reference to any file created in the processing perspective.\n"
			                                           + "The pipeline should be saved to file and the file must be\n"
			                                           + "available to the scanning server.")
	private String operationsFile;
	
	/**
	 * Just for testing, set an operation directly
	 * to be run by the device.
	 */
	@FieldDescriptor(visible=false)
	private Object operation;

	private long timeout = -1;
	
	public ProcessingModel() {
		
	}
	
    public ProcessingModel(String detectorName, String dataFile, String operationsFile, long timeout) {
    	this.detectorName   = detectorName;
       	this.dataFile       = dataFile;
       	this.operationsFile = operationsFile;
       	this.timeout        = timeout;
    }
	
	public String getOperationsFile() {
		return operationsFile;
	}

	public void setOperationsFile(String filePath) {
		this.operationsFile = filePath;
	}

	public String getDataFile() {
		return dataFile;
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}

	public String getDetectorName() {
		return detectorName;
	}

	public void setDetectorName(String datasetPath) {
		this.detectorName = datasetPath;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataFile == null) ? 0 : dataFile.hashCode());
		result = prime * result + ((detectorName == null) ? 0 : detectorName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((operationsFile == null) ? 0 : operationsFile.hashCode());
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
		ProcessingModel other = (ProcessingModel) obj;
		if (dataFile == null) {
			if (other.dataFile != null)
				return false;
		} else if (!dataFile.equals(other.dataFile))
			return false;
		if (detectorName == null) {
			if (other.detectorName != null)
				return false;
		} else if (!detectorName.equals(other.detectorName))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (operationsFile == null) {
			if (other.operationsFile != null)
				return false;
		} else if (!operationsFile.equals(other.operationsFile))
			return false;
		return true;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public Object getOperation() {
		return operation;
	}

	public void setOperation(Object operation) {
		this.operation = operation;
	}
}
