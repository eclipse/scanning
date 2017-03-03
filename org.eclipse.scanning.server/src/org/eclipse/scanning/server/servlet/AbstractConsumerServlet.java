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
package org.eclipse.scanning.server.servlet;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.servlet.IConsumerServlet;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**

    Class used to register a servlet 

    Spring config started servlets, for instance:
    <pre>
    
    {@literal <bean id="scanPerformer" class="org.eclipse.scanning.server.servlet.ScanServlet" init-method="connect">}
    {@literal    <property name="broker"      value="tcp://p45-control:61616" />}
    {@literal    <property name="submitQueue" value="uk.ac.diamond.p45.submitQueue" />}
    {@literal    <property name="statusSet"   value="uk.ac.diamond.p45.statusSet"   />}
    {@literal    <property name="statusTopic" value="uk.ac.diamond.p45.statusTopic" />}
    {@literal    <property name="durable"     value="true" />}
    {@literal </bean>}
    
    Use: property name="purgeQueue" value="false" to stop the startup phase purging old inactive jobs.
    
    </pre>
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public abstract class AbstractConsumerServlet<B extends StatusBean> implements IConsumerServlet<B> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractConsumerServlet.class);
	
	protected IEventService eventService;
	protected String        broker;
	
	// Property to specify if one scan at a time or more are completed.
	private boolean         blocking = true;
	private boolean         durable  = true;
	private boolean         purgeQueue = true;
	private boolean         pauseOnStart = false;
	
	// Recommended to configure these as
	protected String        submitQueue = IEventService.SUBMISSION_QUEUE;
	protected String        statusSet   = IEventService.STATUS_SET;
	protected String        statusTopic = IEventService.STATUS_TOPIC;
	
	// Recommended not to change these because easier for UI to inspect consumer created
	protected String        heartbeatTopic = IEventService.HEARTBEAT_TOPIC;
	protected String        killTopic      = IEventService.CMD_TOPIC;

	protected IConsumer<B> consumer;
	private boolean        isConnected;
	
	protected AbstractConsumerServlet() {
		this.eventService = Services.getEventService();
	}
	
	protected AbstractConsumerServlet(String submitQueue, String statusSet, String statusTopic) {
		this();
		this.submitQueue = submitQueue;
		this.statusSet   = statusSet;
		this.statusTopic = statusTopic;
	}

	@PostConstruct  // Requires spring 3 or better
    public void connect() throws EventException, URISyntaxException {	
    	
    	consumer = eventService.createConsumer(new URI(getBroker()), getSubmitQueue(), getStatusSet(), getStatusTopic(), getHeartbeatTopic(), getKillTopic());
    	consumer.setName(getName());
    	consumer.setDurable(isDurable());
    	consumer.setRunner(new DoObjectCreator<B>());
    	consumer.setPauseOnStart(pauseOnStart);
    	
    	// Purge old jobs, we wouldn't want those running.
    	// This suggests that DAQ should have one
    	// AbstractConsumerServlet for each queue or when 
    	// another one starts, it might purge the old one.
    	// Use setPurgeQueue(false) to stop it.
     	if (isPurgeQueue()) consumer.cleanQueue(getStatusSet());
    	
     	consumer.start();
     	isConnected = true;
     	logger.info("Started "+getClass().getSimpleName());
     	
   }
    
	protected abstract String getName();

	class DoObjectCreator<T> implements IProcessCreator<B> {
		@Override
		public IConsumerProcess<B> createProcess(B bean, IPublisher<B> response) throws EventException {
			return AbstractConsumerServlet.this.createProcess(bean, response);
		}
	}
   
	@PreDestroy
    public void disconnect() throws EventException {
		if (!isConnected) return; // Nothing to disconnect
    	consumer.disconnect();
    }

	public IConsumer<B> getConsumer() {
		return consumer;
	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String uri) {
		this.broker = uri;
	}

	public String getSubmitQueue() {
		return submitQueue;
	}

	public void setSubmitQueue(String submitQueue) {
		this.submitQueue = submitQueue;
	}

	public String getStatusSet() {
		return statusSet;
	}

	public void setStatusSet(String statusSet) {
		this.statusSet = statusSet;
	}

	public String getStatusTopic() {
		return statusTopic;
	}

	public void setStatusTopic(String statusTopic) {
		this.statusTopic = statusTopic;
	}

	public String getHeartbeatTopic() {
		return heartbeatTopic;
	}

	public void setHeartbeatTopic(String heartbeatTopic) {
		this.heartbeatTopic = heartbeatTopic;
	}

	public String getKillTopic() {
		return killTopic;
	}

	public void setKillTopic(String killTopic) {
		this.killTopic = killTopic;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public boolean isConnected() {
		return isConnected && (consumer!=null ? !consumer.isDisconnected() : true);
	}

	public boolean isPurgeQueue() {
		return purgeQueue;
	}

	public void setPurgeQueue(boolean purgeQueue) {
		this.purgeQueue = purgeQueue;
	}

	public void setConsumer(IConsumer<B> consumer) {
		this.consumer = consumer;
	}

	public boolean isPauseOnStart() {
		return pauseOnStart;
	}

	public void setPauseOnStart(boolean pauseOnStart) {
		this.pauseOnStart = pauseOnStart;
	}

}
