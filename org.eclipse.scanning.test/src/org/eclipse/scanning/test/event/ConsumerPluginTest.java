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
package org.eclipse.scanning.test.event;

import org.eclipse.scanning.api.event.IEventService;
import org.junit.Before;

public class ConsumerPluginTest extends AbstractConsumerTest {

	
    private static IEventService service;

	public static IEventService getService() {
		return service;
	}

	public static void setService(IEventService service) {
		ConsumerPluginTest.service = service;
	}

	@Before
	public void createServices() throws Exception {
		
		// Use in memory broker removes requirement on network and external ActiveMQ process
		// http://activemq.apache.org/how-to-unit-test-jms-code.html
		eservice = ConsumerPluginTest.service;
		
		// We use the long winded constructor because we need to pass in the connector.
		// In production we would normally 
		submitter  = eservice.createSubmitter(uri, IEventService.SUBMISSION_QUEUE);
		consumer   = eservice.createConsumer(uri, IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		consumer.setName("Test Consumer");
		consumer.clearQueue(IEventService.SUBMISSION_QUEUE);
		consumer.clearQueue(IEventService.STATUS_SET);
	}

}
