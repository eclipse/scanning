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
package org.eclipse.scanning.test.epics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.connector.epics.EpicsV4ConnectorService;
import org.eclipse.scanning.example.malcolm.ExampleMalcolmDevice;
import org.eclipse.scanning.example.malcolm.ExampleMalcolmModel;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.eclipse.scanning.malcolm.core.MalcolmService;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Class for testing the Epics V4 Connection
 * @author Matt Taylor
 *
 */
public class EpicsV4ConnectorTest {

	private IMalcolmService      service;
	private ExampleMalcolmDevice dummyMalcolmDevice;
	
	@Before
	public void before() throws Exception {
		// The real service, get it from OSGi outside this test!
		// Not required in OSGi mode (do not add this to your real code GET THE SERVICE FROM OSGi!)
		this.service = new MalcolmService(new EpicsV4ConnectorService(), null);

	}
	
	@After
	public void after() throws Exception {
		// Stop the device
		if (dummyMalcolmDevice!=null) dummyMalcolmDevice.stop();
		service.dispose();
	}
	
	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to get the device state from 
	 * it to check that the Epics V4 connection mechanism is working.
	 * @throws Exception
	 */
	@Test
	public void ConnectToValidDevice() throws Exception {

		try {
			// Start the dummy test device
			new Thread(new DeviceRunner()).start();

			// Get the device
			IMalcolmDevice<ExampleMalcolmModel> modelledDevice = service.getDevice(getTestDeviceName());

			// Get the device state.
			DeviceState deviceState = modelledDevice.getDeviceState();
			
			assertEquals(DeviceState.IDLE, deviceState);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} 
	}

	/**
	 * Attempts to get the state of a device that doesn't exist. This should throw an exception with a message
	 * detailing that the channel is unavailable.
	 * @throws Exception
	 */
	@Test
	public void ConnectToInvalidDevice() throws Exception {

		try {
			// Get the device
			IMalcolmDevice<ExampleMalcolmModel> modelledDevice = service.getDevice("INVALID_DEVICE");

			// Get the device state. This should fail as the device does not exist
			modelledDevice.getDeviceState();
			
			fail("No exception thrown but one was expected");

		} catch (Exception ex) {
			assertEquals(MalcolmDeviceException.class, ex.getClass());
			assertTrue(ex.getMessage().contains("Failed to connect to device 'INVALID_DEVICE'"));
			assertTrue(ex.getMessage().contains("channel not connected"));
		}
	}

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to get an attribute that doesn't exist.
	 * This should throw an exception with a message detailing that the attribute is not accessible.
	 * @throws Exception
	 */
	@Test
	public void GetNonExistantAttribute() throws Exception {

		try {
			// Start the dummy test device
			new Thread(new DeviceRunner()).start();

			// Get the device
			IMalcolmDevice<ExampleMalcolmModel> modelledDevice = service.getDevice(getTestDeviceName());

			// Get the device state. This should fail as the device does no exist
			modelledDevice.getAttribute("NON_EXISTANT");

			fail("No exception thrown but one was expected");

		} catch (Exception ex) {
			assertEquals(MalcolmDeviceException.class, ex.getClass());
			assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains("CreateGet failed for 'NON_EXISTANT'"));
			assertTrue(ex.getMessage().contains("illegal pvRequest"));
		} 
	}

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to configure the device after having stopped the device.
	 * Expect to get an error message saying it can't connect to the device.
	 * @throws Exception
	 */
	@Test
	public void ConnectToValidDeviceButOfflineWhenConfigure() throws Exception {

		try {
			// Start the dummy test device
			new Thread(new DeviceRunner()).start();

			// Get the device
			IMalcolmDevice<ExampleMalcolmModel> modelledDevice = service.getDevice(getTestDeviceName());

			// Get the device state.
			DeviceState deviceState = modelledDevice.getDeviceState();
			
			assertEquals(DeviceState.IDLE, deviceState);
			
			List<IROI> regions = new LinkedList<>();
			regions.add(new CircularROI(2, 6, 1));
			
			IPointGeneratorService pgService = new PointGeneratorService();
			IPointGenerator<SpiralModel> temp = pgService
					.createGenerator(new SpiralModel("stage_x", "stage_y", 1, new BoundingBox(0, -5, 8, 3)), regions);
			IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
			
			ExampleMalcolmModel pmac1 = new ExampleMalcolmModel();
			pmac1.setExposureTime(23.1);
			pmac1.setFileDir("/TestFile/Dir");

			// Set the generator on the device
			// Cannot set the generator from @PreConfigure in this unit test.
			((AbstractMalcolmDevice)modelledDevice).setPointGenerator(scan);
			
			dummyMalcolmDevice.stop();
			
			try {
				// Call configure
				modelledDevice.configure(pmac1);
				fail("No exception thrown but one was expected");
				
			} catch (Exception ex) {
				assertEquals(MalcolmDeviceException.class, ex.getClass());
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains("Failed to connect to device"));
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains(getTestDeviceName()));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		} 
	}

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to run the device after having stopped the device.
	 * Expect to get an error message saying it can't connect to the device.
	 * @throws Exception
	 */
	@Test
	public void ConnectToValidDeviceButOfflineWhenRun() throws Exception {

		try {
			// Start the dummy test device
			new Thread(new DeviceRunner()).start();

			// Get the device
			IMalcolmDevice<ExampleMalcolmModel> modelledDevice = service.getDevice(getTestDeviceName());

			// Get the device state.
			DeviceState deviceState = modelledDevice.getDeviceState();
			
			assertEquals(DeviceState.IDLE, deviceState);
			
			List<IROI> regions = new LinkedList<>();
			regions.add(new CircularROI(2, 6, 1));
			
			IPointGeneratorService pgService = new PointGeneratorService();
			IPointGenerator<SpiralModel> temp = pgService
					.createGenerator(new SpiralModel("stage_x", "stage_y", 1, new BoundingBox(0, -5, 8, 3)), regions);
			IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);
			
			ExampleMalcolmModel pmac1 = new ExampleMalcolmModel();
			pmac1.setExposureTime(23.1);
			pmac1.setFileDir("/TestFile/Dir");

			// Set the generator on the device
			// Cannot set the generator from @PreConfigure in this unit test.
			((AbstractMalcolmDevice)modelledDevice).setPointGenerator(scan);
			
			// Call configure
			modelledDevice.configure(pmac1);

			dummyMalcolmDevice.stop();
			
			try {
				modelledDevice.run(null);
				fail("No exception thrown but one was expected");
				
			} catch (Exception ex) {
				assertEquals(MalcolmDeviceException.class, ex.getClass());
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains("Failed to connect to device"));
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains(getTestDeviceName()));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

	public class DeviceRunner implements Runnable {

		public void run() {
			String deviceName = getTestDeviceName();
			dummyMalcolmDevice = new ExampleMalcolmDevice(deviceName);
			dummyMalcolmDevice.start();
		}

	}

	private String getTestDeviceName() {
		String deviceName = "DummyMalcolmDevice";

		Map<String, String> env = System.getenv();
		if (env.containsKey("COMPUTERNAME"))
			deviceName = env.get("COMPUTERNAME");
		else if (env.containsKey("HOSTNAME"))
			deviceName = env.get("HOSTNAME");

		return deviceName.replace('.', ':') + ":malcolmTest";
	}

}
