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

import java.util.EventListener;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IQueueControllerEventConnector;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.remote.BeanStatusFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class QueueControllerService implements IQueueService, IQueueControllerService {

	private static final Logger logger = LoggerFactory.getLogger(QueueControllerService.class);

	private IQueueControllerEventConnector eventConnector;
	private ExceptionHandler resultHandler;

	protected boolean init = false;

	static {
		System.out.println("Created " + IQueueControllerService.class.getSimpleName());
	}

	/**
	 * No argument constructor for OSGi
	 */
	public QueueControllerService() {

	}

	@Override
	public void init() throws EventException {
		//Configure the QueueController-EventService connector
		eventConnector = new QueueControllerEventConnector();
		eventConnector.setEventService(ServicesHolder.getEventService());
		eventConnector.setUri(getURI());

		resultHandler = new ExceptionHandler() {

			@Override
			public Logger getLogger() {
				return logger;
			}

		};

		init = true;
	}

	@Override
	public void startQueueService() throws EventException {
		if (!init) init();
		start();
	}


	@Override
	public void stopQueueService(boolean force) throws EventException {
		stop(force);
	}

	@Override
	public <T extends Queueable> void submit(T bean, String queueID) throws EventException {
		checkBeanType(bean, queueID);
		String submitQueue = getQueue(queueID).getSubmissionQueueName();
		eventConnector.submit(bean, submitQueue);
	}

	@Override
	public <T extends Queueable> void remove(T bean, String queueID) throws EventException {
		checkBeanType(bean, queueID);
		String submitQueueName = getQueue(queueID).getSubmissionQueueName();
		boolean success = eventConnector.remove(bean, submitQueueName);

		resultHandler.handleOutcome(success, "remove", bean);
	}

	@Override
	public <T extends Queueable> void reorder(T bean, int move, String queueID) throws EventException {
		checkBeanType(bean, queueID);
		String submitQueueName = getQueue(queueID).getSubmissionQueueName();
		boolean success = eventConnector.reorder(bean, move, submitQueueName);

		resultHandler.handleOutcome(success, "reordering", bean);
	}

	@Override
	public <T extends Queueable> void pause(T bean, String queueID) throws EventException {
		//Determine if bean is the right type & in a pausable state
		checkBeanType(bean, queueID);
		Status beanState = attemptBeanStatusCheck(bean, queueID);
		resultHandler.alreadyAtState(beanState.isPaused(), "pause", bean, beanState);

		//The bean is pausable. Get the status topic name and publish the bean
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		bean.setStatus(Status.REQUEST_PAUSE);
		eventConnector.publishBean(bean, statusTopicName);
	}

	@Override
	public <T extends Queueable> void resume(T bean, String queueID) throws EventException {
		//Determine if bean is the right type & in a resumable state
		checkBeanType(bean, queueID);
		Status beanState = attemptBeanStatusCheck(bean, queueID);
		resultHandler.alreadyAtState(beanState.isResumed() || beanState.isRunning(), "resume", bean, beanState);

		//The bean is resumable. Get the status topic name and publish the bean
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		bean.setStatus(Status.REQUEST_RESUME);
		eventConnector.publishBean(bean, statusTopicName);
	}

	@Override
	public <T extends Queueable> void terminate(T bean, String queueID) throws EventException {
		//Determine if bean is the right type & in a terminatable state
		checkBeanType(bean, queueID);
		Status beanState = attemptBeanStatusCheck(bean, queueID);
		resultHandler.alreadyAtState(beanState.isTerminated(), "terminate", bean, beanState);

		//The bean is terminatable. Get the status topic name and publish the bean
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		bean.setStatus(Status.REQUEST_TERMINATE);
		eventConnector.publishBean(bean, statusTopicName);
	}

	@Override
	public void pauseQueue(String queueID) throws EventException {
		//We need to get the consumerID of the queue...
		UUID consumerId = getQueue(queueID).getConsumerID();

		//Create pausenator configured for the target queueID & publish it.
		PauseBean pausenator = new PauseBean();
		pausenator.setConsumerId(consumerId);
		pausenator.setPause(true);
		eventConnector.publishCommandBean(pausenator, getCommandTopicName());
	}

	@Override
	public void resumeQueue(String queueID) throws EventException {
		//We need to get the consumerID of the queue...
		UUID consumerId = getQueue(queueID).getConsumerID();

		//Create pausenator configured for the target queueID & publish it.
		PauseBean pausenator = new PauseBean();
		pausenator.setConsumerId(consumerId);
		pausenator.setPause(false);
		eventConnector.publishCommandBean(pausenator, getCommandTopicName());
	}

	@Override
	public void killQueue(String queueID, boolean disconnect, boolean restart, boolean exitProcess)
			throws EventException {
		//We need to get the consumerID of the remote queue...
		UUID consumerId = getQueue(queueID).getConsumerID();

		//Configure the killenator as requested and broadcast it
		KillBean killenator = new KillBean();
		killenator.setConsumerId(consumerId);
		killenator.setDisconnect(disconnect);
		killenator.setRestart(restart);
		killenator.setExitProcess(exitProcess);
		eventConnector.publishCommandBean(killenator, getCommandTopicName());
	}

	@Override
	public <T extends EventListener> ISubscriber<T> createQueueSubscriber(String queueID) throws EventException {
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		return eventConnector.createQueueSubscriber(statusTopicName);
	}

	@Override
	public Status getBeanStatus(String beanID, String queueID) throws EventException {
		IConsumer<? extends Queueable> consumer = getQueue(queueID).getConsumer();
		BeanStatusFinder<? extends Queueable> statusFinder = new BeanStatusFinder<>(beanID, consumer);
		return statusFinder.find();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Convenience check that the bean we're going to pass to the consumer
	 * will be the right type, based on the job-queue accepting only
	 * {@link QueueBean}s & the active-queue only {@link QueueAtom}s.
	 *
	 * @throws EventException if the bean is the wrong type.
	 */
	private <T extends Queueable> void checkBeanType(T bean, String queueID) throws EventException {
		//Check that the bean is the right type for the queue
		if (queueID.equals(getJobQueueID())) {
			if (bean instanceof QueueBean) return;
		} else {
			if (bean instanceof QueueAtom) return;
		}
		logger.error("Bean type ("+bean.getClass().getSimpleName()+") not supported by queue "+queueID);
		throw new EventException("Bean type ("+bean.getClass().getSimpleName()+") not supported by queue with given queueID");
	}

}
