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
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IQueueReader;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;

public class MockEventService implements IEventService {
	
	private Map<String, MockConsumer<? extends StatusBean>> mockConsumers = new HashMap<>(); //Submit Q name, consumer
	
	private MockPublisher<?> mockPublisher;
	private MockPublisher<? extends ConsumerCommandBean> mockCmdPub;
	private MockSubmitter<? extends StatusBean> mockSubmitter;
	private MockSubscriber<? extends EventListener> mockSubscriber;
	private MockRequester<? extends IdBean> mockRequester; 

	@Override
	public <T> IQueueReader<T> createQueueReader(URI uri, String queueName) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends EventListener> ISubscriber<T> createSubscriber(URI uri, String topicName) {
		if (mockSubscriber == null) return new MockSubscriber<>(uri, topicName);
		return (ISubscriber<T>) mockSubscriber;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> IPublisher<U> createPublisher(URI uri, String topicName) {
		if (ServicesHolder.getQueueService() != null) {
			if (ServicesHolder.getQueueService().getCommandTopicName().equals(topicName)) return (IPublisher<U>) mockCmdPub;
		} else if (mockRequester != null) {
			if (mockRequester.getQueueServiceCommandTopic().equals(topicName)) return (IPublisher<U>) mockCmdPub;
		}
		return (IPublisher<U>) mockPublisher;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U extends StatusBean> ISubmitter<U> createSubmitter(URI uri, String queueName) {
		return (ISubmitter<U>) mockSubmitter.create(queueName);
	}

	@Override
	public <U extends StatusBean> IConsumer<U> createConsumer(URI uri) throws EventException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <U extends StatusBean> IConsumer<U> createConsumer(URI uri, String submissionQName, String statusQName,
			String statusTName) throws EventException {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U extends StatusBean> IConsumer<U> createConsumer(URI uri, String submissionQName, String statusQName,
			String statusTName, String heartbeatTName, String commandTName) throws EventException {
		
		//If the test have set a consumer on this event service, return that, otherwise create a new instance
		MockConsumer<? extends StatusBean> mockConsumer;
		if (mockConsumers.get("defaultCons") == null) {
			mockConsumer = new MockConsumer<>();
			mockConsumers.put(submissionQName, mockConsumer);
		} else {
			mockConsumer = mockConsumers.get("defaultCons");
		}
		
		mockConsumer.setCommandTopicName(commandTName);
		mockConsumer.setStatusTopicName(statusTName);
		mockConsumer.setStatusSetName(statusQName);
		mockConsumer.setSubmitQueueName(submissionQName);
		
		return (IConsumer<U>) mockConsumer;
	}

	@Override
	public <T extends IdBean> IRequester<T> createRequestor(URI uri, String requestTopic, String responseTopic)
			throws EventException {
		return (IRequester<T>) mockRequester;
	}

	@Override
	public <T extends IdBean> IResponder<T> createResponder(URI uri, String requestTopic, String responseTopic)
			throws EventException {
		return new MockResponder<T>();
	}

	@Override
	public void checkHeartbeat(URI uri, String patientName, long listenTime)
			throws EventException, InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public <T extends INameable> void checkTopic(URI uri, String patientName, long listenTime, String topicName,
			Class<T> beanClass) throws EventException, InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public IEventConnectorService getEventConnectorService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T createRemoteService(URI uri, Class<T> serviceClass) throws EventException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMockPublisher(MockPublisher<?> mockPublisher) {
		this.mockPublisher = mockPublisher;
	}
	
	public void setMockCmdPublisher(MockPublisher<ConsumerCommandBean> mockCmdPub) {
		this.mockCmdPub = mockCmdPub;
	}
	
	public <U extends StatusBean> void setMockConsumer(MockConsumer<U> mockCons) {
		mockConsumers.put("defaultCons", mockCons);
	}
	
	public <U extends StatusBean> void setMockSubmitter(MockSubmitter<U> mockSub) {
		this.mockSubmitter = mockSub;
	}
	
	public <U extends EventListener> void setMockSubscriber(MockSubscriber<U> mockSub) {
		this.mockSubscriber = mockSub;
	}
	
	public <T extends IdBean> void setMockRequester(MockRequester<T> mockReq) {
		this.mockRequester = mockReq;
	}
	
	public Map<String, MockConsumer<? extends StatusBean>> getRegisteredConsumers() {
		return mockConsumers;
	}
	
	public MockConsumer<? extends StatusBean> getConsumerForQID(String qID) {
		for (Map.Entry<String, MockConsumer<? extends StatusBean>> entry : mockConsumers.entrySet()) {
			if (entry.getKey().startsWith(qID)) {
				return entry.getValue();
			}
		}
		throw new NoSuchElementException("No consumer for queueID '"+qID+"' found in MockEventService");
	}
	
	public void clearRegisteredConsumers() {
		MockConsumer<? extends StatusBean> mc = mockConsumers.get("defaultCons");
		mockConsumers = new HashMap<>();
		mockConsumers.put("defaultCons", mc);
	}
}
