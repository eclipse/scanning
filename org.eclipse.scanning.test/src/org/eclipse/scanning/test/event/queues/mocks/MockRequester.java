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
package org.eclipse.scanning.test.event.queues.mocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.ResponseConfiguration;
import org.eclipse.scanning.api.event.core.ResponseConfiguration.ResponseWaiter;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.Queue;
import org.eclipse.scanning.event.remote._Queue;

public class MockRequester<T extends IdBean> implements IRequester<T> {
	
	private QueueRequest reply;
	private List<T> replies = new ArrayList<>(); 
	private String qServCmdSet, qServCmdTop, qServHeartTop, qServjqID;
	private Map<String, Status> expectedStatuses;

	public MockRequester() {
		expectedStatuses = new HashMap<>();
	}
	
	public MockRequester(String qRoot) {
		this();
		qServCmdSet = qRoot+IQueue.COMMAND_SET_SUFFIX;
		qServCmdTop = qRoot+IQueue.COMMAND_TOPIC_SUFFIX;
		qServHeartTop = qRoot+IQueue.HEARTBEAT_TOPIC_SUFFIX;
		qServjqID = qRoot+IQueueService.JOB_QUEUE_SUFFIX;
	}
	
	@Override
	public void setRequestTopic(String requestTopic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getRequestTopic() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setResponseTopic(String responseTopic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getResponseTopic() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnect() throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public URI getUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IEventConnectorService getConnectorService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDisconnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResponseConfiguration getResponseConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setResponseConfiguration(ResponseConfiguration rc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTimeout(long time, TimeUnit unit) {
		// TODO Auto-generated method stub
		
	}
/*
 * //Values for request construction
	private QueueRequestType requestType;
	private String beanID;//Unique ID of bean to interrogate
	private String queueID;//ID of queue as set in IQueueService where beanID should be found
	private boolean startQueueService = false;
	private boolean stopQueueService = false;
	private boolean forceStop = false;
	
	//Values to be completed by responses
	private String jobQueueID;//jobQueue of IQueueService
	private String commandSetName, commandTopicName, heartbeatTopicName;//QueueService configured destinations
	private Status beanStatus;//State of a the bean in the queue
	private IQueue<? extends Queueable> queue;
 */
	@Override
	public T post(T request) throws EventException, InterruptedException {
		if (request instanceof QueueRequest) {
			QueueRequest queueReq = (QueueRequest) request;
			
			reply = new QueueRequest();
			//Copy the request values across
			reply.setRequestType(queueReq.getRequestType());
			reply.setBeanID(queueReq.getBeanID());
			reply.setQueueID(queueReq.getQueueID());
			reply.setStartQueueService(queueReq.isStartQueueService());
			reply.setStopQueueService(queueReq.isStopQueueService());
			reply.setForceStop(queueReq.isForceStop());
			
			
			switch (queueReq.getRequestType()) {
			case SERVICE_START_STOP:
				//Nothing to do
				break;
			case QUEUE:
				IQueue<Queueable> mockQueue = new Queue<>(reply.getQueueID(), null);
				reply.setCommandSetName(mockQueue.getCommandSetName());
				reply.setCommandTopicName(mockQueue.getCommandTopicName());
				reply.setHeartbeatTopicName(mockQueue.getHeartbeatTopicName());
				
				reply.setQueueID(mockQueue.getQueueID());
				reply.setStatus(mockQueue.getStatus());
				
				reply.setStatusSetName(mockQueue.getStatusSetName());
				reply.setStatusTopicName(mockQueue.getStatusTopicName());
				reply.setSubmissionQueueName(mockQueue.getSubmissionQueueName());
				reply.setConsumerId(mockQueue.getConsumerID());
				break;
			case COMMAND_SET:
				reply.setCommandSetName(qServCmdSet);
				break;
			case COMMAND_TOPIC:
				reply.setCommandTopicName(qServCmdTop);
				break;
			case JOB_QUEUE_ID:
				reply.setJobQueueID(qServjqID);
				break;
			case HEARTBEAT_TOPIC:
				reply.setHeartbeatTopicName(qServHeartTop);
				break;
			case BEAN_STATUS:
				String beanIDQueueID = queueReq.getBeanID()+queueReq.getQueueID();
				if (expectedStatuses.containsKey(beanIDQueueID)) {
					reply.setBeanStatus(expectedStatuses.get(beanIDQueueID));
					break;
				}
				throw new EventException("Bean not found");
			default:
				throw new EventException("Request type "+queueReq.getRequestType()+" not recognised");
			}
		}
		replies.add((T) reply);
		
		return (T) reply;
	}

	@Override
	public T post(T request, ResponseWaiter waiter) throws EventException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public T getReply() {
		assert (replies.size() > 0);
		return replies.get(replies.size()-1);
	}
	
	public List<T> getReplies() {
		return replies;
	}
	
	public void clearReplies() {
		replies.clear();
	}
	
	public String getQueueServiceCommandTopic() {
		return qServCmdTop;
	}
	
	public void setExpectedStatus(String beanIDQueueID, Status expected) {
		expectedStatuses.put(beanIDQueueID, expected);
	}

}
