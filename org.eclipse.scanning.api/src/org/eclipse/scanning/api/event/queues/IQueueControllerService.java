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

import java.util.EventListener;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;

/**
 * Service to allow the server based {@link IQueueService} to be controlled 
 * either from processes running on the service (i.e. those run as part of the 
 * {@link IQueueService}) or remotely from a client. In the local (server-side)
 * mode, calls should be made directly to the running {@link IQueueService} 
 * instance, whereas in remote mode, calls should be made through an 
 * {@link IRequester} instance.
 * 
 * @author Michael Wharmby
 *
 */
public interface IQueueControllerService {
	
	/**
	 * Initialise the service by configuring options not set by Spring. In the 
	 * local instance, this might be setting up the OSGi configured 
	 * {@link IQueueService}; in the remote case, the {@link IRequester} might 
	 * be set up.
	 * 
	 * @throws EventException - if underlying {@link IQueueService} or 
	 *                          {@link IRequester} fail to start up properly.
	 * 
	 */
	public void init() throws EventException;
	
	/**
	 * Start the {@link IQueueService} associated with this IQueueControllerService.
	 * 
	 * @throws EventException - if it was not possible to start the service.
	 */
	public void startQueueService() throws EventException;
	
	/**
	 * Stop the {@link IQueueService} gracefully. If force is true, consumers 
	 * will be killed rather than stopped.
	 * 
	 * @param force True if all consumers are to be killed.
	 * @throws EventException - if the service could not be stopped.
	 */
	public void stopQueueService(boolean force) throws EventException;
	
	/**
	 * Submit the given bean for processing in an {@link IQueue} selected from 
	 * the {@link IQueueService} based on the given queueID.
	 * 
	 * @param bean Object extending {@link Queueable} to be submitted for 
	 *             processing. 
	 * @param queueID String ID of {@link IQueue} where processing will be done.
	 * @throws EventException - if the queueID is unknown or underlying 
	 *                          submission system fails.
	 * @throws IllegalArgumentException - if the bean has the wrong type for 
	 *                                    the given queueID.
	 */
	public <T extends Queueable> void submit(T bean, String queueID) throws EventException;
	
	/**
	 * Remove a bean which has been submitted to the {@link IQueue} with ID 
	 * queueID. Processing of the bean should not have started.
	 * 
	 * @param bean Object extending {@link Queueable} to be removed.
	 * @param queueID String ID of {@link IQueue} where processing will be done.
	 * @throws EventException - if the queueID is unknown, underlying removal 
	 *                          system fails or the bean is not present in the 
	 *                          submission queue.
	 * @throws IllegalArgumentException - if the bean has the wrong type for 
	 *                                    the given queueID.
	 */
	public <T extends Queueable>void remove(T bean, String queueID) throws EventException;
	
	/**
	 * Change the position of a bean in the submission queue of the 
	 * {@link IConsumer} of the {@link IQueue} with the given queueID by a 
	 * specified amount. Bean can be moved up or down the queue.
	 * 
	 * @param bean Object extending {@link Queueable} to be moved in queue.
	 * @param move positive/negative integer number of places to move bean.
	 * @param queueID String ID of {@link IQueue} where bean is.
	 * @throws EventException - if the queueID is unknown, underlying 
	 *                          reordering systems fails or the bean is not 
	 *                          present in the submission queue.
	 * @throws IllegalArgumentException - if the bean has the wrong type for 
	 *                                    the given queueID.
	 */
	public <T extends Queueable>void reorder(T bean, int move, String queueID) throws EventException;
	
	/**
	 * Pause the processing of a bean in the StatusSet (see {@link IConsumer}).
	 * Note, this only pauses the processing of a particular bean, not the 
	 * entire consumer. Thus if the process is  non-blocking, further beans may
	 * begin processing whilst this bean is paused.
	 * 
	 * @param bean Object extending {@link Queueable} to be paused.
	 * @param queueID String ID of {@link IQueue} where bean is.
	 * @throws EventException - if the queueID is unknown, the bean is not in 
	 *                          the queue or has not started processing, or the
	 *                          underlying systems fails.
	 * @throws IllegalArgumentException - if the bean has the wrong type for 
	 *                                    the given queueID.
	 * @throws IllegalStateException - if the bean has already been paused or 
	 *                                 is only submitted.
	 */
	public <T extends Queueable>void pause(T bean, String queueID) throws EventException;
	
	/**
	 * Resume processing of a bean in the StatusSet (see {@link IConsumer}) 
	 * which was previously paused. Note this only affects the processing of 
	 * this bean and will not resume a paused {@link IConsumer}.
	 * 
	 * @param bean paused Object extending {@link Queueable} to be resumed.
	 * @param queueID String ID of {@link IQueue} where bean is.
	 * @throws EventException - if the queueID is unknown, the bean is not in 
	 *                          the queue or has not started processing, or the
	 *                          underlying systems fails.
	 * @throws IllegalArgumentException - if the bean has the wrong type for 
	 *                                    the given queueID.
	 * @throws IllegalStateException - if the bean has already been resumed or 
	 *                                 is only submitted.
	 */
	public <T extends Queueable>void resume(T bean, String queueID) throws EventException;
	
