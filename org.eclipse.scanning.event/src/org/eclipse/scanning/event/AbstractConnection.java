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

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AbstractConnection {
	
	protected static Logger logger = LoggerFactory.getLogger(AbstractConnection.class);

	protected final URI              uri;
	protected String                 topicName;
	
	protected String                 submitQueueName  = IEventService.SUBMISSION_QUEUE;
	protected String                 statusQueueName  = IEventService.STATUS_SET;
	protected String                 statusTopicName  = IEventService.STATUS_TOPIC;
	protected String                 commandTopicName = IEventService.CMD_TOPIC;

	protected IEventConnectorService service;
	
	protected QueueConnection        connection;
	protected QueueSession           qSession;
	protected Session                session;
	
	private boolean disconnected;

	AbstractConnection(URI uri, String topic, IEventConnectorService service) {
		this.uri = uri;
		this.topicName = topic;
		this.service = service;
	}
	
	AbstractConnection(URI uri, String submitQName, String statusQName, String statusTName, String commandTName, IEventConnectorService service) {
		this.uri = uri;
		this.submitQueueName = submitQName;
		this.statusQueueName = statusQName;
		this.statusTopicName = statusTName;
		this.commandTopicName = commandTName;
		this.service = service;
	}
	
	public IEventConnectorService getConnectorService() {
		return service;
	}
	
	/**
	 * Deals with reconnecting or if broker gone down, fails
	 * 
	 * @param topicName
	 * @return
	 * @throws JMSException
	 */
	protected Topic createTopic(String topicName) throws JMSException {
		
		// Deals with reconnecting or if broker gone down, fails
		try {
			if (connection==null) createConnection();
			if (session == null)  createSession();
			
			return session.createTopic(topicName);
			
		} catch (Exception ne) {
			createConnection();
			createQSession();
			
			return (session!=null) ? session.createTopic(topicName) : null;
		}
	}
	
	/**
	 * Deals with reconnecting or if broker gone down, fails
	 * 
	 * @param queueName
	 * @return
	 * @throws JMSException
	 */
	protected Queue createQueue(String queueName) throws JMSException {
		
		// Deals with reconnecting or if broker gone down, fails
		try {
			if (connection==null) createConnection();
			if (qSession == null) createQSession();
			
			return qSession.createQueue(queueName);
			
		} catch (Exception ne) {
			createConnection();
			createQSession();
			
			return qSession.createQueue(queueName);
		}
	}

	
	protected void createSession() throws JMSException {
		this.session      = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}
	
	private void createQSession() throws JMSException {
		this.qSession     = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	protected void createConnection() throws JMSException {
		Object factory = service.createConnectionFactory(uri);
		QueueConnectionFactory connectionFactory = (QueueConnectionFactory)factory;		
		this.connection = connectionFactory.createQueueConnection();
		connection.start();
	}

	public void disconnect() throws EventException {
		try {
			if (connection!=null)        connection.close();
			if (session!=null)           session.close();
			if (qSession!=null)          qSession.close();
			
		} catch (JMSException ne) {
			logger.error("Internal error - unable to close connection!", ne);
		
		} finally {
			connection = null;
			session = null;
			qSession = null;
		}
		setDisconnected(true);
	}


	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topic) throws EventException {
		if (!isListenersEmpty()) throw new EventException("Cannot change the topic while listeners are still added! They would not function correctly.");
		this.topicName = topic;
		disconnect();
	}

	private boolean isListenersEmpty() {
		return true;
	}


	public URI getUri() {
		return uri;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((statusQueueName == null) ? 0 : statusQueueName.hashCode());
		result = prime * result
				+ ((submitQueueName == null) ? 0 : submitQueueName.hashCode());
		result = prime * result
				+ ((topicName == null) ? 0 : topicName.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractConnection other = (AbstractConnection) obj;
		if (statusQueueName == null) {
			if (other.statusQueueName != null)
				return false;
		} else if (!statusQueueName.equals(other.statusQueueName))
			return false;
		if (submitQueueName == null) {
			if (other.submitQueueName != null)
				return false;
		} else if (!submitQueueName.equals(other.submitQueueName))
			return false;
		if (topicName == null) {
			if (other.topicName != null)
				return false;
		} else if (!topicName.equals(other.topicName))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	public String getSubmitQueueName() {
		return submitQueueName;
	}

	public void setSubmitQueueName(String submitQueueName) {
		this.submitQueueName = submitQueueName;
	}

	public String getStatusSetName() {
		return statusQueueName;
	}

	public void setStatusSetName(String statusQueueName) {
		this.statusQueueName = statusQueueName;
	}

	public String getStatusTopicName() {
		return statusTopicName;
	}

	public void setStatusTopicName(String statusTopicName) {
		this.statusTopicName = statusTopicName;
	}

	public String getCommandTopicName() {
		return commandTopicName;
	}

	public void setCommandTopicName(String terminateTopicName) {
		this.commandTopicName = terminateTopicName;
	}
	
	protected boolean isSame(Object qbean, Object bean) {
		
		Object id1 = getUniqueId(qbean);
		if (id1==null) return qbean.equals(bean); // Probably it won't because we are updating it but they might have transient fields.

		Object id2 = getUniqueId(bean);
		if (id2==null) return qbean.equals(bean); // Probably it won't because we are updating it but they might have transient fields.

		return id1.equals(id2);
	}

	private Object getUniqueId(Object bean) {
		
		if (bean instanceof StatusBean) {
			return ((StatusBean)bean).getUniqueId();
		}
		
		Object value = null;
		try {
			Method method = bean.getClass().getDeclaredMethod("getUniqueId");
			value = method.invoke(bean);
		} catch (Exception e) {
			try {
				Method method = bean.getClass().getDeclaredMethod("getName");
				value = method.invoke(bean);
			} catch (Exception e1) {
				value = null;
			}
		}
		
		return value;
	}

	public boolean isDisconnected() {
		return disconnected;
	}

	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}

}
