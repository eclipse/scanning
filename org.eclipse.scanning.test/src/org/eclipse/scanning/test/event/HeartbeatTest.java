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

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.event.Constants;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

public class HeartbeatTest extends BrokerTest {

	private IEventService                   eservice;
	private IPublisher<HeartbeatBean>       publisher;
	private ISubscriber<IHeartbeatListener> subscriber;

	@Before
	public void createServices() throws Exception {
		
		Constants.setNotificationFrequency(100);
		Constants.setTimeout(500);

		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();
		
		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!
		
		// Use in memory broker removes requirement on network and external ActiveMQ process
		// http://activemq.apache.org/how-to-unit-test-jms-code.html
		
		// We use the long winded constructor because we need to pass in the connector.
		// In production we would normally 
		publisher  = eservice.createPublisher(uri, IEventService.HEARTBEAT_TOPIC);		
		subscriber = eservice.createSubscriber(uri, IEventService.HEARTBEAT_TOPIC);
	}
	
	@After
	public void dispose() throws EventException {
		publisher.disconnect();
		subscriber.disconnect();
		Constants.setNotificationFrequency(2000);
		Constants.setTimeout(Constants.TIMEOUT);
	}


	@Test
	public void blindHeartbeatTest() throws Exception {

		publisher.setAlive(true);

		Thread.sleep(1000);

		if (!publisher.isAlive()) throw new Exception("Heartbeat should still be being published!");

		publisher.disconnect();

		if (publisher.isAlive()) throw new Exception("Heartbeat should be stopped and the patient should require resusitation! Are they a vampire?");

	}


	@Test
	public void checkedHeartbeatTest() throws Exception {

		publisher.setAlive(true);
		
		final List<HeartbeatBean> gotBack = new ArrayList<>(3);
		subscriber.addListener(new IHeartbeatListener() {
			@Override
			public void heartbeatPerformed(HeartbeatEvent evt) {
				gotBack.add(evt.getBean());
				//System.out.println("The heart beated at "+((new SimpleDateFormat()).format(new Date(evt.getBean().getPublishTime()))));
			}
		});

		Thread.sleep(2000);

		if (!publisher.isAlive()) throw new Exception("Heartbeat should still be being published!");

		publisher.disconnect();

		if (publisher.isAlive()) throw new Exception("Heartbeat should be stopped and the patient should require resusitation! Are they a vampire?");

		if (gotBack.size()<6) throw new Exception("Not enough heartbeats were detected!");
		System.out.println("Encountered "+gotBack.size()+" beats");
	}
	
	@Test
	public void timeoutHeartbeatTest() throws Exception {

		try {
			final URI uri = new URI("tcp://rubbish:5600");	
			publisher = eservice.createPublisher(uri, IEventService.HEARTBEAT_TOPIC);
			publisher.setAlive(true);

		} catch (EventException required) {
			return;
		}
		throw new Exception("Able to connect to tcp://rubbish:5600 without an error!");

	}

}
