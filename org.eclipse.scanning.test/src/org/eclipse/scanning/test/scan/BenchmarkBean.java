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
package org.eclipse.scanning.test.scan;

import java.io.File;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.ScanBean;

public class BenchmarkBean {
	
	private int size; 
	private long reqTime; 
	private int tries; 
	private boolean silent; 
	private IRunnableDevice<? extends IDetectorModel> detector;
	private String scannableName;
	private String filePath;
	private long maxPointTime = Long.MAX_VALUE;
	private IPublisher<ScanBean> publisher;
	
	public BenchmarkBean() {
		
	}
	
	public BenchmarkBean(int size, long reqTime, int tries, boolean silent,
			IRunnableDevice<? extends IDetectorModel> detector, String scannableName) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.tries = tries;
		this.silent = silent;
		this.detector = detector;
		this.scannableName = scannableName;
	}
	
	public BenchmarkBean(int size, long reqTime, int tries, boolean silent,
			              IRunnableDevice<? extends IDetectorModel> detector) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.tries = tries;
		this.silent = silent;
		this.detector = detector;
		this.scannableName = "benchmark1";
	}
	
	public BenchmarkBean(int size, long reqTime, int tries, IRunnableDevice<? extends IDetectorModel> detector) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.tries = tries;
		this.silent = false;
		this.detector = detector;
		this.scannableName = "benchmark1";
	}

	public BenchmarkBean(int size, long reqTime, long max, int tries, IRunnableDevice<? extends IDetectorModel> detector) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.maxPointTime = max;
		this.tries = tries;
		this.silent = false;
		this.detector = detector;
		this.scannableName = "benchmark1";
	}
	
	public BenchmarkBean(int size, long reqTime, int tries, IRunnableDevice<? extends IDetectorModel> detector, File output) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.tries = tries;
		this.silent = false;
		this.detector = detector;
		this.scannableName = "benchmark1";
		this.filePath = output.getAbsolutePath();
	}

	public BenchmarkBean(int size, long reqTime, long max, int tries, IRunnableDevice<? extends IDetectorModel> detector, File output) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.maxPointTime = max;
		this.tries = tries;
		this.silent = false;
		this.detector = detector;
		this.scannableName = "benchmark1";
		this.filePath = output.getAbsolutePath();
	}
	
	public BenchmarkBean(int size, long reqTime, long max, int tries, IRunnableDevice<? extends IDetectorModel> detector, File output, IPublisher<ScanBean> publisher) {
		super();
		this.size = size;
		this.reqTime = reqTime;
		this.maxPointTime = max;
		this.tries = tries;
		this.silent = false;
		this.detector = detector;
		this.scannableName = "benchmark1";
		this.filePath = output.getAbsolutePath();
		this.publisher = publisher;
	}

	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public long getReqTime() {
		return reqTime;
	}
	public void setReqTime(long reqTime) {
		this.reqTime = reqTime;
	}
	public int getTries() {
		return tries;
	}
	public void setTries(int tries) {
		this.tries = tries;
	}
	public boolean isSilent() {
		return silent;
	}
	public void setSilent(boolean silent) {
		this.silent = silent;
	}
	public IRunnableDevice<? extends IDetectorModel> getDetector() {
		return detector;
	}
	public void setDetector(IRunnableDevice<? extends IDetectorModel> detector) {
		this.detector = detector;
	}
	public String getScannableName() {
		return scannableName;
	}
	public void setScannableName(String scannableName) {
		this.scannableName = scannableName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((detector == null) ? 0 : detector.hashCode());
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + (int) (maxPointTime ^ (maxPointTime >>> 32));
		result = prime * result + (int) (reqTime ^ (reqTime >>> 32));
		result = prime * result + ((scannableName == null) ? 0 : scannableName.hashCode());
		result = prime * result + (silent ? 1231 : 1237);
		result = prime * result + size;
		result = prime * result + tries;
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
		BenchmarkBean other = (BenchmarkBean) obj;
		if (detector == null) {
			if (other.detector != null)
				return false;
		} else if (!detector.equals(other.detector))
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (maxPointTime != other.maxPointTime)
			return false;
		if (reqTime != other.reqTime)
			return false;
		if (scannableName == null) {
			if (other.scannableName != null)
				return false;
		} else if (!scannableName.equals(other.scannableName))
			return false;
		if (silent != other.silent)
			return false;
		if (size != other.size)
			return false;
		if (tries != other.tries)
			return false;
		return true;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public long getMaxPointTime() {
		return maxPointTime;
	}

	public void setMaxPointTime(long maxPointTime) {
		this.maxPointTime = maxPointTime;
	}

	public IPublisher<ScanBean> getPublisher() {
		return publisher;
	}

	public void setPublisher(IPublisher<ScanBean> publisher) {
		this.publisher = publisher;
	}
}
