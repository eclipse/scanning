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
import org.eclipse.scanning.api.event.core.IResponseCreator;
import org.eclipse.scanning.api.event.core.IRequestHandler;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;

/**
 * Class responsible for creating the {@link AbstractQueueRequestHandler} objects.
 *
 * @author Michael Wharmby
 *
 */
public class QueueResponseCreator implements IResponseCreator<QueueRequest> {

	@Override
	public IRequestHandler<QueueRequest> createResponder(QueueRequest requestBean, IPublisher<QueueRequest> responseBroadcaster)
			throws EventException {
		//Decide which strategy to use to interrogate the QueueService
		switch (requestBean.getRequestType()) {
		case BEAN_STATUS:
			return new BeanStatusRequestHandler(requestBean, responseBroadcaster);

		case COMMAND_SET:	// Options COMMAND_SET to JOB_QUEUE_ID all use the GetServerStringResponse
		case COMMAND_TOPIC:
		case HEARTBEAT_TOPIC:
		case JOB_QUEUE_ID:
			return new GetServerStringRequestHandler(requestBean, responseBroadcaster);

		case QUEUE:
			return new GetQueueRequestHandler(requestBean, responseBroadcaster);
		case SERVICE_START_STOP:
			return new StartStopRequestHandler(requestBean, responseBroadcaster);

		default: throw new EventException("Unknown queue response request");
		}
	}

}
