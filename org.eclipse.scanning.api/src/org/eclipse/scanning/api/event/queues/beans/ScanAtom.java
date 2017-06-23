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

import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
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
	
	private ScanRequest<?> scanReq;
	
	private Map<String, List<IQueueValue<?>>> dModsModel;
	private Map<String, List<IQueueValue<?>>> pModsModel;
	private Collection<IQueueValue<?>> monsModel;
	
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
	 * Constructor which allows specification of a scan using the full API of 
	 * the {@link ScanRequest}.
	 * 
	 * @param scShrtNm String short name used within the QueueBeanFactory
	 * @param scanReq {@link ScanRequest} describing the complete scan
	 */
	public ScanAtom(String scShrtNm, ScanRequest<?> scanReq) {
		super();
		setShortName(scShrtNm);
		setModel(false);
		this.scanReq = scanReq;
	}
	
	/**
	 * Constructor with required arguments to configure a scan of positions 
	 * using both detectors and monitors to collect data.
	 * 
	 * @param scShrtNm String short name used within the QueueBeanFactory
	 * @param pMods List<IScanPathModel> describing the motion during the scan
	 * @param dMods Map<String,Object> containing the detector configuration 
	 *        for the scan (get these by calling 
	 *        IRunnableDeviceService.getRunnableDevice(detector_name)
	 * @param mons List<String> names of monitors to use during scan
	 */
	public ScanAtom(String scShrtNm, List<IScanPathModel> pMods, Map<String,Object> dMods, Collection<String> mons) {
		super();
		setShortName(scShrtNm);
		setModel(false);
		scanReq = new ScanRequest<>();
		scanReq.setCompoundModel(new CompoundModel<>(pMods));
		scanReq.setDetectors(dMods);
		scanReq.setMonitorNames(mons);
	}

	/**
	 * Constructor to create a model instance of a {@link ScanAtom} which will 
	 * be converted to a real instance by the {@link IQueueBeanFactory}. The 
	 * final scan has positions, detectors and monitors defined.
	 * 
	 * @param scShrtNm String short name used within the QueueBeanFactory
	 * @param pMods Map of Strings and List of {@link IQueueValue} which 
	 *        define the positions visited during the scan
	 * @param dMods Map of Strings and List of {@link IQueueValue} which 
	 *        define the detectors used during the scan
	 * @param mons Collection of {@link IQueueValue} defining monitors to be 
	 *        read
	 */
	public ScanAtom(String scShrtNm, Map<String, List<IQueueValue<?>>> pMods, Map<String,List<IQueueValue<?>>> dMods, Collection<IQueueValue<?>> mons) {
		super();
		setShortName(scShrtNm);
		setModel(true);
		pModsModel = pMods;
		dModsModel = dMods;
		monsModel = mons;
	}
	
	public ScanRequest<?> getScanReq() {
		return scanReq;
	}

	public void setScanReq(ScanRequest<?> scanReq) {
		this.scanReq = scanReq;
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

	public Map<String, List<IQueueValue<?>>> getdModsModel() {
		return dModsModel;
	}

	public void setdModsModel(Map<String, List<IQueueValue<?>>> dModsModel) {
		this.dModsModel = dModsModel;
	}

	public Map<String, List<IQueueValue<?>>> getpModsModel() {
		return pModsModel;
	}

	public void setpModsModel(Map<String, List<IQueueValue<?>>> pModsModel) {
		this.pModsModel = pModsModel;
	}

	public Collection<IQueueValue<?>> getMonsModel() {
		return monsModel;
	}

	public void setMonsModel(Collection<IQueueValue<?>> monsModel) {
		this.monsModel = monsModel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((dModsModel == null) ? 0 : dModsModel.hashCode());
		result = prime * result + ((monsModel == null) ? 0 : monsModel.hashCode());
		result = prime * result + ((pModsModel == null) ? 0 : pModsModel.hashCode());
		result = prime * result + ((queueMessage == null) ? 0 : queueMessage.hashCode());
		result = prime * result + ((scanBrokerURI == null) ? 0 : scanBrokerURI.hashCode());
		result = prime * result + ((scanReq == null) ? 0 : scanReq.hashCode());
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
		if (dModsModel == null) {
			if (other.dModsModel != null)
				return false;
		} else if (!dModsModel.equals(other.dModsModel))
			return false;
		if (monsModel == null) {
			if (other.monsModel != null)
				return false;
		} else if (!monsModel.equals(other.monsModel))
			return false;
		if (pModsModel == null) {
			if (other.pModsModel != null)
				return false;
		} else if (!pModsModel.equals(other.pModsModel))
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
		if (scanReq == null) {
			if (other.scanReq != null)
				return false;
		} else if (!scanReq.equals(other.scanReq))
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

	@Override
	public String toString() {
		String clazzName = this.getClass().getSimpleName();
		
		StringBuffer scanDetailsStrBuff = new StringBuffer();
		if (model) {
			clazzName = clazzName + " (MODEL)";
			scanDetailsStrBuff.append("{paths="+pModsModel);
			scanDetailsStrBuff.append(", detectors="+dModsModel);
			scanDetailsStrBuff.append(", monitors="+monsModel+"}");
		} else {
			scanDetailsStrBuff.append(scanReq);
		}
		return clazzName + " [name=" + name + " (shortName="+shortName+"), scanReq=" + scanDetailsStrBuff 
				+ ", scanSubmitQueueName=" + scanSubmitQueueName + ", scanStatusTopicName=" + scanStatusTopicName 
				+ ", scanBrokerURI=" + scanBrokerURI + ", status=" + status + ", message=" + message 
				+ ", queueMessage=" + queueMessage + ", percentComplete=" + percentComplete + ", previousStatus=" 
				+ previousStatus + ", runTime=" + runTime+ ", userName=" + userName+ ", hostName=" 
				+ hostName + ", beamline="+ beamline + ", submissionTime=" + submissionTime + "]";
	}

}
