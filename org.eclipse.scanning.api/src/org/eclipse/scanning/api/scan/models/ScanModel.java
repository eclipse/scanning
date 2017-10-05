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
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Model describing a scan to be performed.
 */
public class ScanModel {
	private static Logger logger = LoggerFactory.getLogger(ScanModel.class);

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
	 * A list of scannables that may be set to a position
	 * during the scan. They have {@code setPostition(pos, IPosition)}
	 * called, where {@code pos} is non {@code null}, and should move
	 * to this position and readout their new position.
	 * Note that setting this field is optional, if {@code null} the
	 * scan scannables will be retrieved from by {@link IScannableDeviceService}
	 * by calling {@link IScannableDeviceService#getScannable(String)} for
	 * each scannable name as returned by calling
	 * {@code getPositionIterable().iterator().next().getNames()}.
	 */
	private List<IScannable<?>> scannables;

	/**
	 * Sets of scannables may optionally be 'readout' during
	 * the scan without being told a value for their location.
	 * They have {@code setPosition(null, IPosition)} called and should
	 * ensure that if their value is {@code null}, they do not move but
	 * still readout position
	 */
	private List<IScannable<?>> monitorsPerPoint;
	private List<IScannable<?>> monitorsPerScan;

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
				+ ((monitorsPerPoint == null) ? 0 : monitorsPerPoint.hashCode());
		result = prime * result
				+ ((monitorsPerScan == null) ? 0 : monitorsPerScan.hashCode());
		result = prime
				* result
				+ ((positionIterable == null) ? 0 : positionIterable.hashCode());
		result = prime * result
				+ ((scanMetadata == null) ? 0 : scanMetadata.hashCode());
		return result;
	}

	@SuppressWarnings("squid:S3776")
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
		if (monitorsPerPoint == null) {
			if (other.monitorsPerPoint != null)
				return false;
		} else if (!monitorsPerPoint.equals(other.monitorsPerPoint))
			return false;
		if (monitorsPerScan == null) {
			if (other.monitorsPerScan != null)
				return false;
		} else if (!monitorsPerScan.equals(other.monitorsPerScan))
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

	@SuppressWarnings("squid:S1452")
	public List<IScannable<?>> getScannables() {
		return scannables;
	}

	public void setScannables(List<IScannable<?>> scannables) {
		this.scannables = scannables;
	}

	@SuppressWarnings("squid:S1452")
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

	@SuppressWarnings("squid:S1452")
	public List<IScannable<?>> getMonitorsPerPoint() {
		if (monitorsPerPoint == null) {
			return Collections.emptyList();
		}
		return monitorsPerPoint;
	}

	public void setMonitorsPerPoint(List<IScannable<?>> monitors) {
		logger.info("setMonitorsPerPoint({}) was {} ({})", monitors, this.monitorsPerPoint, this);
		this.monitorsPerPoint = monitors;
	}

	public void setMonitorsPerPoint(IScannable<?>... monitors) {
		logger.info("setMonitorsPerPoint({}) was {} ({})", this, monitors, this.monitorsPerPoint, this);
		this.monitorsPerPoint = new ArrayList<>(Arrays.asList(monitors));
		for (Iterator<IScannable<?>> iterator = this.monitorsPerPoint.iterator(); iterator.hasNext();) {
			if (iterator.next()==null) iterator.remove();
		}
	}

	@SuppressWarnings("squid:S1452")
	public List<IScannable<?>> getMonitorsPerScan() {
		if (monitorsPerScan == null) {
			return Collections.emptyList();
		}
		return monitorsPerScan;
	}

	public void setMonitorsPerScan(List<IScannable<?>> monitors) {
		logger.info("setMonitorsPerScan({}) was {} ({})", monitors, this.monitorsPerScan, this);
		this.monitorsPerScan = monitors;
	}

	public void setMonitorsPerScan(IScannable<?>... monitors) {
		logger.info("setMonitorsPerScan({}) was {} ({})", monitors, this.monitorsPerScan, this);
		this.monitorsPerScan = new ArrayList<>(Arrays.asList(monitors));
		for (Iterator<IScannable<?>> iterator = this.monitorsPerScan.iterator(); iterator.hasNext();) {
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

	@SuppressWarnings("squid:S1452")
	public List<?> getAnnotationParticipants() {
		if (annotationParticipants == null) {
			return Collections.emptyList();
		}
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

	@Override
	public String toString() {
		return "ScanModel [filePath=" + filePath + ", positionIterable=" + positionIterable + ", detectors=" + detectors
				+ ", bean=" + bean + ", scannables=" + scannables + ", monitorsPerPoint=" + monitorsPerPoint
				+ ", monitorsPerScan=" + monitorsPerScan + ", scanMetadata=" + scanMetadata +
				", annotationParticipants=" + annotationParticipants + ", scanInformation=" + scanInformation + "]";
	}

}
