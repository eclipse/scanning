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
package org.eclipse.scanning.api.event.queues.beans;

import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * Base class for all atoms/beans which will be handled by the
 * {@link IQueueService}.
 *
 * @author Michael Wharmby
 *
 */
public abstract class Queueable extends StatusBean {

	/**
	 * Version ID for serialization. Should be updated when class changed.
	 */
	private static final long serialVersionUID = 20161017L;

	protected long runTime;
	protected String beamline;
	protected String shortName;
	protected boolean model;

	protected Queueable() {
		super();
		setStatus(Status.NONE);
		setPreviousStatus(Status.NONE);
	}

	public String getBeamline() {
		return beamline;
	}

	public void setBeamline(String beamline) {
		this.beamline = beamline;
	}

	public long getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public boolean isModel() {
		return model;
	}

	public void setModel(boolean model) {
		this.model = model;
	}

	public void merge(Queueable with) {
		super.merge(with);
		this.runTime = with.runTime;
		this.beamline = with.beamline;
		this.shortName = with.shortName;
		this.model = with.model;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((beamline == null) ? 0 : beamline.hashCode());
		result = prime * result + (model ? 1231 : 1237);
		result = prime * result + (int) (runTime ^ (runTime >>> 32));
		result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
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
		Queueable other = (Queueable) obj;
		if (beamline == null) {
			if (other.beamline != null)
				return false;
		} else if (!beamline.equals(other.beamline))
			return false;
		if (model != other.model)
			return false;
		if (runTime != other.runTime)
			return false;
		if (shortName == null) {
			if (other.shortName != null)
				return false;
		} else if (!shortName.equals(other.shortName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String clazzName = this.getClass().getSimpleName();
		if (model) clazzName = clazzName + " (MODEL)";
		return clazzName + " [name=" + name +" (shortname=" + shortName + "), status=" + status
				+ ", message=" + message + ", percentComplete=" + percentComplete
				+ ", previousStatus=" + previousStatus + ", runTime=" + runTime + ", userName="
				+ userName + ", hostName=" + hostName + ", beamline="+ beamline + ", submissionTime="
				+ submissionTime + ", properties=" + getProperties() + ", id=" + getUniqueId() + "]";
	}

}