	/**
	 * Terminates a running bean process in the {@link IQueue} with the given 
	 * queueID. Note, if the bean has {@link State} submitted, it will be 
	 * removed instead.
	 * 
	 * @param bean Object extending {@link Queueable} to be terminated.
	 * @param queueID String ID of {@link IQueue} where bean is.
	 * @throws EventException - if the queueID is unknown, the bean is not in 
	 *                          the queue or has not started processing, or the 
	 *                          underlying systems fails.
	 * @throws IllegalArgumentException - if the bean has the wrong type for 
	 *                                    the given queueID.
	 * @throws IllegalStateException - if the bean has already been terminated.
	 */
	public <T extends Queueable>void terminate(T bean, String queueID) throws EventException;
	
	/**
	 * Pauses the running of the {@link IConsumer} associated with the 
	 * {@link IQueue} with the given queueID. Note, this will not pause the 
	 * running process, but will stop any new processes from being started 
	 * (without killing any processes). 
	 *  
	 * @param queueID String ID of {@link IQueue} to be paused.
	 * @throws EventException - if the queueID is unknown or underlying pause 
	 *                          systems fails.
	 */
	public void pauseQueue(String queueID) throws EventException;
	
	/**
	 * Resumes the paused {@link IConsumer} associated with the {@link IQueue} 
	 * with the given queueID. Note, this will no resume any paused processes 
	 * on this consumer - it only affects the consumer itself. 
	 * 
	 * @param queueID String ID of paused {@link IQueue} to be resumed.
	 * @throws EventException - if the queueID is unknown or underlying resume 
	 *                          systems fails.
	 */
	public void resumeQueue(String queueID) throws EventException;
	
	/**
	 * Terminates the {@link IConsumer} and any bean processes associated, 
	 * running as the {@link IQueue} object with the given queueID. This will 
	 * not stop gracefully so should be used with care.
	 * 
	 * @param queueID String ID of {@link IQueue} to be killed.
	 * @param disconnect if true stop the {@link IConsumer} and disconnect 
	 *                   underlying queue message passing infrastructure. 
	 * @param restart if true, reconnect & start the consumer. 
	 * @param exitProcess if true, exit the {@link IConsumer} JVM.
	 * @throws EventException - if the queueID is unknown or underlying queue 
	 *                          killing systems fails.
	 */
	public void killQueue(String queueID, boolean disconnect, boolean restart, boolean exitProcess) throws EventException;
	
	/**
	 * Create an {@link ISubscriber} instance configured to listen on the 
	 * status topic (see {@link IConsumer}) of the {@link IQueue} with the 
	 * given queueID.
	 * 
	 * @param queueID String ID of {@link IQueue} to be listened to.
	 * @return {@link ISubscriber} configured to listen to queueID.
	 * @throws EventException - if the queueID is unknown.
	 */
	public <T extends EventListener> ISubscriber<T> createQueueSubscriber(String queueID) throws EventException;
	
	/**
	 * Return the string name of the command set (see {@link IConsumer}) of 
	 * all of the queues associated with the configured {@link IQueueService}.
	 * 
	 * @return String command set name.
	 * @throws EventException if 
	 */
	public String getCommandSetName() throws EventException;
	
	/**
	 * Return the string name of the command topic (see {@link IConsumer}) of 
	 * all of the queues associated with the configured {@link IQueueService}.
	 * 
	 * @return String command topic name.
	 */
	public String getCommandTopicName() throws EventException;
	
	/**
	 * Return the string name of the heartbeat topic (see {@link IConsumer}) of 
	 * all of the queues associated with the configured {@link IQueueService}.
	 * 
	 * @return String heartbeat topic name.
	 */
	public String getHeartbeatTopicName() throws EventException;
	
	/**
	 * Return the ID of the job-queue (top level queue) of the configured 
	 * {@link IQueueService}.
	 * 
	 * @return String job-queue ID for the managed {@link IQueueService}.
	 */
	public String getJobQueueID() throws EventException;
	
	/**
	 * Return an {@link IQueue} object containing the configuration options 
	 * for an entire queue. If this is a server-side instance, the 
	 * {@link IConsumer} running the queue will be returned as part of this 
	 * object too.
	 * 
	 * @param String queueID of {@link IQueue} object to return.
	 * @return {@link IQueue} object containing queue configuration.
	 * @throws EventException - if the queue cannot be returned.
	 */
	public IQueue<? extends Queueable> getQueue(String queueID) throws EventException;
	
	/**
	 * Return the current status of a bean (with unique ID beanID) being 
	 * processed in the the {@link IQueue} with the given queueID. 
	 * 
	 * @param beanID String unique ID of bean
	 * @param queueID String ID of {@link IQueue} in {@link IQueueService}.
	 * @return {@link Status} of the requested bean.
	 * @throws EventException - if the bean wasn't found in the queue.
	 */
	public Status getBeanStatus(String beanID, String queueID) throws EventException;

}
