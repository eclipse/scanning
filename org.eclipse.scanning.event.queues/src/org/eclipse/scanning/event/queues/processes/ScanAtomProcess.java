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
package org.eclipse.scanning.event.queues.processes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ScanAtomProcess takes the fields of a {@link ScanAtom} and from them makes
 * a {@link ScanBean}, which is then submitted to the scan event service.
 * 
 * The process uses a {@link QueueListener} to monitor the process of the 
 * scan and pass up messages to the rest of the queue.
 * 
 * @author Michael Wharmby
 * 
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using this ScanAtomProcess. This will be 
 *            {@link QueueAtom}. 
 */
public class ScanAtomProcess<T extends Queueable> extends QueueProcess<ScanAtom, T> {
	
	/**
	 * Used by {@link QueueProcessFactory} to identify the bean type this 
	 * {@link QueueProcess} handles.
	 */
	public static final String BEAN_CLASS_NAME = ScanAtom.class.getName();
	
	private static Logger logger = LoggerFactory.getLogger(ScanAtomProcess.class);
	
	//Scanning infrastructure
	private IEventService eventService;
	private IPublisher<ScanBean> scanPublisher;
	private ISubscriber<QueueListener<ScanAtom, ScanBean>> scanSubscriber;
	private QueueListener<ScanAtom, ScanBean> queueListener;
	
	//For processor operation
	private ScanBean scanBean;
	
	/**
	 * Create a ScanAtomProcessor which can be used by a {@link QueueProcess}. 
	 * Constructor configures the {@link IEventService} using the instance 
	 * specified in the {@link ServicesHolder}. Additionally, a new 
	 * {@link ScanBean} is created which will be configured with the details 
	 * of from the {@link ScanAtom}.
	 */
	public ScanAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
		eventService = ServicesHolder.getEventService();
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		executed = true;

		//Get config for scanning infrastructure
		URI scanBrokerURI;
		String scanStatusTopicName, scanSubmitQueueName;
		broadcast(Status.RUNNING, "Reading scanning service configuration.");
		try {
			if (queueBean.getScanBrokerURI() == null) {
				scanBrokerURI = new URI(CommandConstants.getScanningBrokerUri());
			} else {
				scanBrokerURI = new URI(queueBean.getScanBrokerURI());
			}
		} catch (URISyntaxException uSEx) {
			logger.error("Could not determine scan broker URI: "+uSEx.getMessage());
			throw new EventException("Scan broker URI syntax incorrect", uSEx);
		}
		if (queueBean.getScanStatusTopicName() == null) {
			scanStatusTopicName = EventConstants.STATUS_TOPIC;
		} else {
			scanStatusTopicName = queueBean.getScanStatusTopicName();
		}
		if (queueBean.getScanSubmitQueueName() == null) {
			scanSubmitQueueName = EventConstants.STATUS_TOPIC;
		} else {
			scanSubmitQueueName = queueBean.getScanSubmitQueueName();
		}
		
		broadcast(Status.RUNNING, 1d, "Creating scan request from configured values.");
		ScanRequest<?> scanReq = new ScanRequest<>();
		scanReq.setCompoundModel(new CompoundModel<>(queueBean.getPathModels()));
		scanReq.setDetectors(queueBean.getDetectorModels());
		scanReq.setMonitorNames(queueBean.getMonitors());
		
		broadcast(Status.RUNNING, 2d, "Setting up ScanBean");
		scanBean = new ScanBean();
		if (scanBean.getUniqueId() == null) scanBean.setUniqueId(UUID.randomUUID().toString());
		scanBean.setBeamline(queueBean.getBeamline());
		scanBean.setName(queueBean.getName());
		scanBean.setHostName(queueBean.getHostName());
		scanBean.setUserName(queueBean.getUserName());
		scanBean.setScanRequest(scanReq);
		
		broadcast(Status.RUNNING, 3d, "Creating scanning infrastructure.");
		scanPublisher = eventService.createPublisher(scanBrokerURI, scanStatusTopicName);
		scanSubscriber = eventService.createSubscriber(scanBrokerURI, scanStatusTopicName);
		queueListener = new QueueListener<>(this, queueBean, processLatch, scanBean);
		try {
			scanSubscriber.addListener(queueListener);
		} catch (EventException evEx) {
			broadcast(Status.FAILED, "Failed to add QueueListener to scan subscriber; unable to monitor queue. Cannot continue: \""+evEx.getMessage()+"\".");
			logger.error("Failed to add QueueListener to scan subscriber for '"+queueBean.getName()+"'; unable to monitor queue. Cannot continue: \""+evEx.getMessage()+"\".");
			throw new EventException("Failed to add QueueListener to scan subscriber", evEx);
		}
		ISubmitter<ScanBean> scanSubmitter = eventService.createSubmitter(scanBrokerURI, scanSubmitQueueName);
		
