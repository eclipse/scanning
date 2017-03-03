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

import static org.eclipse.scanning.api.event.EventConstants.DEVICE_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.DEVICE_RESPONSE_TOPIC;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;

import org.eclipse.scanning.api.device.DeviceResponse;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponseCreator;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.scan.DeviceRequest;

/**
 * A servlet to get the available devices from the IDeviceService.
 * 
     Spring config started, for instance:
    <pre>
    
    {@literal <bean id="deviceServlet" class="org.eclipse.scanning.server.servlet.DeviceServlet" init-method="connect">}
    {@literal    <property name="broker"          value="tcp://p45-control:61616" />}
    {@literal    <property name="requestTopic"    value="uk.ac.diamond.p45.requestTopic" />}
    {@literal    <property name="responseTopic"   value="uk.ac.diamond.p45.responseTopic"   />}
    {@literal </bean>}
     
    </pre>
    
    FIXME Add security via activemq layer. Anyone can run this now.

 * 
 * @author Matthew Gerring
 *
 */
public class DeviceServlet extends AbstractResponderServlet<DeviceRequest> {
	
	public DeviceServlet() {
		super(DEVICE_REQUEST_TOPIC, DEVICE_RESPONSE_TOPIC);
	}
	
	@PostConstruct  // Requires spring 3 or better
    public void connect() throws EventException, URISyntaxException {	
		responder = eventService.createResponder(new URI(broker), requestTopic, responseTopic);
		responder.setBeanClass(DeviceRequest.class);
		responder.setResponseCreator(createResponseCreator());
     	logger.info("Started "+getClass().getSimpleName()+" using bean "+responder.getBeanClass());
	}

	protected IResponseCreator<DeviceRequest> createResponseCreator() {
		return new DoResponseCreator() {
			@Override
			public boolean isSynchronous() {
				return false;
			}
		};
	}

	@Override
	public IResponseProcess<DeviceRequest> createResponder(DeviceRequest bean, IPublisher<DeviceRequest> response) throws EventException {
		return new DeviceResponse(Services.getRunnableDeviceService(), Services.getConnector(), bean, response);
	}

}
