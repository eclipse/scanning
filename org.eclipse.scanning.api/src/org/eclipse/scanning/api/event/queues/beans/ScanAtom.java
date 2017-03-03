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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.models.IScanPathModel;

/**
 * ScanAtom is a type of {@link QueueAtom} which may be processed within an 
 * active-queue of an {@link IQueueService}. It contains all the configuration 
 * necessary to create a {@link ScanBean} which is submitted to the scan 
 * service to actually run the desired scan.
 * 
 * @author Michael Wharmby
 *
 */
public class ScanAtom extends QueueAtom implements IHasChildQueue {
	
	/**
	 * Version ID for serialization. Should be updated when class changed. 
	 */
	private static final long serialVersionUID = 20161021L;
	
	private List<IScanPathModel> pathModels;
	private Collection<String> monitors;
	private Map<String,Object> detectorModels;
//	private IProcess perPointProcess;//TODO
	private String queueMessage;
	
	private String scanSubmitQueueName;
	private String scanStatusTopicName;
	private String scanBrokerURI;
	
	/**
	 * No arg constructor for JSON
	 */
	public ScanAtom() {
		super();
	}
	
	/**
	 * Constructor with required arguments to configure a scan of positions 
	 * using only detectors to collect data.
	 * 
	 * @param scName String name of scan
	 * @param pMods List<IScanPathModel> describing the motion during the scan.
	 * @param dMods Map<String,IDetectorModel> containing the detector 
	 *              configuration for the scan.
	 */
	public ScanAtom(String scName, List<IScanPathModel> pMods, Map<String,Object> dMods) {
		super();
		setName(scName);
		pathModels = pMods;
		detectorModels = dMods;
	}
	
	/**
	 * Constructor with required arguments to configure a scan of positions 
	 * using both detectors and monitors to collect data.
	 * 
	 * @param scName String name of scan
	 * @param pMods List<IScanPathModel> describing the motion during the scan.
	 * @param dMods Map<String,IDetectorModel> containing the detector 
	 *              configuration for the scan.
	 * @param mons List<String> names of monitors to use during scan.
	 */
	public ScanAtom(String scName, List<IScanPathModel> pMods, Map<String,Object> dMods, Collection<String> mons) {
		this(scName, pMods, dMods);
		monitors = mons;
	}

	/**
	 * Get the collection of models describing the motions during the scan.
	 * 
	 * @return Collection<IScanPathModel> models of motor moves.
	 */
	public List<IScanPathModel> getPathModels() {
		return pathModels;
	}

	/**
	 * Change the collection of models describing the motor moves during scan.
	 * 
	 * @param pathModels Collection<IScanPathModel> models of motor moves.
	 */
	public void setPathModels(List<IScanPathModel> pathModels) {
		this.pathModels = pathModels;
	}
	
	/**
	 * Add another motor movement model to the scan.
	 * 
	 * @param pathModel IScanPathModel model of motion for the scan.
	 */
	public void addPathModel(IScanPathModel pathModel) {
		pathModels.add(pathModel);
	}
	
	/**
	 * Remove an existing motor movement model from the scan.
	 * 
	 * @param pathModel IScanPathModel to be removed from the scan.
	 */
	public void removePathModel(IScanPathModel pathModel) {
		pathModels.remove(pathModel);
	}

	/**
	 * Return the monitors for which values will be recorded during the scan.
	 * 
	 * @return Collection<String> monitor names which will be polled during 
	 *         scan.
	 */
	public Collection<String> getMonitors() {
		return monitors;
	}

	/**
	 * Change the collection of monitors with values recorded during the scan.
	 * 
	 * @param monitors Collection<String> monitor names which will be polled 
	 *                 during scan.
	 */
	public void setMonitors(Collection<String> monitors) {
		this.monitors = monitors;
	}
	
	/**
	 * Add a monitor to the collection of monitors to be polled during the scan.
	 *  
	 * @param monitor String name of monitor to be added.
	 */
	public void addMonitor(String monitor) {
		monitors.add(monitor);
	}
	
