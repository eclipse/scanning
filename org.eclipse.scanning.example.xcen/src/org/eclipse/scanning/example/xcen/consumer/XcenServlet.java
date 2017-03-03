/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.example.xcen.consumer;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.example.xcen.beans.XcenBean;
import org.eclipse.scanning.server.servlet.AbstractConsumerServlet;

/**
 * A servlet to do any x-ray centering based on the information provided
 * in a XcenBean.
 * 
     Spring config started, for instance:
    <pre>
    
    {@literal <bean id="xcenServlet" class="org.eclipse.scanning.example.xcen.consumer.XcenServlet" init-method="connect">}
    {@literal    <property name="broker"      value="tcp://localhost:61616" />}
    {@literal    <property name="submitQueue" value="dataacq.xcen.SUBMISSION_QUEUE" />}
    {@literal    <property name="statusSet"   value="dataacq.xcen.STATUS_QUEUE"   />}
    {@literal    <property name="statusTopic" value="dataacq.xcen.STATUS_TOPIC" />}
    {@literal    <property name="durable"     value="true" />}
    {@literal </bean>}
     
    </pre>
    
    FIXME Add security via activemq layer. Anyone can run this now.

 * 
 * @author Matthew Gerring
 *
 */
public class XcenServlet extends AbstractConsumerServlet<XcenBean> {

	@Override
	public String getName() {
		return "X-Ray Centering Consumer";
	}

	@Override
	public IConsumerProcess<XcenBean> createProcess(XcenBean bean, IPublisher<XcenBean> response) throws EventException {
		return new XcenProcess(bean, response);
	}
}
