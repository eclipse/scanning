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

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueProcess;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.IHasAtomQueue;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.Queue;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic class for processing a {@link Queueable} implementing 
 * {@link IHasAtomQueue}. The processor spools the atoms in the contained queue
 * into a new queue  created through the {@link IQueueService}. The new queue 
 * is monitored using the {@link QueueListener} and through the queue service.
 * 
 * @author Michael Wharmby
 *
 * @param <P> Bean implementing {@link Queueable}, but must be an  
 *            {@link IHasAtomQueue}.
 * @param <Q> Bean from within the AtomQueue - implements {@link QueueAtom}.
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using the parent {@link QueueProcess}. This might be a
 *            {@link QueueBean} or a {@link QueueAtom}.
 */
public class AtomQueueProcessor<P extends Queueable & IHasAtomQueue<Q>, 
								Q extends QueueAtom, 
								T extends Queueable> {
	
	private static Logger logger = LoggerFactory.getLogger(AtomQueueProcessor.class);
	
	private IQueueService queueService;
	private IQueueControllerService queueController;
	private QueueListener<P, Q> queueListener;
	private ISubscriber<QueueListener<P, Q>> queueSubscriber;
	
	private QueueProcess<P, T> parentProcess;
	private String activeQueueID; 
	
	/**
	 * Constructs a new AtomQueueProcessor configured to use the 
	 * {@link IQueueService} and {@link IQueueControllerService} provided by 
	 * the OSGi services holder ({@link ServicesHolder}).
	 * 
	 * @param parentProcess {@link IQueueProcess} tasked with processing a 
	 * 						bean implementing {@link IHasAtomQueue}.
	 */
	public AtomQueueProcessor(QueueProcess<P, T> parentProcess) {
		queueService = ServicesHolder.getQueueService();
		queueController = ServicesHolder.getQueueControllerService();
		this.parentProcess = parentProcess;
	}
	
	/**
	 * Creates a child active-queue {@link IQueue} using the 
	 * {@link IQueueService} and a {@link QueueListener} to monitor it. Beans 
	 * are then spooled from the {@link IHasAtomQueue} instance into the 
	 * consumer using the {@link IQueueControllerService}. Finally the 
	 * {@link Queue} is started and run() then waits to be signalled that 
	 * processing has finished before the method returns. 
	 * 
	 * @throws EventException when spooling beans fails.
	 * @throws InterruptedException if wait for processing to complete is 
	 *                              interrupted.
	 */
	public void run() throws EventException, InterruptedException {
		//Everything should be set up by now, so we can get the atomQueue
		final P atomQueue = parentProcess.getQueueBean();
		
		//Create a new active queue to submit the atoms into
		parentProcess.broadcast(Status.RUNNING, 0d, "Registering new active queue.");
		activeQueueID = queueService.registerNewActiveQueue();
		
		/*
		 * Create QueueListener - this must happen BEFORE submitting beans, 
		 * otherwise the QueueListener doesn't know about the child beans it 
		 * has to listen for. 
		 */
		queueListener = new QueueListener<>(
				parentProcess, 
				parentProcess.getQueueBean(), 
				parentProcess.getProcessLatch());
		queueSubscriber = queueController.createQueueSubscriber(activeQueueID);
		queueSubscriber.addListener(queueListener);
		
		/*
		 * Spool beans from bean atom queue to the queue service
		 * (queue empty after this!)
		 */
		parentProcess.broadcast(Status.RUNNING, 1d, "Submitting atoms to active queue.");
		Queueable parentBean = parentProcess.getQueueBean();//TODO This is not needed - see atomQueue
		while (atomQueue.atomQueueSize() > 0) {
			QueueAtom nextAtom = atomQueue.viewNextAtom();
			if (nextAtom.getBeamline() != parentBean.getBeamline()) {
				nextAtom.setBeamline(parentBean.getBeamline());
			}
			if (nextAtom.getHostName() != parentBean.getHostName()) {
				nextAtom.setHostName(parentBean.getHostName());
			}
			if (nextAtom.getUserName() != parentBean.getUserName()) {
				nextAtom.setUserName(parentBean.getUserName());
			}
			queueController.submit(atomQueue.nextAtom(), activeQueueID);
		}
		
		/*
		 * Start processing & wait for it to end - after returning, we start 
		 * the post-match analysis immediately.
		 */
		parentProcess.broadcast(Status.RUNNING, 4d, "Beans submitted. Starting active queue...");
		queueService.startActiveQueue(activeQueueID);
		parentProcess.broadcast(Status.RUNNING, 5d, "Waiting for active queue to complete");
		parentProcess.getProcessLatch().await();
	}
	
	/**
	 * Instructs {@link IQueueService} to terminate the {@link IConsumer} 
	 * instance (using the stop() method - this terminates all beans in the 
	 * status set.
	 *  
	 * @throws EventException if stop failed.
	 */
	protected void terminate() throws EventException {
		//Calling IConsumer.stop() causes all jobs being processed to terminate 
		queueService.stopActiveQueue(activeQueueID, false);
	}
	
	/**
	 * Clean-up {@link IEventService} infrastructure created to process the 
	 * {@link IHasAtomQueue}.
	 * 
	 * @throws EventException in case of problems during shutdown.
	 */
	protected void tidyQueue() throws EventException {
		//This should happen first to avoid spurious messages about termination
		queueSubscriber.disconnect();
		
		//Tidy up our processes, before handing back control
		try {
			//Only stop the queue if it hasn't already been
			if (queueService.getQueue(activeQueueID).getStatus().isActive()) {
				queueService.stopActiveQueue(activeQueueID, false);
			}
			queueService.deRegisterActiveQueue(activeQueueID);
		} catch (EventException evEx) {
			/*
			 * If the queueService pushes back an EventException with the 
			 * message "stopped" this means the QueueService has been stopped 
			 * by another process & we don't need to do anything. If another
			 * message is sent, it's a real problem.
			 */
			if (evEx.getMessage().equals("stopped")) {
				logger.warn("QueueService resources already stopped");
			} else {
				throw evEx;
			}
		}
	}

	/**
	 * Returns the active-queue ID created by this AtomQueueProcessor
	 * - used in tests.
	 */
	public String getActiveQueueID() {
		return activeQueueID;
	}
	
}