		broadcast(Status.RUNNING, 4d, "Submitting bean to scanning service.");
		scanBean.setStatus(Status.SUBMITTED);
		try {
			scanSubmitter.submit(scanBean);
			scanSubmitter.disconnect();
		} catch (EventException evEx) {
			commandScanBean(Status.REQUEST_TERMINATE); //Just in case the submission worked, but the disconnect didn't, stop the runnning process
			broadcast(Status.FAILED, "Failed to submit scan bean to scanning system: \""+evEx.getMessage()+"\".");
			logger.error("Failed to submit scan bean '"+scanBean.getName()+"' to scanning system: \""+evEx.getMessage()+"\".");
			throw new EventException("Failed to submit scan bean to scanning system", evEx);
		}
		
		//Allow scan to run
		broadcast(Status.RUNNING, 5d, "Waiting for scan to complete...");
		processLatch.await();		
	}

	@Override
	protected void postMatchAnalysis() throws EventException, InterruptedException {
		try {
			postMatchAnalysisLock.lockInterruptibly();
			if (isTerminated()) {
				//Do different things if terminate was requested from the child
				if (queueListener.isChildCommand()) {
					queueBean.setMessage("Scan aborted from scanning service.");
				} else {
					queueBean.setMessage("Scan aborted before completion (requested).");
					commandScanBean(Status.REQUEST_TERMINATE);
				}
			} else if (queueBean.getPercentComplete() >= 99.5) {
				//Completed successfully
				updateBean(Status.COMPLETE, 100d, "Scan completed.");
			} else {
				//Scan failed - don't set anything here as messages should have 
				//been updated elsewhere
				queueBean.setStatus(Status.FAILED);
			}
		} finally {
			//This should be run after we've reported the queue final state
			tidyScanActors();
			
			//And we're done, so let other processes continue
			executionEnded();
			
			postMatchAnalysisLock.unlock();
			
			/*
			 * N.B. Broadcasting needs to be done last; otherwise the next 
			 * queue may start when we're not ready. Broadcasting should not 
			 * happen if we've been terminated.
			 */
			if (!isTerminated()) {
				broadcast();
			}
		}
	}
	
	@Override
	protected void doTerminate() throws EventException {
		try {
			//Reentrant lock ensures execution method (and hence post-match 
			//analysis) completes before terminate does
			postMatchAnalysisLock.lockInterruptibly();
			
			terminated = true;
			processLatch.countDown();
			
			//Wait for post-match analysis to finish
			continueIfExecutionEnded();
		} catch (InterruptedException iEx) {
			throw new EventException(iEx);
		} finally {
			postMatchAnalysisLock.unlock();
		}
	}
	
	@Override
	protected void doPause() throws EventException {
//		if (!queueListener.isChildCommand()) {
//			commandScanBean(Status.REQUEST_PAUSE);
//		}
		//TODO!
	}

	@Override
	protected void doResume() throws EventException {
//		if (!queueListener.isChildCommand()) {
//			commandScanBean(Status.REQUEST_RESUME);
//		}
		//TODO!
	}

	
	@Override
	public Class<ScanAtom> getBeanClass() {
		return ScanAtom.class;
	}
	
	/**
	 * Send instructions to the child ScanBean.
	 * 
	 * @param command the new State of the ScanBean.
	 * @throws EventException In case broadcasting fails.
	 */
	private void commandScanBean(Status command) throws EventException {
		if (scanPublisher == null) {
			broadcast(Status.FAILED, "Scan Publisher not initialised. Cannot send commands to scanning system");
			logger.error("Scan publisher not initialised. Cannot send commands to scanning system for '"+queueBean.getName()+"'.");
			throw new EventException("Scan publisher not initialised. Cannot send commands to scanning system");
		}
		if (!command.isRequest()) {
			logger.warn("Command \""+command+"\" to ScanBean '"+scanBean.getName()+"' is not a request. Unexpected behaviour may result.");
		}
		scanBean.setStatus(command);
		scanPublisher.broadcast(scanBean);
	}
	
	/**
	 * Clean up EventService objects which interact with the scan child queue.
	 * @throws EventException
	 */
	private void tidyScanActors() throws EventException {
		scanPublisher.disconnect();
		scanSubscriber.disconnect();
	}

}
