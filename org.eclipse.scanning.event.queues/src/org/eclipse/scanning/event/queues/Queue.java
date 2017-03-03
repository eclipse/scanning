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
package org.eclipse.scanning.event.queues;

import java.net.URI;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.QueueStatus;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * Queue is a concrete implementation of {@link IQueue}. It holds details of 
 * the consumer and of the consumer/queue configuration needed for the 
 * {@link IQueueService} to control the queue.
 * 
 * @author Michael Wharmby
 *
 * @param <T> Base type of atom/bean operated on by the queue, e.g. 
 *            {@link QueueAtom} or {@QueueBean}.
 */
public class Queue<T extends Queueable> implements IQueue<T> {

	private final String queueID;
	private final URI uri;
	private final IConsumer<T> consumer;

	private final String submissionQueueName, statusSetName, statusTopicName, 
	heartbeatTopicName, commandSetName, commandTopicName;

	private QueueStatus status;

	/**
	 * Constructs a Queue object from minimal arguments. Names of heartbeat 
	 * topic and commmand set/topic will be automatically generated, based on 
	 * the suffixes in {@link IQueue}.
	 * 
	 * @param queueID String name of queue.
	 * @param uri URI of the broker.
	 * @throws EventException When consumer cannot be created.
	 */
	public Queue(String queueID, URI uri) throws EventException {
		this(queueID, uri, queueID+IQueue.HEARTBEAT_TOPIC_SUFFIX);
	}

	/**
	 * Constructs a Queue with heartbeats published to a specific destination. 
	 * Command set/topics will be automatically generated, based on  the 
	 * suffixes in {@link IQueue}.
	 * 
	 * @param queueID String name of queue.
	 * @param uri URI of the broker
	 * @param heartbeatTopicName String topic name where heartbeats published.
	 * @throws EventException When consumer cannot be created.
	 */
	public Queue(String queueID, URI uri, String heartbeatTopicName) throws EventException {
		this(queueID, uri, heartbeatTopicName, queueID+IQueue.COMMAND_SET_SUFFIX, 
				queueID+IQueue.COMMAND_TOPIC_SUFFIX);
	}

	/**
	 * Constructs a Queue with heartbeats & commands published to specific 
	 * destinations.
	 *  
	 * @param queueID String name of queue.
	 * @param uri URI of the broker
	 * @param heartbeatTopicName String topic name where heartbeats published.
	 * @param commandSetName String queue name where consumer commands will be 
	 *                       stored.
	 * @param commandTopicName String topic name where commands will be 
	 *                         published.
	 * @throws EventException When consumer cannot be created.
	 */
	public Queue(String queueID, URI uri, String heartbeatTopicName,
			String commandSetName, String commandTopicName) throws EventException {
		this.queueID = queueID;
		this.uri = uri;

		//Record all the destination paths
		submissionQueueName = queueID+IQueue.SUBMISSION_QUEUE_SUFFIX;
		statusSetName = queueID+IQueue.STATUS_SET_SUFFIX;
		statusTopicName = queueID+IQueue.STATUS_TOPIC_SUFFIX; 
		this.heartbeatTopicName = heartbeatTopicName;
		this.commandSetName = commandSetName;
		this.commandTopicName = commandTopicName;

		IEventService eventService = ServicesHolder.getEventService();
		consumer = eventService.createConsumer(this.uri, getSubmissionQueueName(),
				getStatusSetName(), getStatusTopicName(), getHeartbeatTopicName(),
				getCommandTopicName());
		consumer.setName(this.queueID);
		consumer.setRunner(new QueueProcessCreator<T>(true));

		status = QueueStatus.INITIALISED;
	}

	@Override
	public String getQueueID() {
		return queueID;
	}

	@Override
	public void start() throws EventException {
		consumer.start();
		status = QueueStatus.STARTED;
	}

	@Override
	public void stop() throws EventException {
		QueueStatus previousState = status;
		status = QueueStatus.STOPPING;
		
		try {
			//If the consumer has been killed, we still need to set status STOPPED;
			//If it's still active, then we need to push the stop button.
			if (consumer.isActive()) {
				consumer.stop();
			}
			status = QueueStatus.STOPPED;
		} catch (EventException evEx) {
			status = previousState;
			throw new EventException("Failed to stop queue", evEx);
		}
	}

	@Override
	public void disconnect() throws EventException {
		consumer.disconnect();
		status = QueueStatus.DISPOSED;
	}

	@Override
	public IConsumer<T> getConsumer() {
		return consumer;
	}

	@Override
	public QueueStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(QueueStatus status) {
		this.status = status;
	}

	@Override
	public String getSubmissionQueueName() {
		return submissionQueueName;
	}

	@Override
	public String getStatusSetName() {
		return statusSetName;
	}

	@Override
	public String getStatusTopicName() {
		return statusTopicName;
	}
 
	@Override
	public String getHeartbeatTopicName() {
		return heartbeatTopicName;
	}

	@Override
	public String getCommandSetName() {
		return commandSetName;
	}

	@Override
	public String getCommandTopicName() {
		return commandTopicName;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public boolean clearQueues() throws EventException {
		consumer.clearQueue(getSubmissionQueueName());
		consumer.clearQueue(getStatusSetName());

		if (consumer.getStatusSet().size() == 0 && consumer.getSubmissionQueue().size() == 0) return true;
		else return false;
	}

}
