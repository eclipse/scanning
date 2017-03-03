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
package org.eclipse.scanning.api.event.queues;

import java.net.URI;
import java.util.Set;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * **
 * The IQueueService provides an interface for the unattended management of 
 * {@link IEventService} queues.
 * 
 * Two types of queue may be created, started, stopped or destroyed:
 * - job-queue - acting on {@link QueueBean}s
 * - active-queue - acting on {@link QueueAtom}s
 * 
 * The job-queue is treated as a top-level queue - there should only be one 
 * created per IQueueService instance. The active-queue is where the actual 
 * experiment processing should be done; as such there can be any number of 
 * these in parent-child relationships, with the job-queue at the root of the 
 * tree.
 * 
 * @author Michael Wharmby
 *
 */
public interface IQueueService {
	
	/**
	 * Topics which will always be used to obtain information about the 
	 * IQueueService.
	 */
	public static final String QUEUE_REQUEST_TOPIC = "org.eclipse.scanning.event.queues.service.request.topic";
	public static final String QUEUE_RESPONSE_TOPIC = "org.eclipse.scanning.event.queues.service.response.topic";
	
	/**
	 * Suffixes to be appended to the names of the destinations within a 
	 * concrete instance of IQueueService
	 */
	public static final String HEARTBEAT_TOPIC_SUFFIX = ".heartbeat.topic";
	public static final String COMMAND_SET_SUFFIX = ".command.queue";
	public static final String COMMAND_TOPIC_SUFFIX = ".command.topic";
	public static final String JOB_QUEUE_SUFFIX = ".job-queue";
	public static final String ACTIVE_QUEUE_PREFIX = ".aq-";
	public static final String ACTIVE_QUEUE_SUFFIX = ".active-queue";
	
	/**
	 * Initialise the queue service. This should ensure the service is capable
	 * of creating new {@link IQueue} instances and initialise the job queue.
	 * 
	 * @throws EventException - if it was not possible to create the job queue.
	 * @throws IllegalStateException - if the queue-root & URI have not been 
	 *                                 configured.
	 */
	public void init() throws EventException;
	
	/**
	 * Method to tidy up after the method when the service is to be shutdown.
	 * 
	 * @throws EventException - in case of problems stopping the IQueueService.
	 */
	public void disposeService() throws EventException;
	
	/**
	 * Start the job-queue consumer.
	 * 
	 * @throws EventException - in case it was not possible to start the 
	 *                          consumer.
	 * @throws IllegalStateException - if service not initialised.
	 */
	public void start() throws EventException;
	
	/**
	 * Stop the job-queue consumer and any dependent active-queue consumers 
	 * gracefully. If force is true, consumers will be killed rather than 
	 * stopped (this will leave jobs in the submit queues).
	 * 
	 * @param force True if all consumers are to be killed.
	 * @throws EventException - if consumers could not be stopped.
	 */
	public void stop(boolean force) throws EventException;
	
	/**
	 * Create a new active-queue instance and register it with the service. 
	 * This first should also clear the queues/topics to make sure they're 
	 * genuinely clean.
	 * 
	 * @return String name of the active-queue registered.
	 * @throws EventException - if it was not possible to create the 
	 *                          active-queue.
	 */
	public String registerNewActiveQueue() throws EventException;
	
	/**
	 * Remove a defunct active-queue from the map of registered queues. If the 
	 * queue contains jobs, attempt to gracefully stop & dispose of the queue 
	 * first. Use force to forcibly remove the queue.
	 * 
	 * @param queueID String ID  of queue to be deregistered.
	 * @throws EventException - if the given active-queue could not be found or
	 * 						    problems were encountered stopping the 
	 *                          consumer.
	 * @throws IllegalStateException - if the IQueueService has not been 
	 *                                 started.
	 */
	public void deRegisterActiveQueue(String queueID) throws EventException;
	
	/**
	 * Report whether given active-queue is registered with service.
	 * 
	 * @param queueID String ID of registered queue. 
	 * @return true if queueID found in registry.
	 */
	public boolean isActiveQueueRegistered(String queueID);
	
	/**
	 * Start a registered active-queue consumer.
	 * 
	 * @param queueID String ID of active-queue registered.
	 * @throws EventException - if the active-queue cannot be started.
	 */
	public void startActiveQueue(String queueID) throws EventException;
	
