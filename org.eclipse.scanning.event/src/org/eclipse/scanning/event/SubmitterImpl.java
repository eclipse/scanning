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
package org.eclipse.scanning.event;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.event.status.StatusBean;

class SubmitterImpl<T extends StatusBean> extends AbstractQueueConnection<T> implements ISubmitter<T> {

	// Message things
	private String uniqueId;
	private int    priority;
	private long   lifeTime;
	private long   timestamp;

	SubmitterImpl(URI uri, String submitQueue, IEventConnectorService service, IEventService eservice) {
		super(uri, null, service, eservice);
		setSubmitQueueName(submitQueue);
	}

	@Override
	public void submit(T bean) throws EventException {
        submit(bean, true);
	}
	
	@Override
	public void submit(T bean, boolean prepareBean) throws EventException {
		      
		Connection      send     = null;
		Session         session  = null;
		MessageProducer producer = null;

		try {

			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)service.createConnectionFactory(uri);
			send              = connectionFactory.createConnection();

			session = send.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue(getSubmitQueueName());

			producer = session.createProducer(queue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			if (bean.getSubmissionTime()<1) bean.setSubmissionTime(System.currentTimeMillis());
			if (getPriority()<1)  setPriority(1);
			if (getLifeTime()<1)  setLifeTime(7*24*60*60*1000); // 7 days in ms

			if (uniqueId==null) {
				uniqueId = bean.getUniqueId()!=null ? bean.getUniqueId() : UUID.randomUUID().toString();
			}
			if (prepareBean) {
				if (bean.getUserName()==null) bean.setUserName(System.getProperty("user.name"));
				if (bean.getUniqueId()==null) bean.setUniqueId(uniqueId);
				if (getTimestamp()>0) bean.setSubmissionTime(getTimestamp());
			}

			String json = null;
			try {
				json = service.marshal(bean);
			} catch (Exception e) {
				throw new EventException("Unable to marshall bean "+bean, e);
			}

			TextMessage message = session.createTextMessage(json);

			message.setJMSMessageID(bean.getUniqueId());
			message.setJMSExpiration(getLifeTime());
			message.setJMSTimestamp(getTimestamp());
			message.setJMSPriority(getPriority());

			producer.send(message);
			
			try {
				// Deals with paused consumers by publishing something directly after submission.
				// If there is a topic we tell everyone that we sent something to it in case the consumer is paused.
				if (getStatusTopicName()!=null) { 
					TextMessage msg = session.createTextMessage(json);
					Topic topic = session.createTopic(getStatusTopicName());
					MessageProducer prod = session.createProducer(topic);
					prod.send(msg);
					prod.close();
				}
			} catch (Exception ne) {
				logger.error("Problem publishing to "+getStatusTopicName());
			}


		} catch (Exception e) {
			throw new EventException("Problem opening connection to queue! ", e);

		} finally {
			try {
				if (send!=null)     send.close();
				if (session!=null)  session.close();
				if (producer!=null) producer.close();
			} catch (Exception e) {
				throw new EventException("Cannot close connection as expected!", e);
			}
		}

	}

	@Override
	public void blockingSubmit(T bean) throws EventException, InterruptedException, IllegalStateException {

		String topic = getTopicName();
		if (topic == null) {
			// We can't block if we don't know what topic to listen to.
			throw new IllegalStateException(
					"ISubmitter topicName must be set before blockingSubmit can be called.");
		}

		// We will listen for an event whose UID matches this bean
		// and which signals the scan is complete.
		ISubscriber<IScanListener> subscriber = eservice.createSubscriber(getUri(), topic);
		final String UID = bean.getUniqueId();
		final CountDownLatch latch = new CountDownLatch(1);

		subscriber.addListener(new IScanListener() {

			@Override
			public void scanStateChanged(ScanEvent evt) {
				ScanBean scanBean = evt.getBean();
				if (scanBean.getUniqueId().equals(UID)
						&& scanBean.getStatus().isFinal()) {
					latch.countDown();
				}
			}

			@Override
			public void scanEventPerformed(ScanEvent evt) {
				// We should only have to listen for state changes
				// but testing shows that we need to listen to all
				// scan events, like so. FIXME
				scanStateChanged(evt);
			}
		});

		submit(bean);
		latch.await();
	}
	
	@Override
	public boolean reorder(T bean, int amount) throws EventException {
		return reorder(bean, getSubmitQueueName(), amount);
	}

	@Override
	public boolean remove(T bean) throws EventException {
        return remove(bean, getSubmitQueueName());
	}

	@Override
	public boolean replace(T bean) throws EventException {
        return replace(bean, getSubmitQueueName());
	}

	public String getUniqueId() {
		return uniqueId;
	}


	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}


	public int getPriority() {
		return priority;
	}


	public void setPriority(int priority) {
		this.priority = priority;
	}


	public long getLifeTime() {
		return lifeTime;
	}


	public void setLifeTime(long lifeTime) {
		this.lifeTime = lifeTime;
	}


	public long getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
