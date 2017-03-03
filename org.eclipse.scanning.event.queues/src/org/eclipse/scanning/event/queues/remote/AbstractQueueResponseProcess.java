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

import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.event.queues.remote.QueueRequestType;

/**
 * Process to provide remote access to configuration information and data 
 * stored in the {@link IQueueService}, with access through 
 * {@link IQueueControllerService}. Requests are received as 
 * {@link QueueRequest} beans, with a {@link QueueRequestType}. Based on this 
 * value, a class extending {@link AbstractQueueResponseProcess} is selected (through 
 * a strategy pattern) to populate the necessary fields in the request. 
 * Finally the {@link QueueRequest} is passed back to the parent 
 * {@link IResponder}. 
 * 
 * @author Michael Wharmby
 *
 */
public abstract class AbstractQueueResponseProcess implements IResponseProcess<QueueRequest> {
	
	private final QueueRequest requestBean;
	private final IPublisher<QueueRequest> reponseBroadcaster;
	
	protected AbstractQueueResponseProcess(QueueRequest requestBean, IPublisher<QueueRequest> reponseBroadcaster) {
		this.requestBean = requestBean;
		this.reponseBroadcaster = reponseBroadcaster;
	}

	@Override
	public QueueRequest getBean() {
		return requestBean;
	}

	@Override
	public IPublisher<QueueRequest> getPublisher() {
		return reponseBroadcaster;
	}
	
}
