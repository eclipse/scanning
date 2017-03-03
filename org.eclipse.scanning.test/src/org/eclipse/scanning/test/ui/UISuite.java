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
package org.eclipse.scanning.test.ui;

import java.net.URI;
import java.util.Arrays;

import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.sequencer.expression.ServerExpressionService;
import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.eclipse.scanning.server.servlet.DeviceServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerDelegate;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({

	SampleInformationTest.class,
	AxisConfigurationTest.class,
	KnownModelsTest.class,
	BoundingBoxTest.class,
	ScannableViewerTest.class,
	ScannableViewerTest2.class,
	ControlTreeViewerTest.class // Must be last!

})
public class UISuite {
	
	private static DeviceServlet dservlet;
	private static BrokerDelegate delegate;
	private static URI uri;

	public static void createTestServices(boolean requireBroker) throws Exception {

		// DO NOT COPY this outside of tests, these services and servlets are created in OSGi and Spring. 
		if (requireBroker) {
			delegate = new BrokerDelegate();
			delegate.start();
			uri      = delegate.getUri();
		}
		
		System.setProperty("org.eclipse.scanning.broker.uri", "vm://localhost?broker.persistent=false");
		
		IMarshallerService marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(), new ScanningExampleClassRegistry(), new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
		);
		ActivemqConnectorService.setJsonMarshaller(marshaller);

		org.eclipse.scanning.device.ui.ServiceHolder.setExpressionService(new ServerExpressionService());
		org.eclipse.scanning.device.ui.ServiceHolder.setSpringParser(new PseudoSpringParser());
		
		IEventService eservice = new EventServiceImpl(new ActivemqConnectorService());
		Services.setConnector(new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC)));
		Services.setEventService(eservice);
		org.eclipse.scanning.device.ui.ServiceHolder.setEventService(eservice);
		
		// Servlet to provide access to the remote scannables.
		dservlet = new DeviceServlet();
		dservlet.setBroker("vm://localhost?broker.persistent=false");
		dservlet.connect(); // Gets called by Spring automatically

	}

	public static void disposeTestServices() throws Exception {
		if (delegate!=null) delegate.stop();
		try {
			dservlet.disconnect();
		} catch (Exception ignored) {
			
		}
	}
	

}
