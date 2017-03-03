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
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * IQueue object contain all the information necessary to interact with and 
 * control a queue within the {@link IQueueService}. IQueue objects contain an
 * {@link IConsumer} to run the queue and all of it's configuration, plus the
 * current {@link QueueStatus}.
 * 
 * @author Michael Wharmby
 *
 * @param <T> bean object extending {@link Queueable} which is passed around 
 *            this queue. 
 */
public interface IQueue<T extends Queueable> {
	
	/**
	 * Suffixes to be appended to the names of the destinations within a 
	 * concrete instance of IQueue
	 */
	public static final String SUBMISSION_QUEUE_SUFFIX = ".submission.queue";
	public static final String STATUS_SET_SUFFIX = ".status.queue";
	public static final String STATUS_TOPIC_SUFFIX = ".status.topic";
	public static final String HEARTBEAT_TOPIC_SUFFIX = ".heartbeat.topic";
	public static final String COMMAND_SET_SUFFIX = ".command.queue";
	public static final String COMMAND_TOPIC_SUFFIX = ".command.topic";
	
	/**
	 * Get the unique name this queue is registered with in the 
	 * {@link IQueueServiceNew}.
	 * 
	 * @return String Unique queue name.
	 */
	public String getQueueID();
	
	/**
	 * Start the Queue consumer.
	 * 
	 * @throws EventException
	 */
	public void start() throws EventException;
	
	/**
	 * Stop the running consumer.
	 * 
	 * @throws EventException
	 */
	public void stop() throws EventException;
	
	/**
	 * Disconnect the consumer (and the heartbeat monitor).
	 * 
	 * @throws EventException if cannot access services.
	 */
	public void disconnect() throws EventException;
	
	/**
	 * Return unique consumer responsible for this queue. Consumer should 
	 * operate on objects (T) extending {@link IQueueable}.
	 * 
	 * @return IConsumer Queue consumer.
	 */
	public IConsumer<T> getConsumer();

	/**
	 * Return the unique UUID of this queue's consumer.
	 * 
	 * @return UUID unique to the queue consumer
	 */
	public default UUID getConsumerID() {
		return getConsumer().getConsumerId();
	}
	
	/**
	 * Report the current running state of the Queue.
	 * 
	 * @return {@link QueueStatus}
	 */
	public QueueStatus getStatus();
	
	/**
	 * Change the current running state of the Queue.
	 * 
	 * @param new {@link QueueStatus}
	 */
	public void setStatus(QueueStatus status);
	
	/**
	 * Return the submission queue name.
	 * 
	 * @Return String submission queue name.
	 */
	public String getSubmissionQueueName();

	/**
	 * Return the status set name.
	 * 
	 * @return String status queue name.
	 */
	public String getStatusSetName();

	/**
	 * Return the status topic name.
	 * 
	 * @Return String submission queue name.
	 */
	public String getStatusTopicName();

	/**
	 * Return the topic name where this queue publishes heartbeats.
	 * 
	 * @return String heartbeat topic name.
	 */
	public String getHeartbeatTopicName();

	/**
	 * Return the name of the set where commands sent to this queue are
	 * sent.
	 * 
	 * @return String command queue name.
	 */
	public String getCommandSetName();

	/**
	 * Return the topic name where commands can be passed to this queue.
	 * 
	 * @return String command topic name.
	 */
	public String getCommandTopicName();
	
	/**
	 * Return the URI of the broker where the consumer is running.
	 * 
	 * @return URI of the broker.
	 */
	public URI getURI();
	
	/**
	 * Clear both the submission and the status queues of any pending jobs
	 * 
	 * @throws EventException if cannot access consumer.
	 */
	public boolean clearQueues() throws EventException;

}
