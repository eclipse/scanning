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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.EventListener;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IQueueControllerEventConnector;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueControllerEventConnector implements IQueueControllerEventConnector {
	
	private static Logger logger = LoggerFactory.getLogger(QueueControllerEventConnector.class);
	
	private IEventService eventService;
	private URI uri;
	
	@Override
	public <T extends Queueable> void submit(T bean, String submitQueue) throws EventException {
		//Prepare the bean for submission
		bean.setStatus(Status.SUBMITTED);
		try {
			if (bean.getHostName() == null) {
				String localhostName = InetAddress.getLocalHost().getHostName();
				logger.trace("Hostname on received bean not set. Now set to '"+localhostName+"'.");
				bean.setHostName(localhostName);
			}
		} catch (UnknownHostException ex) {
			throw new EventException("Failed to set hostname on bean. " + ex.getMessage());
		}

		//Create a submitter and submit the bean
		ISubmitter<T> submitter = eventService.createSubmitter(uri, submitQueue);
		submitter.submit(bean);
		submitter.disconnect();
	}
	
	@Override
	public <T extends Queueable> boolean remove(T bean, String submitQueue) throws EventException {
		//Create a submitter and remove the bean
		ISubmitter<T> submitter = eventService.createSubmitter(uri, submitQueue);
		boolean result = submitter.remove(bean);
		submitter.disconnect();
		
		return result;
	}
	
	@Override
	public <T extends Queueable> boolean reorder(T bean, int move, String submitQueue) throws EventException {
		
		//Create a submitter and move bean by the given amount
		ISubmitter<T> submitter = eventService.createSubmitter(uri, submitQueue);
		boolean result = submitter.reorder(bean, move);
		submitter.disconnect();

		return result;
	}
	
	@Override
	public <T extends Queueable> void publishBean(T bean, String topicName) throws EventException {
		//Set up the publisher & publish the bean (with state set)
		IPublisher<T> commander = eventService.createPublisher(uri, topicName);
		commander.broadcast(bean);
		commander.disconnect();
	}
	
	@Override
	public <T extends ConsumerCommandBean> void publishCommandBean(T commandBean, String commandTopicName) throws EventException {
		IPublisher<T> commander = eventService.createPublisher(uri, commandTopicName);
		commander.broadcast(commandBean);
		commander.disconnect();
	}
	
	@Override
	public <T extends EventListener> ISubscriber<T> createQueueSubscriber(String statusTopicName) {
		return eventService.createSubscriber(uri, statusTopicName);
	}
	
	@Override
	public void setEventService(IEventService eventService) {
		this.eventService = eventService;
	}
	
	@Override
	public void setUri(URI uri) {
		this.uri = uri;
	}

}
