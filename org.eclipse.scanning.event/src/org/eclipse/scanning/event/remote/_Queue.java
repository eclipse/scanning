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
package org.eclipse.scanning.event.remote;

import java.net.URI;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.QueueStatus;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;

/**
 * Remote {@link IQueue} class containing all the configuration information on 
 * a real queue object, but without anything to directly allow control. This 
 * should be passed back to a client through a remote request. Configuration 
 * information inside this class allows the client to interact remotely with 
 * the queue.
 * 
 * Getters & setters which would control resources which are unavailable in 
 * the remote case will throw {@link IllegalArgumentException}s.
 * 
 * @author Michael Wharmby
 *
 * @param <T> Bean extending the {@link Queueable} super-type.
 */
public class _Queue<T extends Queueable> implements IQueue<T> {

	private final String queueID;
	private final URI uri;
	
	private final String submissionQueueName, statusSetName, statusTopicName, 
	heartbeatTopicName, commandSetName, commandTopicName;
	
	private final QueueStatus status;
	
	private final UUID consumerID;
	
	/**
	 * Constructor takes a real {@link IQueue} object from the 
	 * {@link IQueueService} as it's only argument. All of the configuration 
	 * parameters are extracted from this realQueue object.
	 * 
	 * @param realQueue {@link IQueue} which will be source for the config.
	 */
	public _Queue(URI uri, QueueRequest reply) {
		commandSetName = reply.getCommandSetName();
		commandTopicName = reply.getCommandTopicName();
		heartbeatTopicName = reply.getHeartbeatTopicName();
		queueID = reply.getQueueID();
		status = reply.getStatus();
		statusSetName = reply.getStatusSetName();
		statusTopicName = reply.getStatusTopicName();
		submissionQueueName = reply.getSubmissionQueueName();
		this.uri = uri;
		
		this.consumerID = reply.getConsumerId();
	}
	
	@Override
	public String getQueueID() {
		return queueID;
	}

	@Override
	public void start() throws EventException {
		throw new IllegalArgumentException("Cannot use remote object to start queue.");
	}

	@Override
	public void stop() throws EventException {
		throw new IllegalArgumentException("Cannot use remote object to stop queue.");
	}

	@Override
	public void disconnect() throws EventException {
		throw new IllegalArgumentException("Cannot use remote object to disconnect queue.");
	}

	@Override
	public IConsumer<T> getConsumer() {
		throw new IllegalArgumentException("No consumer on remote object.");
	}

	@Override
	public UUID getConsumerID() {
		return consumerID;
	}

	@Override
	public QueueStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(QueueStatus status) {
		throw new IllegalArgumentException("Cannot set QueueStatus on remote object.");
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
		throw new IllegalArgumentException("Cannot use remote object to clear queues.");
	}

}
