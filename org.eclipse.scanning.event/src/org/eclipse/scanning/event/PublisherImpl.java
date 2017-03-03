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

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.Enumeration;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.alive.ConsumerStatus;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PublisherImpl<T> extends AbstractConnection implements IPublisher<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(PublisherImpl.class);
	
	// JMS things, these are null when not running and 
	// are cleaned up at the end of a run.
	private MessageProducer scanProducer, heartbeatProducer;
	private boolean         alive;
	private String          queueName;
	
	private IConsumer<?> consumer;

	private PrintStream     out;

	public PublisherImpl(URI uri, String topic, IEventConnectorService service) {
		super(uri, topic, service);
	}

	@Override
	public synchronized void broadcast(T bean) throws EventException {
		
		try {
		    if (getTopicName()!=null) {
		    	if (scanProducer==null) {
		    		scanProducer = createProducer(getTopicName());
		    	}
		    }
		    try {
			    if (queueName!=null) {
			    	updateSet(bean);
			    }
		    } catch (Throwable notFatal) {
		    	// Updating the set is not a fatal error
		    	logger.error("Did not update the set", notFatal);
		    }
			if (getTopicName()!=null) {
				send(scanProducer, bean, Constants.getPublishLiveTime());
			}

		} catch (JMSException ne) {
			throw new EventException("Unable to start the scan producer using uri "+uri+" and topic "+getTopicName(), ne);
			
		} catch (Exception neOther) {
			throw new EventException("Unable to prepare and send the event "+bean, neOther);
		}
	}
	
	protected void send(MessageProducer producer, Object message, long messageLifetime)  throws Exception {

		int priority = message instanceof ConsumerCommandBean ? 8 : 4;
	
		String json = service.marshal(message);
		TextMessage msg = createTextMessage(json);
		producer.send(msg, DeliveryMode.NON_PERSISTENT, priority, messageLifetime);	
		if (out!=null) out.println(json);
	}
	
	private TextMessage createTextMessage(String json) throws JMSException {
		
		if (connection==null) createConnection();
		if (session == null)  createSession();
		
		TextMessage message = null;
		try {
			message = session.createTextMessage(json);
		} catch (javax.jms.IllegalStateException ne) {
			createConnection();
			createSession();
			message = session.createTextMessage(json);
		}
        return message;
	}

	public boolean isAlive() {
		return alive;
	}
	
	private volatile HeartbeatBean lastBeat;

	private boolean statusSetAddRequired = false;

	@Override
	public void setAlive(boolean alive) throws EventException {
		
		boolean wasAlive = this.alive;
		this.alive = alive;
		
		if (alive) {
			try {
				if (heartbeatProducer==null) heartbeatProducer = createProducer(getTopicName());
			} catch (JMSException ne) {
				throw new EventException("Unable to start the heartbeat producer using uri "+uri+" and topic "+getTopicName());
			}
			
			Thread aliveThread = new Thread(new Runnable() {
				public void run() {

					long waitTime = 0;
					
					HeartbeatBean beat = new HeartbeatBean();
					beat.setConceptionTime(System.currentTimeMillis());
					
					// Here we are sending the message out to the topic
					while(isAlive()) {
						try {
							
							// Sleep for a bit
							Thread.sleep(Constants.getNotificationFrequency());		

			                // The producer might need to be reconnected.
							if (heartbeatProducer==null) heartbeatProducer = createProducer(getTopicName());
							beat.setPublishTime(System.currentTimeMillis());
							if (consumer!=null) {
								beat.setConsumerId(consumer.getConsumerId());
								beat.setConsumerName(consumer.getName());
								beat.setConsumerStatus(consumer.getConsumerStatus());
							}
							beat.setBeamline(System.getenv("BEAMLINE"));
							beat.setHostName(InetAddress.getLocalHost().getHostName());

							send(heartbeatProducer, beat, Math.round(Constants.getNotificationFrequency()*2.5));
							lastBeat = beat;
							
							waitTime = 0; // We sent something		

						} catch (Exception ne) {
								
							heartbeatProducer = null;
							connection = null;
							session    = null;
							
							waitTime+=Constants.getNotificationFrequency();
							if (waitTime>Constants.getTimeout()) {
								logger.error("Connection to URI "+uri+" is non-viable, no hearbeats will be sent.");
							    PublisherImpl.this.alive = false;
								return;
							}
							
			        		logger.warn("Event publisher heartbeat connection to "+uri+" lost.");
			        		logger.warn("We will check every 2 seconds for 24 hours, until it comes back.");
			        		
							continue;							
						} 
					}
				}
			});
			aliveThread.setName("Alive Notification "+getTopicName()+" ");
			aliveThread.setDaemon(true);
			aliveThread.setPriority(Thread.MIN_PRIORITY);
			aliveThread.start();
			
		} else {
			if (wasAlive) { // Might never have been a heartbeat publisher.
				try {
					Thread.sleep(Constants.getNotificationFrequency()+100); // Make sure dead			
					if (lastBeat!=null) {
						lastBeat.setConsumerStatus(ConsumerStatus.STOPPED);
					    send(heartbeatProducer, lastBeat, Math.round(Constants.getNotificationFrequency()*2.5));
					}
					
				} catch (Exception ne) {
					throw new EventException("Cannot send termination message!", ne);
				}
			}
		}

	}

	private MessageProducer createProducer(String topicName) throws JMSException {
		final Topic topic = createTopic(topicName);
		return session.createProducer(topic);
	}

	@Override
	public void disconnect() throws EventException {
		try {
			alive = false;
			if (scanProducer!=null)      scanProducer.close();
			if (heartbeatProducer!=null) heartbeatProducer.close();
			consumer = null;
			
			super.disconnect();
			
		} catch (JMSException ne) {
			throw new EventException("Internal error - unable to close connection!", ne);
		
		} finally {
			scanProducer = null;
			heartbeatProducer = null;
		}
	}

	public String getStatusSetName() {
		return queueName;
	}

	public void setStatusSetName(String queueName) {
		this.queueName = queueName;
	}
	public void setStatusSetAddRequired(boolean isRequired) {
		this.statusSetAddRequired  = isRequired;
	}

	/**
	 * 
	 * @param bean
	 * @throws Exception 
	 */
	private boolean updateSet(T bean) throws Exception {
		
		
		Queue     queue = createQueue(getStatusSetName());
		QueueBrowser qb = qSession.createBrowser(queue);

		@SuppressWarnings("rawtypes")
		Enumeration  e  = qb.getEnumeration();

		String jMSMessageID = null;
		while(e.hasMoreElements()) {
			Message m = (Message)e.nextElement();
			if (m==null) continue;
			if (m instanceof TextMessage) {
				TextMessage t = (TextMessage)m;

				final T qbean;
				try {
					@SuppressWarnings("unchecked")
					Class<T> beanClass = (Class<T>) bean.getClass();
					qbean = service.unmarshal(t.getText(), beanClass);
					if (qbean==null) continue;
				} catch (Exception ne) {
					// If we cannot deserialize to the type passed in, it certainly is
					// not going to be the bean which we are looking for.
					continue;
				}
				if (isSame(qbean, bean)) {
					jMSMessageID = t.getJMSMessageID();
					break;
				}
			}
		}

		qb.close();

		if (jMSMessageID!=null) {
			MessageConsumer consumer = qSession.createConsumer(queue, "JMSMessageID = '"+jMSMessageID+"'");
			Message m = consumer.receive(Constants.getReceiveFrequency());
			consumer.close();
			if (m!=null && m instanceof TextMessage) {
				MessageProducer producer = qSession.createProducer(queue);
				try {
					TextMessage t = qSession.createTextMessage(service.marshal(bean));
					t.setJMSMessageID(m.getJMSMessageID());
					t.setJMSExpiration(m.getJMSExpiration());
					t.setJMSTimestamp(m.getJMSTimestamp());
					t.setJMSPriority(m.getJMSPriority());
					t.setJMSCorrelationID(m.getJMSCorrelationID());
		
					producer.send(t);
				} finally {
				    producer.close();
				}
				
				return true;
			}
		}
		
		if (statusSetAddRequired) { // It wasn't found so we will add it.
			MessageProducer producer = session.createProducer(queue);
			try {
				producer.setDeliveryMode(DeliveryMode.PERSISTENT);
				String json = null;
				try {
					json = service.marshal(bean);
				} catch (Exception neother) {
					throw new EventException("Unable to marshall bean "+bean, neother);
				}

				TextMessage message = session.createTextMessage(json);
				producer.send(message);
				
			} finally {
				producer.close();
			}

            return true;
		}

		return false;
	}
	
	protected boolean isSame(Object qbean, Object bean) {
		
        if (qbean instanceof PauseBean && bean instanceof PauseBean) {
        	PauseBean q = (PauseBean)qbean;
        	PauseBean b = (PauseBean)bean;
        	
        	if (q.getConsumerId()!=null && q.getConsumerId().equals(b.getConsumerId())) return true;
        	if (q.getQueueName()!=null  && q.getQueueName().equals(b.getQueueName()))   return true;
        }
		return super.isSame(qbean, bean);
	}

	@Override
	public void setLoggingStream(PrintStream stream) {
		this.out = stream;
		if (out!=null) {
			if (consumer!=null) out.println("Publisher for consumer name "+consumer.getName());
		}
	}

	public IConsumer<?> getConsumer() {
		return consumer;
	}

	@Override
	public void setConsumer(IConsumer<?> consumer) {
		this.consumer = consumer;
	}

}
