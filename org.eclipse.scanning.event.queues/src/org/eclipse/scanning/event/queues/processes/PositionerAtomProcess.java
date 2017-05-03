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
	public Class<PositionerAtom> getBeanClass() {
		return PositionerAtom.class;
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		executed = true;
		broadcast(Status.RUNNING,"Creating position from configured values.");
		
		final IPosition target = new MapPosition(queueBean.getPositionerConfig());
		broadcast(10d);
		
		//Get the positioner
		broadcast(Status.RUNNING, "Getting device positioner.");
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
					broadcast(Status.RUNNING, "Moving device(s) to requested position.");
					positioner.setPosition(target);
					
					//Check whether we received an interrupt whilst setting the positioner
					if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Position setting interrupted.");
					//Completed cleanly
					broadcast(99.5);
					processLatch.countDown();
				} catch (Exception ex) {
					if (isTerminated()) {
						positioner.abort();
						processLatch.countDown();
					} else {
						reportFail(ex, "Moving device(s) in '"+queueBean.getName()+"' failed with: \""+ex.getMessage()+"\".");
					}
				}
			}
		});
		positionThread.setDaemon(true);
		positionThread.setPriority(Thread.MAX_PRIORITY);
		positionThread.start();
	}
	
	@Override
	protected void postMatchAnalysis() throws EventException, InterruptedException {
		try {
			postMatchAnalysisLock.lockInterruptibly();

			if (isTerminated()) {
				broadcast("Position change aborted before completion (requested).");
				return;
			}

			if (queueBean.getPercentComplete() >= 99.5) {
				//Clean finish
				broadcast(Status.COMPLETE, 100d, "Position change completed.");
			} else {
				//Scan failed
				//TODO Set message? Or is it set elsewhere?
				positioner.abort();
				broadcast(Status.FAILED);
			} 
		} finally {
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

			positionThread.interrupt();
			terminated = true;

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
		logger.error("Pause/resume not implemented on PositionerAtom");
		throw new EventException("Pause/resume not implemented on PositionerAtom");
	}

	@Override
	protected void doResume() throws EventException {
		logger.error("Pause/resume not implemented on PositionerAtom");
		throw new EventException("Pause/resume not implemented on PositionerAtom");
	}

}
