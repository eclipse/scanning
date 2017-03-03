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

import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.status.Status;

/**
 * A request to acquire data from a particular detector
 * 
 * @author Matthew Dickie
 */
public class AcquireRequest extends IdBean {
	
	private static final long serialVersionUID = 8640329010699426771L;

	private String detectorName;
	
	private Object detectorModel;
	
	private String filePath;
	
	private Status status = Status.NONE;
	
	private String message;
	
	public AcquireRequest() {
		// do nothing
	}
	
	public AcquireRequest(String filePath) {
		this.filePath = filePath;
	}
	
	public AcquireRequest(String filePath, String detectorName, Object detectorModel) {
		this(filePath);
		this.detectorName = detectorName;
		this.detectorModel = detectorModel;
	}

	public String getDetectorName() {
		return detectorName;
	}

	public void setDetectorName(String detectorName) {
		this.detectorName = detectorName;
	}

	public Object getDetectorModel() {
		return detectorModel;
	}

	public void setDetectorModel(Object detectorModel) {
		this.detectorModel = detectorModel;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((detectorModel == null) ? 0 : detectorModel.hashCode());
		result = prime * result + ((detectorName == null) ? 0 : detectorName.hashCode());
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		AcquireRequest other = (AcquireRequest) obj;
		if (detectorModel == null) {
			if (other.detectorModel != null)
				return false;
		} else if (!detectorModel.equals(other.detectorModel))
			return false;
		if (detectorName == null) {
			if (other.detectorName != null)
				return false;
		} else if (!detectorName.equals(other.detectorName))
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
		if (status != other.status)
			return false;
		return true;
	}

}
