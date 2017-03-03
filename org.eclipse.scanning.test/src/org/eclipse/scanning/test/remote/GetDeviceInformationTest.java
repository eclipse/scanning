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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.event.remote.RemoteServiceFactory;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.server.servlet.AbstractResponderServlet;
import org.eclipse.scanning.server.servlet.DeviceServlet;
import org.eclipse.scanning.server.servlet.PositionerServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * These tests are designed to test the logic of getting device information from the runnable device service
 * Put in after changing the way this works, in only attempting to get some device information if a device is 
 * currently marked as being alive. This can be overridden to get all information for all devices, whether alive or not
 * 
 * @author Matt Taylor
 *
 */
public class GetDeviceInformationTest extends BrokerTest {

	private static  IRunnableDeviceService    dservice;
	private static  IEventService             eservice;
	private static AbstractResponderServlet<?>  dservlet, pservlet;
	private static FakeDevice dev1, dev2, dev3, dev4;
	
	private         IRunnableDeviceService    rservice;

	@Before
	public void createService() throws Exception {
		
		System.out.println("Create Services");
		RemoteServiceFactory.setTimeout(1, TimeUnit.MINUTES); // Make test easier to debug.

		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();
		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!

		// Set up stuff because we are not in OSGi with a test
		// DO NOT COPY TESTING ONLY
		dservice = new RunnableDeviceServiceImpl(new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC)));
		
		// First device - up and online
		dev1 = new FakeDevice();
		final DeviceInformation<Object> info1 = new DeviceInformation<Object>(); // This comes from extension point or spring in the real world.
		info1.setName("dev1UpOn");
		info1.setLabel("Example Mandelbrot");
		info1.setDescription("Example mandelbrot device");
		info1.setId("org.eclipse.scanning.example.detector.mandelbrotDetector");
		info1.setIcon("org.eclipse.scanning.example/icon/mandelbrot.png");
		info1.setAlive(true);
		dev1.setDeviceInformation(info1);
		dev1.setDeviceState(DeviceState.RUNNING);
		dev1.setAlive(true);
		dev1.setUp(true);
		((RunnableDeviceServiceImpl)dservice)._register("dev1UpOn", dev1);
		
		// Second device - down and offline
		dev2 = new FakeDevice();
		final DeviceInformation<Object> info2 = new DeviceInformation<Object>(); // This comes from extension point or spring in the real world.
		info2.setName("dev2DownOff");
		info2.setLabel("Example Mandelbrot");
		info2.setDescription("Example mandelbrot device");
		info2.setId("org.eclipse.scanning.example.detector.mandelbrotDetector");
		info2.setIcon("org.eclipse.scanning.example/icon/mandelbrot.png");
		info2.setAlive(false);
		dev2.setDeviceInformation(info2);
		dev2.setDeviceState(DeviceState.RUNNING);
		dev2.setAlive(false);
		dev2.setUp(false);
		((RunnableDeviceServiceImpl)dservice)._register("dev2DownOff", dev2);
		
		// Third device - up and offline
		dev3 = new FakeDevice();
		final DeviceInformation<Object> info3 = new DeviceInformation<Object>(); // This comes from extension point or spring in the real world.
		info3.setName("dev3UpOff");
		info3.setLabel("Example Mandelbrot");
		info3.setDescription("Example mandelbrot device");
		info3.setId("org.eclipse.scanning.example.detector.mandelbrotDetector");
		info3.setIcon("org.eclipse.scanning.example/icon/mandelbrot.png");
		info3.setAlive(false);
		dev3.setDeviceInformation(info3);
		dev3.setDeviceState(DeviceState.RUNNING);
		dev3.setAlive(false);
		dev3.setUp(true);
		((RunnableDeviceServiceImpl)dservice)._register("dev3UpOff", dev3);
		
		// Fourth device - down and online
		dev4 = new FakeDevice();
		final DeviceInformation<Object> info4 = new DeviceInformation<Object>(); // This comes from extension point or spring in the real world.
		info4.setName("dev4DownOn");
		info4.setLabel("Example Mandelbrot");
		info4.setDescription("Example mandelbrot device");
		info4.setId("org.eclipse.scanning.example.detector.mandelbrotDetector");
		info4.setIcon("org.eclipse.scanning.example/icon/mandelbrot.png");
		info4.setAlive(true);
		dev4.setDeviceInformation(info4);
		dev4.setDeviceState(DeviceState.RUNNING);
		dev4.setAlive(true);
		dev4.setUp(false);
		((RunnableDeviceServiceImpl)dservice)._register("dev4DownOn", dev4);

		Services.setRunnableDeviceService(dservice);
		Services.setEventService(eservice);
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
		rservice = eservice.createRemoteService(uri, IRunnableDeviceService.class);
	}
	
	@After
	public void disposeService() throws EventException {
		((IDisconnectable)rservice).disconnect();
		dservlet.disconnect();
		pservlet.disconnect();
	}

	@Test
	public void testGetDeviceInformation() throws Exception {
		
		Collection<DeviceInformation<?>> devInfo1 = dservice.getDeviceInformation();
		Collection<DeviceInformation<?>> devInfo2 = rservice.getDeviceInformation();
		assertTrue(devInfo1!=null);
		assertTrue(devInfo2!=null);
		
		assertEquals(4, devInfo1.size());
		assertEquals(4, devInfo2.size());
		
		DeviceInformation<?> diArray[] = new DeviceInformation[devInfo1.size()];
		devInfo1.toArray(diArray);
		
		DeviceInformation<?> di1 = null;
		DeviceInformation<?> di2 = null;
		DeviceInformation<?> di3 = null;
		DeviceInformation<?> di4 = null;
		
		for (int i = 0; i < diArray.length; i++) {
			switch (diArray[i].getName()) {
				case "dev1UpOn":
					di1 = diArray[i];
					break;
				case "dev2DownOff":
					di2 = diArray[i];
					break;
				case "dev3UpOff":
					di3 = diArray[i];
					break;
				case "dev4DownOn":
					di4 = diArray[i];
					break;
				default:
					fail("Unknown name " + diArray[i].getName());
			}
		}
		
		assertEquals("dev1UpOn", di1.getName());
		assertEquals("dev2DownOff", di2.getName());
		assertEquals("dev3UpOff", di3.getName());
		assertEquals("dev4DownOn", di4.getName());
				
		// D1 should show as running as online, and was marked as alive
		assertEquals(DeviceState.RUNNING, di1.getState());

		// D2 and D3 should show as offline as both marked as not alive
		assertEquals(DeviceState.OFFLINE, di2.getState());
		assertEquals(DeviceState.OFFLINE, di3.getState());

		// D4 should show as offline as offline, but was marked as alive so will fail, and turn to offline
		assertEquals(DeviceState.OFFLINE, di4.getState());
		
		// Check the flag on the device to ensure only the alive ones were checked
		assertEquals(true, dev1.isDeviceStateChecked());
		assertEquals(false, dev2.isDeviceStateChecked());
		assertEquals(false, dev3.isDeviceStateChecked());
		assertEquals(true, dev4.isDeviceStateChecked());
		
	}

	@Test
	public void testGetDeviceInformationIncludingOffline() throws Exception {
		
		Collection<DeviceInformation<?>> devInfo1 = dservice.getDeviceInformationIncludingNonAlive();
		Collection<DeviceInformation<?>> devInfo2 = rservice.getDeviceInformationIncludingNonAlive();
		assertTrue(devInfo1!=null);
		assertTrue(devInfo2!=null);
		
		assertEquals(4, devInfo1.size());
		assertEquals(4, devInfo2.size());
		
		DeviceInformation<?> diArray[] = new DeviceInformation[devInfo1.size()];
		devInfo1.toArray(diArray);
		
		DeviceInformation<?> di1 = null;
		DeviceInformation<?> di2 = null;
		DeviceInformation<?> di3 = null;
		DeviceInformation<?> di4 = null;
		
		for (int i = 0; i < diArray.length; i++) {
			switch (diArray[i].getName()) {
				case "dev1UpOn":
					di1 = diArray[i];
					break;
				case "dev2DownOff":
					di2 = diArray[i];
					break;
				case "dev3UpOff":
					di3 = diArray[i];
					break;
				case "dev4DownOn":
					di4 = diArray[i];
					break;
				default:
					fail("Unknown name " + diArray[i].getName());
			}
		}
		
		assertEquals("dev1UpOn", di1.getName());
		assertEquals("dev2DownOff", di2.getName());
		assertEquals("dev3UpOff", di3.getName());
		assertEquals("dev4DownOn", di4.getName());
		
		// D1 should show as running as online, and was marked as alive
		assertEquals(DeviceState.RUNNING, di1.getState());

		// D2 should show as offline as marked as not alive
		assertEquals(DeviceState.OFFLINE, di2.getState());

		// D3 should show as online as was marked as not alive, but new state was got as online
		assertEquals(DeviceState.RUNNING, di3.getState());

		// D4 should show as offline as offline, but was marked as alive so will fail, and turn to offline
		assertEquals(DeviceState.OFFLINE, di4.getState());

		// Check the flag on the device to ensure all were checked
		assertEquals(true, dev1.isDeviceStateChecked());
		assertEquals(true, dev2.isDeviceStateChecked());
		assertEquals(true, dev3.isDeviceStateChecked());
		assertEquals(true, dev4.isDeviceStateChecked());
		
	}

	static class FakeDevice extends AbstractRunnableDevice<Object> {

		protected FakeDevice(IRunnableDeviceService dservice) {
			super(dservice);
		}
		
		public FakeDevice() {
			super(Services.getRunnableDeviceService());
		}

		private boolean alive = true;
		private DeviceState deviceState = DeviceState.IDLE;
		private String name = "";
		private boolean up = false;
		private boolean deviceStateChecked = false;
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public DeviceRole getRole() {
			return null;
		}

		@Override
		public void setRole(DeviceRole role) {
			
		}

		@Override
		public Set<ScanMode> getSupportedScanModes() {
			return null;
		}

		@Override
		public void setLevel(int level) {
			
		}

		@Override
		public int getLevel() {
			return 0;
		}

		@Override
		public void configure(Object model) throws ScanningException {
			
		}

		@Override
		public void reset() throws ScanningException {
			
		}

		@Override
		public DeviceState getDeviceState() throws ScanningException {
			deviceStateChecked = true;
			if (up) {
				alive = true;
				return deviceState;
			}
			alive = false;
			throw new ScanningException("Not Up");
		}

		public void setDeviceState(DeviceState deviceState) throws ScanningException {
			this.deviceState =  deviceState;
		}

		@Override
		public String getDeviceStatus() throws ScanningException {
			return null;
		}

		@Override
		public boolean isDeviceBusy() throws ScanningException {
			return false;
		}

		@Override
		public void run(IPosition position) throws ScanningException, InterruptedException {
			
		}

		@Override
		public void abort() throws ScanningException {			
		}

		@Override
		public void disable() throws ScanningException {			
		}

		@Override
		public Object getModel() {
			return null;
		}

		@Override
		public boolean isAlive() {
			return alive;
		}

		@Override
		public void setAlive(boolean alive) {
			this.alive = alive;
		}

		public boolean isUp() {
			return up;
		}

		public void setUp(boolean up) {
			this.up = up;
		}
		
		@Override
		public void setDeviceInformation(DeviceInformation<Object> deviceInformation) {
			// TODO Auto-generated method stub
			super.setDeviceInformation(deviceInformation);
			setName(deviceInformation.getName());
		}

		public boolean isDeviceStateChecked() {
			return deviceStateChecked;
		}
		
	}
	
}
