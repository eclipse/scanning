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
package org.eclipse.scanning.event.queues.remote;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.remote._Queue;

/**
 * A response which returns a remote {@link IQueue} ({@link _Queue}) object 
 * containing all of the configuration options of a real queue from the 
 * {@link IQueueService}. The queue which will have its configuration returned 
 * is determined from the queueID field of the request.
 *   
 * @author Michael Wharmby
 *
 */
public class GetQueueResponse extends AbstractQueueResponseProcess {
	
	private IQueueService queueService;

	protected GetQueueResponse(QueueRequest requestBean, IPublisher<QueueRequest> reponseBroadcaster) {
		super(requestBean, reponseBroadcaster);
		queueService = ServicesHolder.getQueueService();
	}

	@Override
	public QueueRequest process(QueueRequest request) throws EventException {
		//Get the requested queue & make a local copy with it
		IQueue<? extends Queueable> realQueue = queueService.getQueue(request.getQueueID());
		
		//Send the remote copy back to the requester
		request.setCommandSetName(realQueue.getCommandSetName());
		request.setCommandTopicName(realQueue.getCommandTopicName());
		request.setHeartbeatTopicName(realQueue.getHeartbeatTopicName());
		
		request.setQueueID(realQueue.getQueueID());
		request.setStatus(realQueue.getStatus());
		
		request.setStatusSetName(realQueue.getStatusSetName());
		request.setStatusTopicName(realQueue.getStatusTopicName());
		request.setSubmissionQueueName(realQueue.getSubmissionQueueName());
		request.setConsumerId(realQueue.getConsumerID());

		return request;
	}

}
