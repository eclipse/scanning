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

/**
 * MonitorAtom is a type of {@link QueueAtom} which may be processed within an 
 * active-queue of an {@link IQueueService}. It contains name of a monitor 
 * which the current value needs to be recorded at a particular point in an 
 * experiment.
 * 
 * @author Michael Wharmby
 *
 */
public class MonitorAtom extends QueueAtom {
	
	/**
	 * Version ID for serialization. Should be updated when class changed. 
	 */
	private static final long serialVersionUID = 20161021L;
	
	private String monitor;
	private String filePath;
	private String dataset;
	
	/**
	 * No arg constructor for JSON
	 */
	public MonitorAtom() {
		super();
	}
	
	/**
	 * Constructor with arguments required to fully configure this atom
	 * @param monName - name for atom
	 * @param dev - name of monitor
	 */
	public MonitorAtom(String monName, String dev, long time) {
		super();
		setName(monName);
		monitor = dev;
		runTime = time;
	}

	@Override
	public long getRunTime() {
		return runTime;
	}

	@Override
	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}

	/**
	 * Return the monitor which will be polled by this atom 
	 * @return the monitor to be polled
	 */
	public String getMonitor() {
		return monitor;
	}

	/**
	 * Set the monitor which will be polled by this atom
	 * @param monitor - new monitor to be polled
	 */
	public void setMonitor(String monitor) {
		this.monitor = monitor;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dataset == null) ? 0 : dataset.hashCode());
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + ((monitor == null) ? 0 : monitor.hashCode());
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
		MonitorAtom other = (MonitorAtom) obj;
		if (dataset == null) {
			if (other.dataset != null)
				return false;
		} else if (!dataset.equals(other.dataset))
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (monitor == null) {
			if (other.monitor != null)
				return false;
		} else if (!monitor.equals(other.monitor))
			return false;
		return true;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getDataset() {
		return dataset;
	}

	public void setDataset(String dataset) {
		this.dataset = dataset;
	}

	@Override
	public String toString() {
		return "MonitorAtom [monitor=" + monitor + ", filePath=" + filePath + ", dataset=" + dataset + "]";
	}
	
}
