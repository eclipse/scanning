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

import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.FileType;

public class ProcessingModel extends SlicingModel {


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


	public ProcessingModel() {

	}

    public ProcessingModel(String detectorName, String dataFile, String operationsFile, long timeout) {
	super(detectorName,dataFile,timeout);
	this.operationsFile = operationsFile;
    }

	public String getOperationsFile() {
		return operationsFile;
	}

	public void setOperationsFile(String filePath) {
		this.operationsFile = filePath;
	}

	public Object getOperation() {
		return operation;
	}

	public void setOperation(Object operation) {
		this.operation = operation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((operation == null) ? 0 : operation.hashCode());
		result = prime * result + ((operationsFile == null) ? 0 : operationsFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessingModel other = (ProcessingModel) obj;
		if (operation == null) {
			if (other.operation != null)
				return false;
		} else if (!operation.equals(other.operation))
			return false;
		if (operationsFile == null) {
			if (other.operationsFile != null)
				return false;
		} else if (!operationsFile.equals(other.operationsFile))
			return false;
		return true;
	}
}
