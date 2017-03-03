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
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.ServicesHolder;

/**
 * A response which searches in both the submission queue and status set of an 
 * {@link IConsumer} for a bean with a given unique ID and then returns the
 * {@link Status} of the bean. The {@link IConsumer} to search is that 
 * associated with the {@link IQueue} with the queueID found on the request. 
 * 
 * @author Michael Wharmby
 *
 */
public class BeanStatusReponse extends AbstractQueueResponseProcess {
	
	private IQueueService queueService;
	
	public BeanStatusReponse(QueueRequest requestBean, IPublisher<QueueRequest> reponseBroadcaster) {
		super(requestBean, reponseBroadcaster);
		queueService = ServicesHolder.getQueueService();
	}
	
	@Override
	public QueueRequest process(QueueRequest request) throws EventException {
		//Get request details
		String beanID = request.getBeanID();
		String queueID = request.getQueueID();
		
		//Get the queue consumer requested
		IConsumer<? extends Queueable> consumer = queueService.getQueue(queueID).getConsumer();
		
		BeanStatusFinder<? extends Queueable> statusFinder = new BeanStatusFinder<>(beanID, consumer);
		Status foundStatus = statusFinder.find();
		
		//We've got the status, put it into the reply & return
		request.setBeanStatus(foundStatus);
		return request;
	}

}
