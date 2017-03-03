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

import static org.eclipse.scanning.api.event.EventConstants.POSITIONER_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.POSITIONER_RESPONSE_TOPIC;

import org.eclipse.scanning.api.device.PositionerResponse;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.scan.PositionerRequest;

/**
 * A servlet to get the available devices from the IDeviceService.
 * 
     Spring config started, for instance:
    <pre>
    
    {@literal <bean id="positionerServlet" class="org.eclipse.scanning.server.servlet.PositionerServlet" init-method="connect">}
    {@literal    <property name="broker"          value="tcp://p45-control:61616" />}
    {@literal    <property name="requestTopic"    value="org.eclipse.scanning.request.positioner.topic" />}
    {@literal    <property name="responseTopic"   value="org.eclipse.scanning.response.positioner.topic"  />}
    {@literal </bean>}
     
    </pre>
    
    FIXME Add security via activemq layer. Anyone can run this now.

 * 
 * @author Matthew Gerring
 *
 */
public class PositionerServlet extends AbstractResponderServlet<PositionerRequest> {
	
	public PositionerServlet() {
		super(POSITIONER_REQUEST_TOPIC, POSITIONER_RESPONSE_TOPIC);
	}

	@Override
	public IResponseProcess<PositionerRequest> createResponder(PositionerRequest bean, IPublisher<PositionerRequest> response) throws EventException {
		return new PositionerResponse(Services.getRunnableDeviceService(), bean, response);
	}

}
