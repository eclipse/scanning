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

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PositionerAtomProcess reads the values included in a {@link PositionerAtom} 
 * and instructs the positioners detailed in the atom to set these positions.
 * 
 * It uses the server's {@link IRunnableDeviceService} to create an 
 * {@link IPositioner} which it passes the target positions to.
 * 
 * TODO Implement reporting back of the percent complete (when it might 
 *      become available).
 * TODO Implement pausing/resuming when implemented in the IPositioner system.
 * 
 * @author Michael Wharmby
 * 
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using this PositionerAtomProcess. This will be 
 *            {@link QueueAtom}.
 */
public class PositionerAtomProcess<T extends Queueable> extends QueueProcess<PositionerAtom, T> {
	
	/**
	 * Used by {@link QueueProcessFactory} to identify the bean type this 
	 * {@link QueueProcess} handles.
	 */
	public static final String BEAN_CLASS_NAME = PositionerAtom.class.getName();
	
	private static Logger logger = LoggerFactory.getLogger(PositionerAtomProcess.class);
	
	//Scanning infrastructure
	private final IRunnableDeviceService deviceService;
	private IPositioner positioner;
	
	//For processor operation
	private Thread positionThread;
	
	/**
	 * Create a PositionerAtomProcess to position motors on the beamline. 
	 * deviceService ({@link IRunnableDeviceService}) is configured using OSGi 
	 * through {@link ServicesHolder}.
	 */
	public PositionerAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
		//Get the deviceService from the OSGi configured holder.
		deviceService = ServicesHolder.getDeviceService();
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		logger.debug("Creating target position object from configured values");
		broadcast(Status.RUNNING,"Creating target position from configured values");
		
		final IPosition target = new MapPosition(queueBean.getPositionerConfig());
		broadcast(10d);
		
		//Get the positioner
		logger.debug("Getting device positioner");
		broadcast(Status.RUNNING, "Getting device positioner");
		try {
			positioner = deviceService.createPositioner();
		} catch (ScanningException se) {
			broadcast(Status.FAILED, "Failed to get device positioner: \""+se.getMessage()+"\".");
			logger.error("Failed to get device positioner in "+queueBean.getName()+": \""+se.getMessage()+"\".");
			throw new EventException("Failed to get device positioner", se);
		}
		broadcast(20d);
				
		//Create a new thread to call the position setting in
		positionThread = new Thread(new Runnable() {
			
			/*
			 * DO NOT SET FINAL STATUSES IN THIS THREAD - set them in the post-match analysis
			 */
			@Override
			public synchronized void run() {
				//Set positioner device(s)
				try {
					logger.debug("Setting device(s) to requested position");
					broadcast(Status.RUNNING, "Moving device(s) to requested position");
					positioner.setPosition(target);
					
					//Check whether we received an interrupt whilst setting the positioner
					if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Position setting interrupted.");
					//Completed cleanly
					broadcast(99.5);
				} catch (Exception ex) {
					logger.debug("Positioner thread interrupted with: "+ex.getMessage());
					if (isTerminated()) {
						positioner.abort();
						logger.debug("Termination requested. Aborting positioner...");
					} else {
						try {
							broadcast(Status.FAILED, "Moving device(s) in '"+queueBean.getName()+"' failed with: '"+ex.getMessage()+"'");
						} catch(EventException evEx) {
							logger.error("Broadcasting bean failed with: \""+evEx.getMessage()+"\".");
						}
					}
				} finally {
					processLatch.countDown();
				}
			}
		});
		positionThread.setDaemon(true);
		positionThread.setPriority(Thread.MAX_PRIORITY);
		positionThread.start();
	}
	
	@Override
	public void postMatchCompleted() {
		updateBean(Status.COMPLETE, 100d, "Set position completed successfully");
	}

	@Override
	public void postMatchTerminated() {
		positionThread.interrupt();
		queueBean.setMessage("Position change aborted before completion (requested)");
		logger.debug("'"+bean.getName()+"' was requested to abort");
	}

	@Override
	public void postMatchFailed() {
		positioner.abort();
		queueBean.setStatus(Status.FAILED);//<-- Don't set message here; it's broadcast above!
		logger.error("'"+bean.getName()+"' failed. Last message was: '"+bean.getMessage()+"'");
	}
	
	@Override
	protected void terminateCleanupAction() throws EventException {
		positioner.abort();			//<--since setPosition is blocking we need to abort it before...
		positionThread.interrupt(); //<-- ...we call interrupt.
	}
	
	@Override
	protected void doPause() throws EventException {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO
		logger.error("Pause/resume not implemented on PositionerAtom");
	}

	@Override
	protected void doResume() throws EventException {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO
		logger.error("Pause/resume not implemented on PositionerAtom");
	}
	
	@Override
	public Class<PositionerAtom> getBeanClass() {
		return PositionerAtom.class;
	}

}