	/**
	 * Stop a registered active-queue gracefully. If force is true, consumers 
	 * will be killed rather than stopped (this will leave jobs in the submit 
	 * queue). This will also dispose the queue, clearing any remaining beans.
	 * 
	 * @param queueID String ID of registered queue.
	 * @param force True if all consumers are to be killed.
	 * @throws EventException - if consumers could not be stopped.
	 */
	public void stopActiveQueue(String queueID, boolean force) throws EventException;
	
	/**
	 * Return a list of all registered active-queues.
	 * 
	 * @return List of strings of active-queues registered with the service.
	 */
	public Set<String> getAllActiveQueueIDs();
	
	/**
	 * Default method to return any queue based on a supplied queueID.
	 * Note: the IQueue is case to type T which extends {@link Queueable}; both
	 * {@link QueueBean} & {@link QueueAtom} (expected types) extend Queueable.
	 * 
	 * @param String queueID of the queue to be returned.
	 * @return IQueue representing the requested queue.
	 * @throws EventException - if queueID is unknown
	 */
	//OK, since Queueable is a supertype of QueueBean & QueueAtom
	@SuppressWarnings("unchecked")
	public default <T extends Queueable> IQueue<T> getQueue(String queueID) throws EventException {//FIXME
		if (queueID.equals(getJobQueueID())) {
			return (IQueue<T>) getJobQueue();
		} else {
			if (isActiveQueueRegistered(queueID)) {
				return (IQueue<T>) getActiveQueue(queueID);
			} else {
				throw new EventException("QueueID does not match any registered queue");
			}
		}
	}
	
	/**
	 * Get the job-queue managed by this service.
	 * 
	 * @return {@link IQueue} job-queue of this service.
	 */
	public IQueue<QueueBean> getJobQueue();

	/**
	 * Get a particular registered active-queue from the queue registry.
	 * 
	 * @param queueID String ID of registered queue.
	 * @return The requested active-queue instance, implementing {@link IQueue}.
	 * @throws EventException - if queueID is unknown
	 */
	public IQueue<QueueAtom> getActiveQueue(String queueID) throws EventException;
	
	/**
	 * Get the queueID of the job-queue managed by this service.
	 * 
	 * @return String queueID of job-queue.
	 */
	public String getJobQueueID();
	
	/**
	 * Return the base name for all queues managed by this IQueueService.
	 * 
	 * @return String queue base name.
	 */
	public String getQueueRoot();
	
	/**
	 * Change the base name used by all queues managed by this IQueueService.
	 * Should not be possible to change this while service is started. Changing
	 * the queueRoot should also change the heartbeat and command topic names.
	 * 
	 * @param String queueRoot queue base name.
	 * @throws EventException If attempting to change whilst service started.
	 */
	public void setQueueRoot(String queueRoot) throws UnsupportedOperationException, EventException;
	
	/**
	 * Return name of topic on which heartbeats of queues associated with this 
	 * service will be published.
	 * 
	 * @return String name of heartbeat topic for this service.
	 */
	public String getHeartbeatTopicName();
	
	/**
	 * Return name of queue (set) where command to consumers associated with 
	 * this service will be held.
	 * 
	 * @return String name of command queue (set) for this service.
	 */
	public String getCommandSetName();
	
	/**
	 * Return name of topic where commands to consumers associated with this 
	 * service will be published.
	 * 
	 * @return String name of command topic for this service.
	 */
	public String getCommandTopicName();
	
	/**
	 * Return the URI of the broker storing the queues.
	 * 
	 * @return URI to the queue broker.
	 */
	public URI getURI();
	
	/**
	 * Return the String path of the URI used to configure this service.
	 * 
	 * @return String URI to the queue broker. 
	 */
	public String getURIString();
	
	/**
	 * Change the URI of the broker storing the queues.
	 * Should not be possible to change this while service is started.
	 * 
	 * @param uri URI of new queue server.
	 * @throws EventException If attempting to change whilst service started.
	 */
	public void setUri(URI uri) throws UnsupportedOperationException, EventException;
	
	/**
	 * Change the URI of the broker storing the queues with a String.
	 * Should not be possible to change this while service is started.
	 * 
	 * @param uri String URI of new queue server.
	 * @throws EventException If attempting to change whilst service started.
	 */
	public void setUri(String uri) throws UnsupportedOperationException, EventException;
	
	/**
	 * Return whether the service has been initialised.
	 * 
	 * @return true if initialised.
	 */
	public boolean isInitialized();
	
	/**
	 * Return whether the queue service is currently running.
	 * 
	 * @return true if queue service running.
	 */
	public boolean isActive();

}
