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
import java.util.List;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.status.StatusBean;

public class MockConsumer<U extends StatusBean> implements IConsumer<U> {

	private UUID consumerId;
	
	private boolean clearSubmitQueue = false, clearStatQueue = false;
	private boolean started = false, stopped = false, disconnected = false, pauseOnStart=false;
	
	private String statusQueueName = "statQ", submitQueueName = "submQ";
	private String name;
	
	private List<U> statusSet = new ArrayList<>(), submitQueue = new ArrayList<>();
	
	@SuppressWarnings("unchecked")
	public MockConsumer() {
		consumerId = UUID.randomUUID();
		statusSet.add((U) new StatusBean());
		submitQueue.add((U) new StatusBean());
	}
	@Override
	public String getStatusSetName() {
		return statusQueueName;
	}

	@Override
	public void setStatusSetName(String queueName) throws EventException {
		statusQueueName = queueName;
	}

	@Override
	public String getSubmitQueueName() {
		return submitQueueName;
	}

	@Override
	public void setSubmitQueueName(String queueName) throws EventException {
		submitQueueName = queueName;
	}

	@Override
	public List<U> getQueue(String queueName, String fieldName) throws EventException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearQueue(String queueName) throws EventException {
		if (queueName == submitQueueName) {
			submitQueue.clear();
			clearSubmitQueue = true;
		}else if (queueName == statusQueueName) {
			statusSet.clear();
			clearStatQueue = true;
		}
	}

	@Override
	public void cleanQueue(String queueName) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean reorder(U bean, String queueName, int amount) throws EventException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(U bean, String queueName) throws EventException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean replace(U bean, String queueName) throws EventException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<U> getBeanClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBeanClass(Class<U> beanClass) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<U> getQueue() throws EventException {
		// TODO Auto-generated method stub
		return null;
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
	public List<U> getSubmissionQueue() throws EventException {
		return submitQueue;
	}

	@Override
	public List<U> getStatusSet() throws EventException {
		return statusSet;
	}

	@Override
	public String getStatusTopicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStatusTopicName(String queueName) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRunner(IProcessCreator<U> process) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() throws EventException {
		started = true;
	}

	@Override
	public void stop() throws EventException {
		stopped = true;
	}

	@Override
	public void run() throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IProcessCreator<U> getRunner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommandTopicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCommandTopicName(String commandTName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UUID getConsumerId() {
		return consumerId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void disconnect() throws EventException {
		disconnected = true;
	}

	@Override
	public boolean isActive() {
		return started && !stopped;
	}

	@Override
	public boolean isDurable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDurable(boolean durable) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isDisconnected() {
		return disconnected;
	}
	
	public boolean isClearSubmitQueue() {
		return clearSubmitQueue;
	}
	
	public boolean isClearStatQueue() {
		return clearStatQueue;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public boolean isStopped() {
		return stopped;
	}
	
	public void addToStatusSet(U bean) {
		//No duplicates
		for (U setBean : statusSet) {
			if (setBean.getUniqueId().equals(bean.getUniqueId())) {
				statusSet.remove(setBean);
			}
		}
		statusSet.add(bean);
	}
	
	public void addToSubmitQueue(U bean) {
		submitQueue.add(bean);
	}
	public boolean isPauseOnStart() {
		return pauseOnStart;
	}
	public void setPauseOnStart(boolean pauseOnStart) {
		this.pauseOnStart = pauseOnStart;
	}
	@Override
	public void awaitStart() {
		throw new IllegalArgumentException("The method awaitStart() is not implemented for "+getClass().getSimpleName());
	}
	

}
	