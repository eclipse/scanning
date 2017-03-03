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
package org.eclipse.scanning.api.scan;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.scanning.api.device.models.ScanMode;

/**
 * 
 * Holds state of whole scan. May be used in annotated methods like &#64;ScanStart
 * to provide information about whole scan. Should not be used to hold transient
 * state during the scan. One should be created per run.
 * 
 * @author Matthew Gerring
 *
 */
public class ScanInformation {
	
	private String filePath;
	private int                size;
	private int                rank;
	private Collection<String> scannableNames;
	private transient ScanEstimator  estimator;
	private int[] shape;
	private ScanMode scanMode;
	private long estimatedScanTime;
	
	public ScanInformation() {
		
	}
	
	/**
	 * Setup the scan information from a ScanEstimator
	 * NOTE the getShape() method is then delegated to the ScanEstimator
	 * for speed reasons. It will not be calculated until you call
	 * getShape() for the first time.
	 *  
	 * @param prov
	 */
	public ScanInformation(ScanEstimator prov) {
		this.estimator = prov;
		setSize(estimator.getSize());
		setRank(estimator.getRank());
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + rank;
		result = prime * result + ((scannableNames == null) ? 0 : scannableNames.hashCode());
		result = prime * result + size;
		result = prime * result + ((scanMode == null) ? 0 : scanMode.hashCode());
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
		ScanInformation other = (ScanInformation) obj;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (rank != other.rank)
			return false;
		if (scannableNames == null) {
			if (other.scannableNames != null)
				return false;
		} else if (!scannableNames.equals(other.scannableNames))
			return false;
		if (size != other.size)
			return false;
		if (scanMode == null) {
			if (other.scanMode != null)
				return false;
		} else if (!scanMode.equals(other.scanMode))
			return false;
		return true;
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public Collection<String> getScannableNames() {
		return scannableNames;
	}
	public void setScannableNames(Collection<String> scannableNames) {
		this.scannableNames = scannableNames;
	}

	public int[] getShape() {
		if (shape!=null) return shape;
		// We calculate shape on the fly because it can be expensive to estimate.
		shape = estimator!=null ? estimator.getShape() : null;
		return shape;
	}
	
	public long getEstimatedScanTime() {
		if (estimatedScanTime > 0) return estimatedScanTime;
		estimatedScanTime = estimator != null ? estimator.getEstimatedScanTime() : 0;
		return estimatedScanTime;
	}

	public void setShape(int[] shape) {
		this.shape = shape;
	}

	@Override
	public String toString() {
		return "ScanInformation [filePath=" + filePath + ", size=" + size + ", rank=" + rank
				+ ", scannableNames=" + scannableNames + ", shape=" + Arrays.toString(shape)
				+ ", scanMode=" + scanMode + "]";
	}

}
