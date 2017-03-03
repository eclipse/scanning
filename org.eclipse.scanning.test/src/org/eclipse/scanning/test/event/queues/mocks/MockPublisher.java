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

import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.IHasChildQueue;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueue;

public class MockPublisher<T> implements IPublisher<T> {
	
	
	private String topicName;
	private final URI uri;
	private String queueName;
	private MockConsumer<Queueable> mockCons;
	
	private volatile List<ConsumerCommandBean> broadcastCmdBeans = new ArrayList<>();
	private volatile List<StatusBean> broadcastStatusBeans = new ArrayList<>();
	
	private boolean disconnected = false;
	
	private boolean alive;
	
	public MockPublisher(URI uri, String topic) {
		//Removed from sig: IEventConnectorService service
		this.topicName = topic;
		this.uri = uri;
		
		alive = true;
	}
	
	public void resetPublisher() {
		broadcastStatusBeans.clear();
		disconnected = false;
	}

	@Override
	public String getTopicName() {
		return topicName;
	}

	@Override
	public void setTopicName(String topic) throws EventException {
		this.topicName = topic;
		
	}

	@Override
	public void disconnect() throws EventException {
		setDisconnected(true);
	}

	@Override
	public URI getUri() {
		return uri;
	}
	
	@Override
	public void broadcast(T bean) throws EventException {
		if (bean instanceof ConsumerCommandBean) {
			if (bean instanceof PauseBean) {
				addPauseBean((PauseBean) bean);
			} else if (bean instanceof KillBean) {
				addKillBean((KillBean) bean);
			}
		} else {
			StatusBean broadBean;
			if (bean instanceof IHasChildQueue) {
				if (bean instanceof ScanAtom) {
					broadBean = new ScanAtom();
					((ScanAtom)broadBean).setQueueMessage(((ScanAtom)bean).getQueueMessage());
					((ScanAtom)broadBean).setScanSubmitQueueName(((ScanAtom)bean).getScanSubmitQueueName());
					((ScanAtom)broadBean).setScanStatusTopicName(((ScanAtom)bean).getScanStatusTopicName());
				} else {
					broadBean = new DummyHasQueue();
					((DummyHasQueue)broadBean).setQueueMessage(((IHasChildQueue)bean).getQueueMessage());
				}
				
			} else {
				broadBean = new StatusBean(); 
			}
			StatusBean loBean = (StatusBean)bean;
			broadBean.setMessage(loBean.getMessage());
			broadBean.setPreviousStatus(loBean.getPreviousStatus());
			broadBean.setStatus(loBean.getStatus());
			broadBean.setPercentComplete(loBean.getPercentComplete());
			broadBean.setUniqueId(loBean.getUniqueId());
			broadBean.setName(loBean.getName());
			
			
			broadcastStatusBeans.add(broadBean);
			if ((loBean.getStatus().isRequest()) && (mockCons != null)) {
				mockCons.addToStatusSet((Queueable) broadBean);
			}
		}
	}
	
	public List<StatusBean> getBroadcastBeans() {
		return broadcastStatusBeans;
	}
	
	public List<ConsumerCommandBean> getCmdBeans() {
		return broadcastCmdBeans;
	}
	
	public StatusBean getLastQueueable() {
		if (broadcastStatusBeans.size() > 0) {
			return broadcastStatusBeans.get(broadcastStatusBeans.size()-1);
		} else {
			return null;
		}
	}
	
	public ConsumerCommandBean getLastCmdBean() {
		if (broadcastCmdBeans.size() > 0) {
			return broadcastCmdBeans.get(broadcastCmdBeans.size()-1);
		} else {
			return null;
		}
	}

	@Override
	public void setAlive(boolean alive) throws EventException {
		this.alive = alive;
	}

	@Override
	public boolean isAlive() {
		return alive;
	}

	@Override
	public void setStatusSetName(String queueName) {
		this.queueName = queueName;
		
	}

	@Override
	public String getStatusSetName() {
		return queueName;
	}
	
	@Override
	public void setStatusSetAddRequired(boolean required) {
		throw new RuntimeException("setStatusSetAddRequired is not implemented!");
	}


	@Override
	public void setLoggingStream(PrintStream stream) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IEventConnectorService getConnectorService() {
		// TODO Auto-generated method stub
		return null;
	}

	public MockConsumer<Queueable> getConsumer() {
		return mockCons;
	}

	public void setConsumer(MockConsumer<Queueable> consumer) {
		this.mockCons = consumer;
	}

	public boolean isDisconnected() {
		return disconnected;
	}

	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}
	
	private void addPauseBean(PauseBean bean) {
		final PauseBean pbean = new PauseBean();
		pbean.setConsumerId(bean.getConsumerId());
		pbean.setMessage(bean.getMessage());
		pbean.setPause(bean.isPause());
		pbean.setQueueName(bean.getQueueName());
		pbean.setUniqueId(bean.getUniqueId());
		broadcastCmdBeans.add(pbean);
	}
	
	private void addKillBean(KillBean bean) {
		final KillBean kBean = new KillBean();
		kBean.setConsumerId(bean.getConsumerId());
		kBean.setDisconnect(bean.isDisconnect());
		kBean.setExitProcess(bean.isExitProcess());
		kBean.setMessage(bean.getMessage());
		kBean.setQueueName(bean.getQueueName());
		kBean.setRestart(bean.isRestart());
		kBean.setUniqueId(bean.getUniqueId());
		broadcastCmdBeans.add(kBean);
	}

	@Override
	public void setConsumer(IConsumer<?> consumer) {
		// TODO Auto-generated method stub
		
	}

}
