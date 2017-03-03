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
import java.util.EventListener;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * Connector interface which allows {@link IQueueControllerService} classes to 
 * make use of the same underlying code to interact with the 
 * {@link IEventService} infrastructure. The connector should abstract the 
 * {@link IEventService} from the {@link IQueueControllerService} and should 
 * itself not have a dependency on the {@link IQueueService}.
 * 
 * @author Michael Wharmby
 *
 */
public interface IQueueControllerEventConnector {
	
	/**
	 * Submit a bean in to an {@link IConsumer} submission queue for 
	 * processing.
	 * 
	 * @param bean T ({@link Queueable}) to be submitted.
	 * @param submitQueueName String name of submission queue.
	 * @throws EventException - if submission is rejected.
	 */
	public <T extends Queueable> void submit(T bean, String submitQueueName) throws EventException;

	/**
	 * Remove a bean from an {@link IConsumer} submission queue prior to it 
	 * starting processing.
	 * 
	 * @param bean T ({@link Queueable}) to be removed.
	 * @param submitQueueName String name of submission queue.
	 * @return true if removal successful.
	 * @throws EventException - if removal failed.
	 */
	public <T extends Queueable> boolean remove(T bean, String submitQueueName) throws EventException;
	
	/**
	 * Move a single bean a specified number of places up (positive move) or 
	 * down (negative move) the submission queue. This must be done prior to 
	 * processing beginning.
	 * 
	 * @param bean T ({@link Queueable}) to be reordered.
	 * @param move positive/negative integer number of places to move in queue.
	 * @param submitQueueName String name of submission queue.
	 * @return true if move successful
	 * @throws EventException - if bean couldn't be reordered
	 */
	public <T extends Queueable> boolean reorder(T bean, int move, String submitQueueName) throws EventException;
	
	/**
	 * Publish a bean to a given topic. Typically the bean would have a new 
	 * {@link Status}, which would instruct the {@link IConsumer} to change 
	 * how it is processing the bean. In this case the bean would be published 
	 * to the {@link IConsumer} status topic.
	 * 
	 * @param bean T ({@link Queueable}) whose processing is to be changed.
	 * @param TopicName String name of topic to publish bean on.
	 * @throws EventException - if the bean could not be published on the 
	 *                          requested topic.
	 */
	public <T extends Queueable> void publishBean(T bean, String topicName) throws EventException;
	
	/**
	 * Publish a {@link ConsumerCommandBean} which will instruct the whole 
	 * {@link IConsumer} instance to change it's state (e.g. pause). The bean 
	 * should be published to the {@link IConsumer}'s command topic and should 
	 * have the ID of the consumer to be commanded set on the bean.
	 * 
	 * @param commandBean T ({@link ConsumerCommandBean}) with instruction for 
	 *        {@link IConsumer instance.
	 * @param commandTopicName String name of topic to publish bean on.
	 * @throws EventException - if the command bean could not be published on 
	 *                          the requested topic.
	 */
	public <T extends ConsumerCommandBean> void publishCommandBean(T commandBean, String commandTopicName) throws EventException;
	
	/**
	 * Create an {@link ISubscriber} instance configured to listen on the 
	 * status topic of an {@link IConsumer}.
	 * 
	 * @param statusTopicName String status topic of {@link IConsumer}.
	 * @return {@link ISubscriber} configured to listen to queueID.
	 */
	public <T extends EventListener> ISubscriber<T> createQueueSubscriber(String statusTopicName);
	
	/**
	 * Sets the {@link IEventService} instance which will be used by this 
	 * {@link IQueueControllerEventConnector} to create event infrastructure.
	 * 
	 * @param eventService {@link IEventService} to be used by connector.
	 */
	public void setEventService(IEventService eventService);
	
	/**
	 * Sets the URI of the server running the {@link IQueueService} and 
	 * associated infrastructure.
	 * 
	 * @param uri URI of {@IQueueService} server.
	 */
	public void setUri(URI uri);
}
