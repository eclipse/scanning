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

import java.util.Arrays;

import org.eclipse.scanning.api.event.queues.IQueueService;

/**
 * MonitorAtom is a type of {@link QueueAtom} which may be processed within an 
 * active-queue of an {@link IQueueService}. It contains name of a monitor the
 * current value of which will be on execution as a dataset within a file 
 * (located at filePath).
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
	private int[] dataShape;
	private String filePath;
	private String dataset;
	
	/**
	 * No arg constructor for JSON
	 */
	public MonitorAtom() {
		super();
	}
	
	/**
	 * Constructor with arguments required to fully configure this atom to 
	 * read from a monitor providing data with shape 1.
	 * 
	 * @param monShrtNm String short name used within the QueueBeanFactory
	 * @param dev String name of monitor
	 */
	public MonitorAtom(String monShrtNm, String dev) {
		this(monShrtNm, false, dev, new int[]{1});
	}
	
	/**
	 * Constructor with arguments required to configure a model or real 
	 * instance of this atom. Resulting atom will read from a monitor 
	 * providing data with shape 1.
	 * 
	 * @param monShrtNm String short name used within the 
	 *        {@link IQueueBeanFactory}
	 * @param model boolean flag indicating whether this is a model
	 * @param dev String name of monitor
	 */
	public MonitorAtom(String monShrtNm, boolean model, String dev) {
		this(monShrtNm, model, dev, new int[]{1});
	}
	
	/**
	 * Constructor with arguments required to configure a model or real 
	 * instance of this atom. 
	 * 
	 * @param monShrtNm String short name used within the 
	 *        {@link IQueueBeanFactory}
	 * @param model boolean flag indicating whether this is a model
	 * @param dev String name of monitor
	 * @param dataShape int[] describing shape of expected data (e.g. pixels)
	 */
	public MonitorAtom(String monShrtNm, boolean model, String dev, int[] dataShape) {
		super();
		setShortName(monShrtNm);
		setModel(model);
		monitor = dev;
		this.dataShape = dataShape;
		
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

	public int[] getDataShape() {
		return dataShape;
	}

	public void setDataShape(int[] dataShape) {
		this.dataShape = dataShape;
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
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(dataShape);
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
		if (!Arrays.equals(dataShape, other.dataShape))
			return false;
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
	
	@Override
	public String toString() {
		String clazzName = this.getClass().getSimpleName();
		return clazzName + " [name=" + name + "(shortName=" + shortName + "), monitor=" + monitor
				+ ", filePath=" + filePath + ", dataset=" + dataset + ", status=" + status
				+ ", message=" + message + ", percentComplete=" + percentComplete + ", previousStatus=" 
				+ previousStatus + ", runTime=" + runTime + ", userName=" + userName + ", hostName=" 
				+ hostName + ", beamline="+ beamline + ", submissionTime=" + submissionTime 
				+ ", properties=" + getProperties() + ", id=" + getUniqueId() + "]";
	}
	
}
