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
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;

public class MockSubmitter<T extends StatusBean> implements ISubmitter<T> {
	
	private Map<String, List<T>> submittedBeans;
	private Map<String, List<ReorderedBean>> reorderedBeans;
	private String uniqueId, submitQ;
	
	private boolean sendToConsumer = false, disconnected = false;
	
	public MockSubmitter() {
		submittedBeans = new HashMap<>();
		reorderedBeans = new HashMap<>();
		
		//This is used in case we don't specify a queueName
		List<T> defaultQueue = new ArrayList<>();
		submittedBeans.put("defaultQ", defaultQueue);
	}
	
	public void resetSubmitter() {
		submittedBeans.clear();
		disconnected= false;
	}
	
	/**
	 * Hideous hack to allow changing of the submit queue name by MockEventService
	 * @param queueName
	 */
	protected MockSubmitter<T> create(String queueName) {
		this.submitQ = queueName;
		return this;
	}

	@Override
	public String getStatusSetName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStatusSetName(String queueName) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getSubmitQueueName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSubmitQueueName(String queueName) throws EventException {
		this.submitQ = queueName;
	}

	@Override
	public List<T> getQueue(String queueName, String fieldName) throws EventException {
		throw new EventException("Wrong reorder");
	}

	@Override
	public void clearQueue(String queueName) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cleanQueue(String queueName) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean reorder(T bean, String queueName, int amount) throws EventException {
		throw new EventException("Wrong reorder");
	}

	@Override
	public boolean remove(T bean, String queueName) throws EventException {
		throw new EventException("Wrong remove");
	}

	@Override
	public boolean replace(T bean, String queueName) throws EventException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<T> getBeanClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBeanClass(Class<T> beanClass) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<T> getQueue() throws EventException {
		return getQueue("defaultQ");
	}
	
	public List<T> getQueue(String queueName) {
		if (!submittedBeans.containsKey(queueName)) {
			submittedBeans.put(queueName, new ArrayList<T>());
		}
		return submittedBeans.get(queueName);
	}
	
	public T getLastSubmitted(String queueName) throws EventException {
		List<T> queue = getQueue(queueName);
		return queue.get(queue.size()-1);
	}
	
	public int getQueueSize(String queueName) {
		return getQueue(queueName).size();
	}

	@Override
	public void disconnect() throws EventException {
		disconnected = true;
		
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
	public void submit(T bean) throws EventException {
		submit(bean, true);
	}

	@Override
	public void submit(T bean, boolean prepareBean) throws EventException {
		if (uniqueId==null) {
			uniqueId = bean.getUniqueId()!=null ? bean.getUniqueId() : UUID.randomUUID().toString();
		}
		if (prepareBean) {
			if (bean.getUniqueId()==null) bean.setUniqueId(uniqueId);
			if (getTimestamp()>0) bean.setSubmissionTime(getTimestamp());
		}
		if (submitQ == null) {
			getQueue("defaultQ").add(bean);
		} else {
			getQueue(submitQ).add(bean);
			if (sendToConsumer) {
				//Recover the queueID string from the submit queue
				String[] queueIDParts = submitQ.split("\\.");
				String queueID = queueIDParts[0];
				for (int i = 1; i < queueIDParts.length - 2; i++) {
					queueID = queueID+"."+queueIDParts[i];
				}
				
				//Get the MockConsumer & pass bean into status set 
				IQueueService qServ = ServicesHolder.getQueueService();
				@SuppressWarnings("unchecked")
				MockConsumer<T> mockCons = (MockConsumer<T>) qServ.getQueue(queueID).getConsumer();
				mockCons.addToStatusSet(bean);
			}
		}
	}

	@Override
	public void blockingSubmit(T bean) throws EventException, InterruptedException, IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getStatusTopicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStatusTopicName(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean reorder(T bean, int amount) throws EventException {
		if (getQueue(submitQ).contains(bean)) {
			if (!reorderedBeans.containsKey(submitQ)) {
				reorderedBeans.put(submitQ, new ArrayList<ReorderedBean>());
			}
			reorderedBeans.get(submitQ).add(new ReorderedBean(bean, amount));
			return true;
		}
		else return false;
	}

	@Override
	public boolean remove(T bean) throws EventException {
		if (submitQ == null) {
			return getQueue("defaultQ").remove(bean);
		} else {
			return getQueue(submitQ).remove(bean);
		}
	}

	@Override
	public boolean replace(T bean) throws EventException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getUniqueId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUniqueId(String uniqueId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPriority(int priority) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getLifeTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLifeTime(long lifeTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTimestamp(long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDisconnected() {
		return disconnected;
	}
	
	public boolean isBeanReordered(T bean) {
		try {
			getReorderedBean(bean);
			return true;
		} catch (EventException | NullPointerException evEx) {
			//Bean not reordered
			return false;
		}
	}
	
	public int getReorderedBeanMove(T bean) throws EventException {
		return getReorderedBean(bean).move;
		
	}
	
	private MockSubmitter<T>.ReorderedBean getReorderedBean(T bean) throws EventException {
		List<ReorderedBean> beanList = reorderedBeans.get(submitQ);
		for (ReorderedBean reBean : beanList){
			if (reBean.bean == bean) return reBean;
		}
		throw new EventException("Bean not found");
	}
	
	public void setSendToConsumer(boolean send) {
		sendToConsumer = send;
	}
	
	class ReorderedBean {
		protected final T bean;
		protected final int move;
		
		public ReorderedBean(T bean, int move) {
			this.bean = bean;
			this.move = move;
		}
	}

	@Override
	public boolean isQueuePaused(String submissionQueueName) {
		if (!submissionQueueName.equals(getSubmitQueueName())) throw new IllegalArgumentException(getClass().getSimpleName()+" can only deal with the same queue!");
		return false;
	}

}
