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

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IHeartbeatMonitor;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.SizeLimitedRecorder;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

@Deprecated
public class HeartbeatMonitor implements IHeartbeatMonitor {

	private ISubscriber<IHeartbeatListener> monitor;
	private SizeLimitedRecorder<HeartbeatBean> heartbeatRecord;
	
	private URI uri;
	private String heartbeatTopic;
	private UUID monitoredConsumerID;
	private String queueID = null;
	
	private final boolean locked;

	/**
	 * Set up monitor directly from a given consumer ID.
	 * 
	 * @param uri Location of the JMS broker.
	 * @param heartbeatTopic on which {@link HeartbeatBeans} are published.
	 * @param consumerID UUID to listen for.
	 * @throws EventException In case listener cannot be added correctly.
	 */
	public HeartbeatMonitor(URI uri, String heartbeatTopic, UUID consumerID) 
			throws EventException {
		this(uri, heartbeatTopic, consumerID, false);
	}
	
	/**
	 * Set up monitor directly from a given consumer ID. Can be locked to 
	 * follow a given consumer.
	 * 
	 * @param uri Location of the JMS broker.
	 * @param heartbeatTopic on which {@link HeartbeatBeans} are published.
	 * @param consumerID UUID to listen for.
	 * @param locked boolean true if monitor cannot be reconfigured.
	 * @throws EventException In case listener cannot be added correctly.
	 */
	public HeartbeatMonitor(URI uri, String heartbeatTopic, UUID consumerID, 
			boolean locked) throws EventException{
		this.locked = locked;
		this.uri = uri;
		this.heartbeatTopic = heartbeatTopic;
		monitoredConsumerID = consumerID;
		setUpMonitor();
	}

	/**
	 * Set up monitor using an {@link IQueue} instance.
	 * 
	 * @param uri Location of the JMS broker.
	 * @param heartbeatTopic on which {@link HeartbeatBeans} are published.
	 * @param queue {@link IQueue} object to monitor
	 * @throws EventException In case listener cannot be added correctly.
	 */
	public HeartbeatMonitor(URI uri, IQueue<? extends Queueable> queue) 
			throws EventException {
		this(uri, queue, false);
	}
	
	/**
	 * Set up monitor using an {@link IQueue} instance. Can be locked to 
	 * follow a given consumer.
	 * 
	 * @param uri Location of the JMS broker.
	 * @param queue {@link IQueue} object to monitor
	 * @param locked boolean true if monitor cannot be reconfigured.
	 * @throws EventException In case listener cannot be added correctly.
	 */
	public HeartbeatMonitor(URI uri, IQueue<? extends Queueable> queue, 
			boolean locked) throws EventException {
		this.locked = locked;
		this.uri = uri;
		this.heartbeatTopic = queue.getHeartbeatTopicName();
		monitoredConsumerID = queue.getConsumerID();
		setUpMonitor();
		queueID = queue.getQueueID();
	}

	/**
	 * Set up monitor using the queueID string name and the 
	 * {@link IQueueService} which maintains this queue.
	 * 
	 * @param  uri Location of the JMS broker.
	 * @param heartbeatTopic on which {@link HeartbeatBeans} are published.
	 * @param queueID String name of queue in {@link IQueueService}
	 * @param queueService {@link IQueueService} with reference to the 
	 *                     {@link IQueue} to monitor.
	 * @throws EventException In case listener cannot be added correctly.
	 */
	public HeartbeatMonitor(URI uri, String heartbeatTopic, String queueID, 
			IQueueService queueService) throws EventException {
		this(uri, heartbeatTopic, queueID, queueService, false);
	}
	
	/**
	 * Set up monitor using the queueID string name and the 
	 * {@link IQueueService} which maintains this queue. Can be locked to 
	 * follow a given consumer.
	 * 
	 * @param  uri Location of the JMS broker.
	 * @param heartbeatTopic on which {@link HeartbeatBeans} are published.
	 * @param queueID String name of queue in {@link IQueueService}
	 * @param queueService {@link IQueueService} with reference to the 
	 *                     {@link IQueue} to monitor.
	 * @param locked boolean true if monitor cannot be reconfigured.
	 * @throws EventException In case listener cannot be added correctly.
	 */
	public HeartbeatMonitor(URI uri, String heartbeatTopic, String queueID, 
			IQueueService queueService, boolean locked) throws EventException {
		this.locked = locked;
		this.uri = uri;
		this.heartbeatTopic = heartbeatTopic;

		//Get consumerID through the IQueueService
		if (queueID.equals(queueService.getJobQueueID())) {
			monitoredConsumerID = queueService.getJobQueue().getConsumerID();
		} else {
			monitoredConsumerID = queueService.getActiveQueue(queueID).getConsumerID();
		}
		setUpMonitor();
		setQueueID(queueID);
	}

	private void setUpMonitor() throws EventException {
		if (monitoredConsumerID == null) {
			throw new EventException("Consumer ID not set. Has a consumer been instantiated?");
		}
		heartbeatRecord = new SizeLimitedRecorder<>(100);
		
		IEventService evServ = ServicesHolder.getEventService();
		monitor = evServ.createSubscriber(uri, heartbeatTopic);
		monitor.addListener(new IHeartbeatListener() {
			@Override
			public void heartbeatPerformed(HeartbeatEvent evt) {
				HeartbeatBean beat = evt.getBean();
				if (beat.getConsumerId().equals(monitoredConsumerID)) {
					//Only add beans if they are from this consumer!!
					heartbeatRecord.add(beat);
				}
			}
		});
	}
	
	@Override
	public SizeLimitedRecorder<HeartbeatBean> getRecorder() {
		return heartbeatRecord;
	}

	@Override
	public HeartbeatBean getLastHeartbeat() {
		return heartbeatRecord.latest();
	}

	@Override
	public List<HeartbeatBean> getLatestHeartbeats() {
		return heartbeatRecord.getRecording();
	}

	@Override
	public String getHeartbeatTopic() {
		return heartbeatTopic;
	}

	@Override
	public void setHeartbeatTopic(String topicName) {
		heartbeatTopic = topicName;
	}

	@Override
	public UUID getConsumerID() {
		return monitoredConsumerID;
	}

	@Override
	public void setConsumerID(UUID consumerID) throws EventException {
		if (consumerID == null) {
			throw new EventException("New consumer ID is not valid. Has the consumer been instantiated?");
		}
		if (isLocked()) throw new EventException("Cannot change monitored QueueID; monitor is locked to another queue.");
		monitoredConsumerID = consumerID;
		getRecorder().clear();
	}

	@Override
	public String getQueueID() {
		return queueID;
	}

	@Override
	public void setQueueID(String queueID) {
		this.queueID = queueID;
	}

	@Override
	public int getRecorderCapacity() {
		return heartbeatRecord.getCapacity();
	}

	@Override
	public void setRecorderCapacity(int beats) {
		heartbeatRecord.setCapacity(beats);
	}

	@Override
	public boolean isLocked() {
		return locked;
	}

	@Override
	public void disconnect() throws EventException {
		monitor.disconnect();
	}

}
