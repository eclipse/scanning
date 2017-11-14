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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.core.ResponseConfiguration;
import org.eclipse.scanning.api.event.core.ResponseConfiguration.ResponseType;
import org.eclipse.scanning.api.event.scan.DeviceAction;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.DeviceRequest;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute;
import org.eclipse.scanning.api.malcolm.attributes.StringArrayAttribute;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.Constants;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.server.servlet.DeviceServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Class to test that we can send DeviceRequests and getting a response
 *
 * @author Matthew Gerring
 *
 */
public class DeviceRequestTest extends BrokerTest {


	protected IRunnableDeviceService    dservice;
	protected IEventService             eservice;
	protected IRequester<DeviceRequest> requester;
	protected IResponder<DeviceRequest> responder;

	@Before
	public void createServices() throws Exception {

		// We wire things together without OSGi here
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();

		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!

		// Set up stuff because we are not in OSGi with a test
		// DO NOT COPY TESTING ONLY
		dservice = new RunnableDeviceServiceImpl(new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC)));
		MandelbrotDetector mandy = new MandelbrotDetector();
		final DeviceInformation<MandelbrotModel> info = new DeviceInformation<MandelbrotModel>(); // This comes from extension point or spring in the real world.
		info.setName("mandelbrot");
		info.setLabel("Example Mandelbrot");
		info.setDescription("Example mandelbrot device");
		info.setId("org.eclipse.scanning.example.detector.mandelbrotDetector");
		info.setIcon("org.eclipse.scanning.example/icon/mandelbrot.png");
		mandy.setDeviceInformation(info);
		((RunnableDeviceServiceImpl)dservice)._register("mandelbrot", mandy);

		final DummyMalcolmDevice malc = new DummyMalcolmDevice();
		final DeviceInformation<DummyMalcolmModel> malcInfo = new DeviceInformation<>();
		malcInfo.setName("malcolm");
		malcInfo.setLabel("Malcolm");
		malcInfo.setDescription("Example malcolm device");
		malcInfo.setId("org.eclipse.scanning.example.malcolm.dummyMalcolmDevice");
		malc.setDeviceInformation(malcInfo);

		((RunnableDeviceServiceImpl) dservice)._register("malcolm", malc);

		Services.setRunnableDeviceService(dservice);
		Services.setEventService(eservice);

		connect();
	}

	@Before
	public void start() {

		Constants.setNotificationFrequency(200); // Normally 2000
		Constants.setReceiveFrequency(100);
	}

	@After
	public void stop() throws Exception {

	Constants.setNotificationFrequency(2000); // Normally 2000
	if (requester!=null) requester.disconnect();
	if (responder!=null) responder.disconnect();
	}

	protected void connect() throws Exception {
		DeviceServlet dservlet = new DeviceServlet();
		dservlet.setBroker(uri.toString());
		dservlet.setRequestTopic(EventConstants.DEVICE_REQUEST_TOPIC);
		dservlet.setResponseTopic(EventConstants.DEVICE_RESPONSE_TOPIC);
		dservlet.connect();

		// We use the long winded constructor because we need to pass in the connector.
		// In production we would normally
		requester  = eservice.createRequestor(uri, EventConstants.DEVICE_REQUEST_TOPIC, EventConstants.DEVICE_RESPONSE_TOPIC);
		requester.setTimeout(10, TimeUnit.MINUTES); // It's a test, give it a little longer. // TODO change back to SECONDS

	}

	@Test
	public void simpleSerialize() throws Exception {

		DeviceRequest in = new DeviceRequest();
        String json = eservice.getEventConnectorService().marshal(in);
		DeviceRequest back = eservice.getEventConnectorService().unmarshal(json, DeviceRequest.class);
        assertTrue(in.equals(back));
	}

	// @Test
	public void testGetDevices() throws Exception {

		DeviceRequest req = new DeviceRequest();
		DeviceRequest res = requester.post(req);

		if (res.getDevices().size()<1) throw new Exception("There were no devices found and at least the mandelbrot example should have been!");
	}

	//@Test
	public void testGetDevicesUsingString() throws Exception {

		final ResponseConfiguration responseConfiguration = new ResponseConfiguration(ResponseType.ONE, 1000, TimeUnit.MILLISECONDS);

		final List<DeviceRequest> responses = new ArrayList<>(1);

        final ISubscriber<IBeanListener<DeviceRequest>>  receive = eservice.createSubscriber(uri, EventConstants.DEVICE_RESPONSE_TOPIC);
		// Just listen to our id changing.
		receive.addListener("726c5d29-72f8-42e3-ba0c-51d26378065e", new IBeanListener<DeviceRequest>() {
			@Override
			public void beanChangePerformed(BeanEvent<DeviceRequest> evt) {
				responses.add(evt.getBean());
				responseConfiguration.countDown();
			}
		});

		// Manually send a string without the extra java things...
		final String rawString = "{\"uniqueId\":\"726c5d29-72f8-42e3-ba0c-51d26378065e\",\"deviceType\":\"RUNNABLE\",\"configure\":false}";

		MessageProducer producer = null;
		Connection      send     = null;
		Session         session  = null;

		try {

			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)eservice.getEventConnectorService().createConnectionFactory(uri);
			send              = connectionFactory.createConnection();

			session = send.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Topic topic = session.createTopic(EventConstants.DEVICE_REQUEST_TOPIC);

			producer = session.createProducer(topic);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			// Send the request
			producer.send(session.createTextMessage(rawString));

		} finally {
			try {
				if (session!=null) session.close();
			} catch (JMSException e) {
				throw new EventException("Cannot close session!", e);
			}
		}

		responseConfiguration.latch(null); // Wait or die trying

		if (responses.isEmpty()) throw new Exception("There was no response identified!");
		if (responses.get(0).getDevices().size()<1) throw new Exception("There were no devices found and at least the mandelbrot example should have been!");

	}


	@Test
	public void testGetNamedDeviceModel() throws Exception {
		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("mandelbrot");
		DeviceRequest res = requester.post(req);
		if (res.getDevices().size()!=1) throw new Exception("There were no devices found and at least the mandelbrot example should have been!");
	}

	@Test
	public void testInvalidName() throws Exception {
		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("fred");
		DeviceRequest res = requester.post(req);
		if (!res.isEmpty()) throw new Exception("There should have been no devices found!");
	}

	@Test
	public void testMandelbrotDeviceInfo() throws Exception {
		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("mandelbrot");
		DeviceRequest res = requester.post(req);

		@SuppressWarnings("unchecked")
		DeviceInformation<MandelbrotModel> info = (DeviceInformation<MandelbrotModel>)res.getDeviceInformation();
		assertNotNull("There were no devices found and at least the mandelbrot example should have been!", info);
		assertEquals("Example mandelbrot device", info.getDescription());
		assertEquals(DeviceRole.HARDWARE, info.getDeviceRole());
		assertNull(info.getHealth()); // TODO what does this attribute mean?
		assertEquals("Example Mandelbrot", info.getLabel());
		assertEquals(1, info.getLevel());
		assertEquals("mandelbrot", info.getName());
		assertEquals(DeviceState.READY, info.getState());
		assertEquals(new HashSet<>(Arrays.asList(ScanMode.SOFTWARE)), info.getSupportedScanModes());

		IRunnableDevice<MandelbrotModel> mandy = dservice.getRunnableDevice("mandelbrot");
		assertEquals(mandy.getModel(), info.getModel());
	}

	@Test
	public void testMandelbrotConfigure() throws Exception {

		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("mandelbrot");
		DeviceRequest res = requester.post(req);

		@SuppressWarnings("unchecked")
		DeviceInformation<MandelbrotModel> info = (DeviceInformation<MandelbrotModel>)res.getDeviceInformation();
		assertNotNull("There were no devices found and at least the mandelbrot example should have been!", info);

		MandelbrotModel model = info.getModel();
		model.setExposureTime(0);
		assertTrue(info.getState()==DeviceState.READY); // We do not set an exposure as part of the test.

		// Now we will reconfigure the device
		// and send a new request
		req = new DeviceRequest();
		req.setDeviceName("mandelbrot");
		model.setExposureTime(100);
		model.setEscapeRadius(15);
		req.setDeviceModel(model);
		req.setDeviceAction(DeviceAction.CONFIGURE);

		res = requester.post(req);

		@SuppressWarnings("unchecked")
		DeviceInformation<MandelbrotModel> info2 = (DeviceInformation<MandelbrotModel>)res.getDeviceInformation();
		assertNotNull("There were no devices found and at least the mandelbrot example should have been!", info2);
		assertEquals(100, model.getExposureTime(), 1e-15); // We do not set an exposure as part of the test.
		assertEquals(15, model.getEscapeRadius(), 1e-15); // We do not set an exposure as part of the test.
		assertEquals(DeviceState.ARMED, info2.getState()); // We do not set an exposure as part of the test.
	}

	@Test
	public void testGetAttribute() throws Exception {
		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("malcolm");
		req.setAttributeName("axesToMove");
		DeviceRequest res = requester.post(req);
		assertNotNull(res);
		assertNotNull(res.getAttributes());
		assertEquals(1, res.size());
		IDeviceAttribute<?> attr = res.getAttributes().get("axesToMove");
		assertNotNull(attr);
		assertEquals("axesToMove", attr.getName());
		assertEquals("axesToMove", attr.getLabel());
		assertEquals("Default axis names to scan for configure()", attr.getDescription());
		assertEquals(StringArrayAttribute.class, attr.getClass());
		assertArrayEquals(new String[] { "stage_x", "stage_y" }, (String[]) attr.getValue());
	}

	@Test
	public void testGetAllAttributes() throws Exception {
		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("malcolm");
		req.setGetAllAttributes(true);
		DeviceRequest res = requester.post(req);
		assertNotNull(res);
		assertNotNull(res.getAttributes());
		assertEquals(9, res.getAttributes().size());

		// no need to test all 9 attributes, we'll just test a couple
		IDeviceAttribute<?> stateAttr = res.getAttributes().get("state");
		assertNotNull(stateAttr);
		assertEquals("state", stateAttr.getName());
		assertEquals("Ready", stateAttr.getValue());

		IDeviceAttribute<?> totalSteps = res.getAttributes().get("totalSteps");
		assertNotNull(totalSteps);
		assertEquals("totalSteps", totalSteps.getName());
		assertEquals(0, totalSteps.getValue());
	}

	@Test
	public void testGetUnknownAttribute() throws Exception {
		DeviceRequest req = new DeviceRequest();
		req.setDeviceName("malcolm");
		req.setAttributeName("unknown");
		DeviceRequest res = requester.post(req);
		assertNotNull(res);
		assertTrue(res.getAttributes() == null || res.getAttributes().isEmpty());
		assertEquals("No such attribute: unknown", res.getErrorMessage());
	}

}
