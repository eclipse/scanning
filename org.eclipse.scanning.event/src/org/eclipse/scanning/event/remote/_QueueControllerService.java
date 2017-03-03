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

import java.util.EventListener;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.core.ResponseConfiguration;
import org.eclipse.scanning.api.event.core.ResponseConfiguration.ResponseType;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerEventConnector;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.event.queues.remote.QueueRequestType;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueControllerEventConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to allow access to all the controls of the {@link IQueueService} from 
 * a remote client. Calls to {@link IQueueService} are mediated by an 
 * {@link IRequester} instance (in contrast to the non-remote implementation 
 * of the {@link IQueueControllerService}). Calls to influence beans (e.g. 
 * submission/termination) are handled through the same {@link IEventService} 
 * classes.
 * 
 * @author Michael Wharmby
 *
 */
public class _QueueControllerService extends AbstractRemoteService implements IQueueControllerService {

	private static final Logger logger = LoggerFactory.getLogger(_QueueControllerService.class);

	private IQueueControllerEventConnector eventConnector;
	private IRequester<QueueRequest> requester;

	@Override
	public void init() throws EventException {
		//Configure the QueueController-EventService connector
		eventConnector = new QueueControllerEventConnector();
		eventConnector.setEventService(eservice);
		eventConnector.setUri(uri);

		//Set up requester to handle remote requests
		requester = eservice.createRequestor(uri, 
				IQueueService.QUEUE_REQUEST_TOPIC, 
				IQueueService.QUEUE_RESPONSE_TOPIC);
		requester.setResponseConfiguration(new ResponseConfiguration(ResponseType.ONE, 5000, TimeUnit.MILLISECONDS));
	}

	@Override
	public void disconnect() throws EventException {
		requester.disconnect();
		setDisconnected(true);
	}

	@Override
	public void startQueueService() throws EventException {
		//Create QueueRequest & set up to start service
		QueueRequest startRequest = new QueueRequest();
		startRequest.setRequestType(QueueRequestType.SERVICE_START_STOP);
		startRequest.setStartQueueService(true);
		try {
			requester.post(startRequest);
		} catch (InterruptedException e) {
			throw new EventException("Interrupted during attempt to start queue service");
		} catch (EventException evEx) {
			logger.error("Failed to start queue service with "+evEx.getMessage());
			throw evEx;
		}
	}

	@Override
	public void stopQueueService(boolean force) throws EventException {
		QueueRequest stopRequest = new QueueRequest();
		stopRequest.setRequestType(QueueRequestType.SERVICE_START_STOP);
		stopRequest.setStopQueueService(true);
		stopRequest.setForceStop(force);
		try {
			requester.post(stopRequest);
		} catch (InterruptedException e) {
			throw new EventException("Interrupted during attempt to start queue service");
		} catch (EventException evEx) {
			logger.error("Failed to stop queue service with "+evEx.getMessage());
			throw evEx;
		}
	}

	@Override
	public <T extends Queueable> void submit(T bean, String queueID) throws EventException {
		checkBeanType(bean, queueID);
		String submitQueueName = getQueue(queueID).getSubmissionQueueName();
		eventConnector.submit(bean, submitQueueName);
	}

	@Override
	public <T extends Queueable> void remove(T bean, String queueID) throws EventException {
		checkBeanType(bean, queueID);
		String submitQueueName = getQueue(queueID).getSubmissionQueueName();
		boolean success = eventConnector.remove(bean, submitQueueName);
		if (!success) {
			logger.error("Bean removal failed. Is it in the status set already?");
			throw new EventException("Bean removal failed.");
		}
	}

	@Override
	public <T extends Queueable> void reorder(T bean, int move, String queueID) throws EventException {
		checkBeanType(bean, queueID);
		String submitQueueName = getQueue(queueID).getSubmissionQueueName();
		boolean success = eventConnector.reorder(bean, move, submitQueueName);
		if (!success) {
			logger.error("Bean reordering failed. Is it in the status set already?");
			throw new EventException("Bean reordering failed.");
		}
	}

	@Override
	public <T extends Queueable> void pause(T bean, String queueID) throws EventException {
		//Determine if bean is the right type & in a pausable state
		checkBeanType(bean, queueID);
		Status beanState;
		try {
			String beanID = bean.getUniqueId();
			beanState = getBeanStatus(beanID, queueID);
		} catch (EventException evEx) {
			logger.error("Could not get state of bean in queue. Is it in queue '"+queueID+"'? "+evEx.getMessage());
			throw evEx;
		}
		if (beanState.isPaused()) {
			logger.error("Bean '"+bean.getName()+"' is already paused.");
			throw new IllegalStateException("Bean '"+bean.getName()+"' is already paused");
		} else if (beanState == Status.SUBMITTED) {
			logger.error("Bean is submitted but not being processed. Cannot pause.");
			throw new IllegalStateException("Cannot pause a bean with SUBMITTED status");
		}

		//The bean is pausable. Get the status topic name and publish the bean
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		bean.setStatus(Status.REQUEST_PAUSE);
		eventConnector.publishBean(bean, statusTopicName);
	}

