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
package org.eclipse.scanning.api.scan.models;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanInformation;

/**
 * A Model describing a scan to be performed.
 */
public class ScanModel {

	/**
	 * If you want the scan to attempt to write to a given
	 * path, set this field. If it is set the scan will 
	 * attempt to use the NexusBuilderFactory and register all the
	 * devices with it.
	 * 
	 * TODO Should we never allow this to be set? Would it allow
	 * users to write data anywhere?
	 * 
	 */
	private String filePath;
	
	/**
	 * Normally this is a generator for the scan points
	 * of the scan. IPointGenerator implements Iterable
	 */
	private Iterable<IPosition> positionIterable;
	
	/**
	 * This is the set of detectors which should be collected
	 * and (if they are IReadableDetector) read out during the 
	 * scan.
	 */
	private List<IRunnableDevice<?>> detectors;
	
	/**
	 * The bean which was submitted. May be null but if it is not,
	 * all points are published using this bean.
	 */
	private ScanBean bean;
	
	/**
	 * A set of scannables may optionally be 'readout' during
	 * the scan without being told a value for their location.
	 * They have setPosition(null, IPosition) called and should 
	 * ensure that if their value is null, they do not move but
	 * still readout position
	 */
	private List<IScannable<?>> monitors;
	
	/**
	 * Scan metadata that is not produced by a particular device, e.g.
	 * scan command, chemical formula etc., grouped by type.
	 */
	private List<ScanMetadata> scanMetadata;

	/**
	 * A list of objects which participant in the scan by having
	 * annotated methods which the scan should call at different points.
	 */
	private List<?> annotationParticipants;
	
	private ScanInformation scanInformation;
	
	public ScanModel() {
		this(null);
	}
	
	public ScanModel(Iterable<IPosition> positionIterator, IRunnableDevice<?>... detectors) {
		this.positionIterable = positionIterator;
		if (detectors!=null && detectors.length>0) this.detectors = Arrays.asList(detectors);
	}
	public ScanModel(Iterable<IPosition> positionIterator, File file) {
		this.positionIterable = positionIterator;
		this.filePath = file.getAbsolutePath();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bean == null) ? 0 : bean.hashCode());
		result = prime * result
				+ ((detectors == null) ? 0 : detectors.hashCode());
		result = prime * result
				+ ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result
				+ ((monitors == null) ? 0 : monitors.hashCode());
		result = prime
				* result
				+ ((positionIterable == null) ? 0 : positionIterable.hashCode());
		result = prime * result
				+ ((scanMetadata == null) ? 0 : scanMetadata.hashCode());
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
		ScanModel other = (ScanModel) obj;
		if (bean == null) {
			if (other.bean != null)
				return false;
		} else if (!bean.equals(other.bean))
			return false;
		if (detectors == null) {
			if (other.detectors != null)
				return false;
		} else if (!detectors.equals(other.detectors))
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (monitors == null) {
			if (other.monitors != null)
				return false;
		} else if (!monitors.equals(other.monitors))
			return false;
		if (scanMetadata == null) {
			if (other.scanMetadata != null)
				return false;
		} else if (!scanMetadata.equals(other.scanMetadata))
			return false;
		if (positionIterable == null) {
			if (other.positionIterable != null)
				return false;
		} else if (!positionIterable.equals(other.positionIterable))
			return false;
		return true;
	}
	public ScanBean getBean() {
		return bean;
	}
	public void setBean(ScanBean bean) {
		this.bean = bean;
	}


	public Iterable<IPosition> getPositionIterable() {
		if (positionIterable == null) {
			return Collections.emptyList();
		}
		
		return positionIterable;
	}

	public void setPositionIterable(Iterable<IPosition> positionIterator) {
		this.positionIterable = positionIterator;
	}

	public List<IRunnableDevice<?>> getDetectors() {
		if (detectors == null) {
			return Collections.emptyList();
		}
		return detectors;
	}

	public void setDetectors(List<IRunnableDevice<?>> ds) {
		this.detectors = ds;
	}

	public void setDetectors(IRunnableDevice<?>... detectors) {
		this.detectors = Arrays.asList(detectors);
	}
	
	public List<IScannable<?>> getMonitors() {
		if (monitors == null) {
			return Collections.emptyList();
		}
		return monitors;
	}
	
	public void setMonitors(List<IScannable<?>> monitors) {
		this.monitors = monitors;
	}
	
	public void setMonitors(IScannable<?>... monitors) {
		this.monitors = new ArrayList<>(Arrays.asList(monitors));
		for (Iterator<IScannable<?>> iterator = this.monitors.iterator(); iterator.hasNext();) {
			if (iterator.next()==null) iterator.remove();
		}
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public List<ScanMetadata> getScanMetadata() {
		if (scanMetadata == null) {
			return Collections.emptyList();
		}
		return scanMetadata;
	}
	
	public void setScanMetadata(List<ScanMetadata> scanMetadata) {
		this.scanMetadata = scanMetadata;
	}
	
	public void addScanMetadata(ScanMetadata scanMetadata) {
		if (this.scanMetadata == null) {
			this.scanMetadata = new ArrayList<>();
		}
		this.scanMetadata.add(scanMetadata);
	}

	public List<?> getAnnotationParticipants() {
		return annotationParticipants;
	}

	public void setAnnotationParticipants(List<?> annotationParticipants) {
		this.annotationParticipants = annotationParticipants;
	}
	
	public ScanInformation getScanInformation() {
		return scanInformation;
	}

	public void setScanInformation(ScanInformation scanInformation) {
		this.scanInformation = scanInformation;
	}
	
}
