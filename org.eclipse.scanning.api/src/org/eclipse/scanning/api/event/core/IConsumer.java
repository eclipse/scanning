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
package org.eclipse.scanning.api.event.core;

import java.util.List;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.ConsumerStatus;

/**
 * 
 * A consumer consumes the submission queue. If a job appears on the queue, it
 * starts to run the job and moves the job to a status queue. As information is
 * run about the job, it publishes topics containing the current state of the bean
 * in the queue (JSON string).
 * 
 * This consumer is intended to replace the consumer in the DAWN command server and
 * be a generic way to run jobs with messaging.
 * 
 * @author Matthew Gerring
 *
 * @param <T> The bean class used for the Queue
 */
public interface IConsumer<T> extends IQueueConnection<T> {
	
	
	/**
	 * Get a copy of the current submission queue as a list of beans, type T
	 * The list is ordered by submission time, not necessarily the ordering
	 * of the JMS queue.
	 * 
	 * @return
	 */
	public List<T> getSubmissionQueue() throws EventException ;
	
	/**
	 * Get a copy of the current status queue as a list of beans, type T
	 * The list is ordered by submission time, not necessarily the ordering
	 * of the JMS queue.
	 * 
	 * @return
	 */
	public List<T> getStatusSet() throws EventException ;

	/**
	 * The string to define the queue for storing status of scans.
	 * 
	 * @return
	 */
	public String getStatusTopicName();
	
	/**
	 * The string to define the queue for storing status of scans.
	 * @param topic
	 * @throws EventException
	 */
	public void setStatusTopicName(String queueName) throws EventException;

	/**
	 * Set the consumer process to run for each job.
	 * @param process
	 * @throws Exception if the alive topic cannot be sent
	 */
	void setRunner(IProcessCreator<T> process) throws EventException ;
	
	
	/**
	 * Starts the consumer in new thread and return. Similar to Thread.start()
	 * You must set the runner before calling this method
	 * @throws Exception
	 */
	void start() throws EventException;
	
	/**
	 * Ask the consumer to stop 
	 * @throws EventException
	 */
	void stop() throws EventException;

	/**
	 * Awaits the start of the consumer. There are occasions
	 * when the consumer should start in its own thread but
	 * still provide the ability to await the startup process.
	 * 
	 * @throws Exception
	 */
	void awaitStart() throws InterruptedException;

	/**
	 * Starts the consumer and block. Similar to Thread.run()
	 * You must set the runner before calling this method
	 * @throws Exception
	 */
	void run() throws EventException;
	
	/**
	 * 
	 * @return the current active process which will run jobs 
	 */
	IProcessCreator<T> getRunner();
	
	/**
	 * The topic used to run commands like terminate the running process and get the consumer to stop.
	 * @return topic name
	 */
	public String getCommandTopicName();
	
	/**
	 * The topic used  to run commands terminate the running process and get the consumer to stop.
	 */
	public void setCommandTopicName(String commandTName);
	
    /**
     * The string UUID which denotes this consumer
     * @return
     */
	public UUID getConsumerId();
	
	/**
	 * 
	 * @return the current status of the consumer.
	 */
	default ConsumerStatus getConsumerStatus() {
		return ConsumerStatus.ALIVE;
	}
	
	public String getName();
	public void setName(String name);
		
	/**
	 * Call to disconnect all publishers and subscribers when the connection goes down.
	 * @throws EventException
	 */
	public void disconnect() throws EventException;

	/**
	 * 
	 * @return true if the consumer is active and actively running things from the queue.
	 */
	public boolean isActive();

	/**
	 * Durable consumers try to keep going when there are exceptions.
	 * @return
	 */
	public boolean isDurable();
	
	/**
	 * Durable consumers try to keep going when there are exceptions.
	 * @param durable
	 */
	public void setDurable(boolean durable);
	
	/**
	 * If the consumer should pause when it is started with
	 * jobs in the queue and wait until the user requires it to unpause.
	 * 
	 * NOTE: setPauseOnStartup(...) must be called before the consumer is started!
	 * 
	 * @return
	 */
	boolean isPauseOnStart();
	
	/**
	 * If the consumer should pause when it is started with
	 * jobs in the queue and wait until the user requires it to unpause.
	 * 
	 * NOTE: setPauseOnStartup(...) must be called before the consumer is started!
	 * 
	 * @param pauseOnStart
	 */
	void setPauseOnStart(boolean pauseOnStart);

}
