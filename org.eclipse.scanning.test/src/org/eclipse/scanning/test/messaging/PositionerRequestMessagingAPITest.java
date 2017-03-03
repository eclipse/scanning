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
package org.eclipse.scanning.test.messaging;

import static org.eclipse.scanning.api.event.EventConstants.POSITIONER_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.POSITIONER_RESPONSE_TOPIC;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.scan.PositionerRequest;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.scannable.MockNeXusScannable;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.server.servlet.PositionerServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * Class to test the API changes for PositionerRequest messaging.
 * 
 * NOTE: Change Python messaging examples accordingly, if any of these
 * tests fail. The 'examples' package can be found in:
 * org.eclipse.scanning.example.messaging/scripts
 * 
 * @author Martin Gaughran
 *
 */
public class PositionerRequestMessagingAPITest extends BrokerTest {
	
	
	protected IEventService             	eservice;
	protected MockScannableConnector 		connector;
	protected IRunnableDeviceService		dservice;
	protected IRequester<PositionerRequest>	requester;
	protected PositionerServlet positionerServlet;
	
	@Before
	public void createServices() throws Exception {
		
		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();
		
		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!	
		
		// If the publisher is not given, then the mock items are not created! Use null instead to avoid publishing.
		connector = new MockScannableConnector(null);
		dservice = new RunnableDeviceServiceImpl(connector);
				
		setupScannableDeviceService();

		Services.setEventService(eservice);
		Services.setConnector(connector);
		Services.setRunnableDeviceService(dservice);

		connect();
	}
	
	protected void setupScannableDeviceService() throws IOException, ScanningException {
	
		registerScannableDevice(new MockScannable("drt_mock_scannable", 10d, 2, "µm"));
		
		MockScannable x = new MockNeXusScannable("drt_mock_nexus_scannable", 0d,  3, "mm");
		x.setRealisticMove(true);
		x.setRequireSleep(false);
		x.setMoveRate(10000);
		
		registerScannableDevice(x);
	}
	
	@SuppressWarnings("rawtypes")
	protected void registerScannableDevice(IScannable device) {
		connector.register(device);
	}

	protected void connect() throws EventException, URISyntaxException {
		
		positionerServlet = new PositionerServlet();
		positionerServlet.setBroker(uri.toString());
		positionerServlet.connect();
		
		requester = eservice.createRequestor(uri, POSITIONER_REQUEST_TOPIC, POSITIONER_RESPONSE_TOPIC);
		requester.setTimeout(10, TimeUnit.SECONDS);
	}
	
	@After
	public void stop() throws EventException {
		
    	if (requester!=null) requester.disconnect();
    	if (positionerServlet!=null) positionerServlet.disconnect();
	}
	
	public String getMessageResponse(String sentJson) throws Exception {
		
		PositionerRequest req = eservice.getEventConnectorService().unmarshal(sentJson, null);
		PositionerRequest res = requester.post(req);
		return eservice.getEventConnectorService().marshal(res);
	}
	
	@Test
	public void testPositioner() throws Exception {
				
		String sentJson = "{\"@type\":\"PositionerRequest\",\"positionType\":\"SET\",\"position\": {\"values\":{\"drt_mock_scannable\":290.0},\"indices\":{\"T\":0},\"stepIndex\": -1, \"dimensionNames\":[[\"drt_mock_scannable\"]]},\"uniqueId\":\"c8f12aee-d56a-49f6-bc03-9c7de9415674\"}";
		String expectedJson = "{\"@type\":\"PositionerRequest\",\"positionType\":\"SET\",\"position\": {\"values\":{\"drt_mock_scannable\":290.0},\"indices\":{},\"stepIndex\": -1, \"dimensionNames\":[[\"drt_mock_scannable\"]]},\"uniqueId\":\"c8f12aee-d56a-49f6-bc03-9c7de9415674\"}";
		
		String returnedJson = getMessageResponse(sentJson);
		
		SubsetStatus.assertJsonContains("Failed to return correct Positioner response.", returnedJson, expectedJson);
		
	}
}
