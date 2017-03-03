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
package org.eclipse.scanning.test.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.event.remote.RemoteServiceFactory;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.server.servlet.AbstractResponderServlet;
import org.eclipse.scanning.server.servlet.DeviceServlet;
import org.eclipse.scanning.server.servlet.PositionerServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

public class RemoteScannableServiceTest extends BrokerTest {

	private static IScannableDeviceService      cservice;
	private        IScannableDeviceService      rservice;
	private static IEventService                eservice;
	private static AbstractResponderServlet<?>  dservlet, pservlet;

	@BeforeClass
	public static void createServices() throws Exception {
		
		System.out.println("Create Services");
		RemoteServiceFactory.setTimeout(1, TimeUnit.MINUTES); // Make test easier to debug.
		
		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();
		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!

		// Set up stuff because we are not in OSGi with a test
		// DO NOT COPY TESTING ONLY
		cservice = new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC));

		Services.setEventService(eservice);
		Services.setConnector(cservice);
		System.out.println("Set connectors");
		
		dservlet = new DeviceServlet();
		dservlet.setBroker(uri.toString());
		dservlet.setRequestTopic(IEventService.DEVICE_REQUEST_TOPIC);
		dservlet.setResponseTopic(IEventService.DEVICE_RESPONSE_TOPIC);
		dservlet.connect();
		
		pservlet = new PositionerServlet();
		pservlet.setBroker(uri.toString());
		pservlet.setRequestTopic(IEventService.POSITIONER_REQUEST_TOPIC);
		pservlet.setResponseTopic(IEventService.POSITIONER_RESPONSE_TOPIC);
		pservlet.connect();
		System.out.println("Made Servlets");

	}
	
	@Before
	public void createService() throws EventException {
		rservice = eservice.createRemoteService(uri, IScannableDeviceService.class);
	}
	
	@After
	public void disposeService() throws EventException {
		((IDisconnectable)rservice).disconnect();
	}

	
	@AfterClass
	public static void cleanup() throws EventException {
		dservlet.disconnect();
		pservlet.disconnect();
	}

	@Test
	public void checkNotNull() throws Exception {
		assertNotNull(rservice);
	}
	
	@Test
	public void testScannableNames() throws Exception {
		
		Collection<String> names1 = cservice.getScannableNames();
		Collection<String> names2 = rservice.getScannableNames();
		assertTrue(names1!=null);
		assertTrue(names2!=null);
		assertTrue(names1.containsAll(names2));
		assertTrue(names2.containsAll(names1));
	}

	@Test
	public void testGetScannable() throws Exception {
		
		IScannable<Double> xNex1 = cservice.getScannable("xNex");
		IScannable<Double> xNex2 = rservice.getScannable("xNex");
		assertTrue(xNex1!=null);
		assertTrue(xNex2!=null);
	}
	
	@Test
	public void testScannablePositionLocal() throws Exception {
		
		IScannable<Double> xNex1 = cservice.getScannable("xNex");
		IScannable<Double> xNex2 = rservice.getScannable("xNex");
		scannableValues(xNex1, xNex2);
	}
	
	@Test
	public void testScannablePositionRemote() throws Exception {
		
		IScannable<Double> xNex1 = cservice.getScannable("xNex");
		IScannable<Double> xNex2 = rservice.getScannable("xNex");
		scannableValues(xNex2, xNex1);
	}


	private void scannableValues(IScannable<Double> setter, IScannable<Double> getter) throws Exception {
		assertTrue(setter!=null);
		assertTrue(getter!=null);
		
		for (int i = 0; i < 10; i++) {
			setter.setPosition(i*10d);
//			System.out.println("Set "+setter.getName()+" to value "+(i*10d)+" It's value is "+setter.getPosition());
			assertTrue(getter.getPosition()==(i*10d));
//			System.out.println("The value of "+setter.getName()+" was also "+getter.getPosition());
		}
	}
	
	@Test
	public void addFive() throws Exception {
		checkTemperature(5);
	}
	
	@Test
	public void subtractFive() throws Exception {
		checkTemperature(-5);
	}
	
	private void checkTemperature(double delta) throws Exception {
		
		IScannable<Double> temp = rservice.getScannable("T");
	
		List<Double> positions = new ArrayList<>();
		((IPositionListenable)temp).addPositionListener(new IPositionListener() {
			public void positionChanged(PositionEvent evt) throws ScanningException {
				double val = (Double)evt.getPosition().get("T");
//				System.out.println("The value of T was at "+val);
				positions.add(val);
			}
		});
		System.out.println("Moving to "+(temp.getPosition().doubleValue()+delta)+" from "+temp.getPosition());
		temp.setPosition(temp.getPosition().doubleValue()+delta);

        assertEquals(10, positions.size());
	}

}