	@Override
	public <T extends Queueable> void resume(T bean, String queueID) throws EventException {
		//Determine if bean is the right type & in a resumable state
		checkBeanType(bean, queueID);
		Status beanState;
		try {
			String beanID = bean.getUniqueId();
			beanState = getBeanStatus(beanID, queueID);
		} catch (EventException evEx) {
			logger.error("Could not get state of bean in queue. Is it in queue '"+queueID+"'? "+evEx.getMessage());
			throw evEx;
		}
		if (beanState.isResumed() || beanState.isRunning()) {
			logger.error("Bean '"+bean.getName()+"' is already resumed/running.");
			throw new IllegalStateException("Bean '"+bean.getName()+"' is already resumed/running");
		} else if (beanState == Status.SUBMITTED) {
			logger.error("Bean is submitted but not being processed. Cannot resume.");
			throw new IllegalStateException("Cannot resume a bean with SUBMITTED status");
		}

		//The bean is resumable. Get the status topic name and publish the bean
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		bean.setStatus(Status.REQUEST_RESUME);
		eventConnector.publishBean(bean, statusTopicName);
	}

	@Override
	public <T extends Queueable> void terminate(T bean, String queueID) throws EventException {
		//Determine if bean is the right type & in a terminatable state
		checkBeanType(bean, queueID);
		Status beanState;
		try {
			String beanID = bean.getUniqueId();
			beanState = getBeanStatus(beanID, queueID);
		} catch (EventException evEx) {
			logger.error("Could not get state of bean in queue. Is it in queue '"+queueID+"'? "+evEx.getMessage());
			throw evEx;
		}
		if (beanState.isTerminated()) {
			logger.error("Bean '"+bean.getName()+"' is already terminated.");
			throw new IllegalStateException("Bean '"+bean.getName()+"' is already terminated");
		} else if (beanState == Status.SUBMITTED) {
			logger.warn("Bean is submitted but not being processed. Bean will be removed, rather than terminated.");
			remove(bean, queueID);
			return;
		}

		//The bean is terminatable. Get the status topic name and publish the bean
		String statusTopicName = getQueue(queueID).getStatusTopicName();
		bean.setStatus(Status.REQUEST_TERMINATE);
		eventConnector.publishBean(bean, statusTopicName);
	}

	@Override
	public void pauseQueue(String queueID) throws EventException {
		//We need to get the consumerID of the remote queue...
		UUID consumerId = getQueue(queueID).getConsumerID();

		//Create pausenator configured for the target queueID & publish it.
		PauseBean pausenator = new PauseBean();
		pausenator.setConsumerId(consumerId);
		pausenator.setPause(true);
		eventConnector.publishCommandBean(pausenator, getCommandTopicName());
	}

	@Override
	public void resumeQueue(String queueID) throws EventException {
		//We need to get the consumerID of the remote queue...
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
	public String getCommandSetName() throws EventException {
		QueueRequest query = new QueueRequest(), reply;
		query.setRequestType(QueueRequestType.COMMAND_SET);
		reply = sendQuery(query);
		return reply.getCommandSetName();
	}

	@Override
	public String getCommandTopicName() throws EventException {
		QueueRequest query = new QueueRequest(), reply;
		query.setRequestType(QueueRequestType.COMMAND_TOPIC);
		reply = sendQuery(query);
		return reply.getCommandTopicName();
	}

	@Override
	public String getHeartbeatTopicName() throws EventException {
		QueueRequest query = new QueueRequest(), reply;
		query.setRequestType(QueueRequestType.HEARTBEAT_TOPIC);
		reply = sendQuery(query);
		return reply.getHeartbeatTopicName();
	}


	@Override
	public String getJobQueueID() throws EventException {
		QueueRequest query = new QueueRequest(), reply;
		query.setRequestType(QueueRequestType.JOB_QUEUE_ID);
		reply = sendQuery(query);
		return reply.getJobQueueID();
	}

	@Override
	public IQueue<? extends Queueable> getQueue(String queueID) throws EventException {
		QueueRequest query = new QueueRequest(), reply;
		query.setRequestType(QueueRequestType.QUEUE);
		query.setQueueID(queueID);
		reply = sendQuery(query);
		return new _Queue<>(uri, reply);
	}

	@Override
	public Status getBeanStatus(String beanID, String queueID) throws EventException {
		QueueRequest stateQuery = new QueueRequest(), stateReply;
		stateQuery.setRequestType(QueueRequestType.BEAN_STATUS);
		stateQuery.setBeanID(beanID);
		stateQuery.setQueueID(queueID);
		stateReply = sendQuery(stateQuery);
		return stateReply.getBeanStatus();
	}
	
	private <T extends Queueable> void checkBeanType(T bean, String queueID) throws EventException {
		//Check that the bean is the right type for the queue
		QueueRequest jqQuery = new QueueRequest(), jqReply;
		jqQuery.setRequestType(QueueRequestType.JOB_QUEUE_ID);
		jqReply = sendQuery(jqQuery);
		if (queueID.equals(jqReply.getJobQueueID())) {
			if (bean instanceof QueueBean) return;
		} else {
			if (bean instanceof QueueAtom) return;
		}
		logger.error("Bean type ("+bean.getClass().getSimpleName()+") not supported by queue "+queueID);
		throw new EventException("Bean is wrong type for given queueID"); 
	}

	/**
	 * Covenience method to post requests. Avoids repeated try/catch blocks.
	 */
	private QueueRequest sendQuery(QueueRequest query) throws EventException {
		try {
			return requester.post(query);
		} catch (InterruptedException iEx) {
			throw new EventException("Interrupted while waiting for a reply from remote queue service");
		}
	}


}
