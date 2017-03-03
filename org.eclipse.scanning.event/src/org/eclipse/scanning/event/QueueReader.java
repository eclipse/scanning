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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a Queue of json beans from the activemq queue and deserializes the 
 * json beans to a specific class.
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public final class QueueReader<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueReader.class);
	
	private Comparator<? super T> comparator;

	private IEventConnectorService service;

	public QueueReader(IEventConnectorService service) {
		this(service, null);
	}
	
	public QueueReader(IEventConnectorService service, Comparator<? super T> comparator) {
		this.service    = service;
		this.comparator = comparator;
	}

	
	/**
	 * Read the status beans from any queue.
	 * Returns a list of optionally date-ordered beans in the queue.
	 * 
	 * @param uri
	 * @param queueName
	 * @param clazz
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public List<T> getBeans(final URI uri, final String queueName, final Class<T> beanClass) throws Exception {
		
		QueueConnection qCon = null;
		try {	        
			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)service.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(queueName);
			qCon.start();

			QueueBrowser qb = qSes.createBrowser(queue);
			@SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();


			final Collection<T> list;
			if (comparator!=null) {
				list = new TreeSet<T>(comparator);
			} else {
				list = new ArrayList<T>(17);
			}

			while(e.hasMoreElements()) {
				Message m = (Message)e.nextElement();
				if (m==null) continue;
				if (m instanceof TextMessage) {
					TextMessage t = (TextMessage)m;
					String json   = t.getText();
					@SuppressWarnings("unchecked")
					final Class<T> statusBeanClass = (Class<T>) StatusBean.class;
					try {
						final T bean = (T)service.unmarshal(json, beanClass != null ? beanClass : statusBeanClass);
						list.add(bean);
						
					} catch (Exception unmarshallable) {
						System.out.println("Removing old message "+json);
						String jMSMessageID = m.getJMSMessageID();
						if (jMSMessageID!=null) {
							MessageConsumer consumer = qSes.createConsumer(queue, "JMSMessageID = '"+jMSMessageID+"'");
							Message ignored = consumer.receive(1000);
							consumer.close();
							System.out.println("Removed");
						}

					}
				}
			}
			return list instanceof List ? (List<T>)list : new ArrayList<T>(list);

		} finally {
			if (qCon!=null) qCon.close();
		}

	}

	/**
	 * 
	 * @param uri
	 * @param topicName
	 * @param clazz
	 * @param monitorTime
	 * @return
	 */
	public Map<String, T> getHeartbeats(final URI uri, final String topicName, final Class<T> clazz, final long monitorTime) throws Exception {
		
		final Map<String, T> ret = new HashMap<String, T>(3);
		Connection topicConnection = null;
		try {
			ConnectionFactory connectionFactory  = (ConnectionFactory)service.createConnectionFactory(uri);
			topicConnection = connectionFactory.createConnection();
			topicConnection.start();

			Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			final Topic           topic    = session.createTopic(topicName);
			final MessageConsumer consumer = session.createConsumer(topic);


			MessageListener listener = new MessageListener() {
				public void onMessage(Message message) {		            	
					try {
						if (message instanceof TextMessage) {
							TextMessage t = (TextMessage) message;
							final T bean = (T)service.unmarshal(t.getText(), clazz);
							Method nameMethod = bean.getClass().getMethod("getName");
							ret.put((String)nameMethod.invoke(bean), bean);
						}
					} catch (Exception e) {
						logger.error("Updating changed bean from topic", e);
					}
				}
			};
			consumer.setMessageListener(listener);
			Thread.sleep(monitorTime);
			
			return ret;

		} catch (Exception ne) {
			logger.error("Cannot listen to topic changes because command server is not there", ne);
			return null;
		} finally {
			topicConnection.close();
		}

	}
}
