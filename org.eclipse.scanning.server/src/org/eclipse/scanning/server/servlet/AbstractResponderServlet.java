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
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.IResponseCreator;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.servlet.IConsumerServlet;
import org.eclipse.scanning.api.event.servlet.IResponderServlet;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**

    Class used to register a servlet 

    S
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public abstract class AbstractResponderServlet<B extends IdBean> implements IResponderServlet<B> {
	
	protected static final Logger logger = LoggerFactory.getLogger(AbstractResponderServlet.class);

	protected IEventService eventService;
	protected String        broker;
	
	
	// Recommended to configure these as
	protected String        requestTopic;
	protected String        responseTopic;
	
	// The responder for requests to this servlet.
	protected IResponder<B>   responder;

	
	protected AbstractResponderServlet() {
		this.eventService = Services.getEventService();
	}
	
	protected AbstractResponderServlet(String requestTopic, String responseTopic) {
		this();
		this.requestTopic  = requestTopic;
		this.responseTopic = responseTopic;
	}

	@PostConstruct  // Requires spring 3 or better
    public void connect() throws EventException, URISyntaxException {	
    	
		responder = eventService.createResponder(new URI(broker), requestTopic, responseTopic);
		responder.setResponseCreator(createResponseCreator());
     	logger.info("Started "+getClass().getSimpleName());
    }

	/**
	 * Override to change the behaviour of the IResponseCreator
	 * @return
	 */
	protected IResponseCreator<B> createResponseCreator() {
		return new DoResponseCreator();
	}

	class DoResponseCreator implements IResponseCreator<B> {
		@Override
		public IResponseProcess<B> createResponder(B bean, IPublisher<B> response) throws EventException {
			return AbstractResponderServlet.this.createResponder(bean, response);
		}
	}
   
	@PreDestroy
    public void disconnect() throws EventException {
		responder.disconnect();
    }

	public String getBroker() {
		return broker;
	}

	public void setBroker(String uri) {
		this.broker = uri;
	}

	public String getRequestTopic() {
		return requestTopic;
	}

	public void setRequestTopic(String requestTopic) {
		this.requestTopic = requestTopic;
	}

	public String getResponseTopic() {
		return responseTopic;
	}

	public void setResponseTopic(String responseTopic) {
		this.responseTopic = responseTopic;
	}


}