	/**
	 * Remove a monitor from the collection of monitors to be polled during the
	 * scan.
	 *  
	 * @param monitor String name of monitor to be removed.
	 */
	public void removeMonitor(String monitor) {
		monitors.remove(monitor);
	}

	/**
	 * Return the mapping of names and models of detectors from which data will
	 * be recorded during the scan.
	 * 
	 * @return Map<String, IDetectorModel> Key String names of detectors and 
	 *         detector models.
	 */
	public Map<String, Object> getDetectorModels() {
		return detectorModels;
	}

	/**
	 * Replace the mapping of names and models of detectors from which data 
	 * will be recorded during the scan.
	 * 
	 * @param Map<String, IDetectorModel> Key String names of detectors and 
	 *         detector models.
	 */
	public void setDetectorModels(Map<String, Object> detModels) {
		this.detectorModels = detModels;
	}
	
	/**
	 * Add a detector to the collection of detectors from which data will be 
	 * recorded during the scan.
	 * 
	 * @param detName String name of detector.
	 * @param detModel IDetectorModel configuration of detector.
	 */
	public void addDetector(String detName, IDetectorModel detModel) {
		detectorModels.put(detName, detModel);
	}
	
	/**
	 * Remove a detector from the collection of detectors from which data will 
	 * be recorded during the scan.
	 * 
	 * @param String name of detector to be removed.
	 */
	public void removeDetector(String detName) {
		detectorModels.remove(detName);
	}
	
	@Override
	public String getQueueMessage() {
		return queueMessage;
	}

	@Override
	public void setQueueMessage(String msg) {
		this.queueMessage = msg;
	}

	public String getScanSubmitQueueName() {
		return scanSubmitQueueName;
	}

	public void setScanSubmitQueueName(String scanSubmitQueueName) {
		this.scanSubmitQueueName = scanSubmitQueueName;
	}

	public String getScanStatusTopicName() {
		return scanStatusTopicName;
	}

	public void setScanStatusTopicName(String scanStatusTopicName) {
		this.scanStatusTopicName = scanStatusTopicName;
	}

	public String getScanBrokerURI() {
		return scanBrokerURI;
	}

	public void setScanBrokerURI(String scanBrokerURI) {
		this.scanBrokerURI = scanBrokerURI;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((detectorModels == null) ? 0 : detectorModels.hashCode());
		result = prime * result + ((monitors == null) ? 0 : monitors.hashCode());
		result = prime * result + ((pathModels == null) ? 0 : pathModels.hashCode());
		result = prime * result + ((queueMessage == null) ? 0 : queueMessage.hashCode());
		result = prime * result + ((scanBrokerURI == null) ? 0 : scanBrokerURI.hashCode());
		result = prime * result + ((scanStatusTopicName == null) ? 0 : scanStatusTopicName.hashCode());
		result = prime * result + ((scanSubmitQueueName == null) ? 0 : scanSubmitQueueName.hashCode());
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
		ScanAtom other = (ScanAtom) obj;
		if (detectorModels == null) {
			if (other.detectorModels != null)
				return false;
		} else if (!detectorModels.equals(other.detectorModels))
			return false;
		if (monitors == null) {
			if (other.monitors != null)
				return false;
		} else if (!monitors.equals(other.monitors))
			return false;
		if (pathModels == null) {
			if (other.pathModels != null)
				return false;
		} else if (!pathModels.equals(other.pathModels))
			return false;
		if (queueMessage == null) {
			if (other.queueMessage != null)
				return false;
		} else if (!queueMessage.equals(other.queueMessage))
			return false;
		if (scanBrokerURI == null) {
			if (other.scanBrokerURI != null)
				return false;
		} else if (!scanBrokerURI.equals(other.scanBrokerURI))
			return false;
		if (scanStatusTopicName == null) {
			if (other.scanStatusTopicName != null)
				return false;
		} else if (!scanStatusTopicName.equals(other.scanStatusTopicName))
			return false;
		if (scanSubmitQueueName == null) {
			if (other.scanSubmitQueueName != null)
				return false;
		} else if (!scanSubmitQueueName.equals(other.scanSubmitQueueName))
			return false;
		return true;
	}

}
