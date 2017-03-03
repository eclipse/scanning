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
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.event.queues.ServicesHolder;

/**
 * A response which returns the the ID of the job-queue configured in the 
 * {@link IQueueService} that the parent responder is associated with. 
 * 
 * @author Michael Wharmby
 *
 */
public class GetServerStringResponse extends AbstractQueueResponseProcess {
	
	private IQueueControllerService queueControl;
	
	public GetServerStringResponse(QueueRequest requestBean, IPublisher<QueueRequest> reponseBroadcaster) {
		super(requestBean, reponseBroadcaster);
		queueControl = ServicesHolder.getQueueControllerService();
	}

	@Override
	public QueueRequest process(QueueRequest request) throws EventException {
		switch (request.getRequestType()) {
		case COMMAND_SET:		request.setCommandSetName(queueControl.getCommandSetName());
								break;
		case COMMAND_TOPIC:		request.setCommandTopicName(queueControl.getCommandTopicName());
								break;
		case HEARTBEAT_TOPIC:	request.setHeartbeatTopicName(queueControl.getHeartbeatTopicName());
								break;
		case JOB_QUEUE_ID:		request.setJobQueueID(queueControl.getJobQueueID());
								break;
		default: throw new EventException("Unsupported QueueRequestType");
		}
		return request;
	}
}
