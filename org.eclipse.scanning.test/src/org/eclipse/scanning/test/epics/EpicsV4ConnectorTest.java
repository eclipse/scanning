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
import org.eclipse.scanning.example.malcolm.EPICSv4EvilDevice;
import org.eclipse.scanning.example.malcolm.EPICSv4ExampleModel;
import org.eclipse.scanning.example.malcolm.IEPICSv4Device;
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
	private IEPICSv4Device epicsv4Device;

	@Before
	public void before() throws Exception {
		// The real service, get it from OSGi outside this test!
		// Not required in OSGi mode (do not add this to your real code GET THE SERVICE FROM OSGi!)
		this.service = new MalcolmService(new EpicsV4ConnectorService(), null);
	}

	@After
	public void after() throws Exception {
		// Stop the device
		if (epicsv4Device!=null) epicsv4Device.stop();
		service.dispose();
	}

	@Test(expected=MalcolmDeviceException.class)
	public void connectToNonExistentService() throws Exception {

		IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice("fred");

		// Get the device state.
		modelledDevice.getDeviceState();
	}

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to get the device state from
	 * it to check that the Epics V4 connection mechanism is working.
	 * @throws Exception
	 */
	@Test
	public void connectToValidDevice() throws Exception {

		// Start the dummy test device
		DeviceRunner runner = new DeviceRunner();
		epicsv4Device = runner.start();

		// Get the device
		IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice(epicsv4Device.getRecordName());

		// Get the device state.
		DeviceState deviceState = modelledDevice.getDeviceState();

		assertEquals(DeviceState.READY, deviceState);

	}

	@Test(expected=MalcolmDeviceException.class)
	public void connectToEvilDevice() throws Exception {

		// Start the dummy test device
		DeviceRunner runner = new DeviceRunner(EPICSv4EvilDevice.class);
		epicsv4Device = runner.start();

		// Get the device
		IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice(epicsv4Device.getRecordName());

		// Get the device state.
		DeviceState deviceState = modelledDevice.getDeviceState();

		assertEquals(DeviceState.READY, deviceState);

	}

	/**
	 * This device is designed to reproduce a hang which happens with the GDA Server
	 * if malcolm has got into an error state. This happened on I18!
	 */
	@Test(expected=MalcolmDeviceException.class)
	public void connectToHangingService() throws Exception {

		// Start the dummy test device
		DeviceRunner runner = new DeviceRunner(EPICSv4EvilDevice.class);
		epicsv4Device = runner.start();

		try (MalcolmService hanger = new MalcolmService(new HangingGetConnectorService(), null)) {
	        System.setProperty("org.eclipse.scanning.malcolm.core.timeout", String.valueOf(100));

			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = hanger.getDevice(epicsv4Device.getRecordName());

			// Get the device state.
			modelledDevice.getDeviceState(); // Hangs unless timeout is working
		} finally {
			System.setProperty("org.eclipse.scanning.malcolm.core.timeout", String.valueOf(5000));
		}
	}

	/**
	 * Attempts to get the state of a device that doesn't exist. This should throw an exception with a message
	 * detailing that the channel is unavailable.
	 * @throws Exception
	 */
	@Test
	public void connectToInvalidDevice() throws Exception {

		try {
			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice("INVALID_DEVICE");

			// Get the device state. This should fail as the device does not exist
			modelledDevice.getDeviceState();

			fail("No exception thrown but one was expected");

		} catch (Exception ex) {
			assertEquals(MalcolmDeviceException.class, ex.getClass());
			assertTrue(ex.getMessage().contains("Failed to connect to device 'INVALID_DEVICE'"));
			assertTrue(ex.getMessage().contains("channel not connected"));
		}
	}

	@Test
	public void connectToInvalidDeviceTimeout() throws Exception {

		try {
	        System.setProperty("org.eclipse.scanning.malcolm.core.timeout", String.valueOf(50));
			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice("INVALID_DEVICE");

			// Get the device state. This should fail as the device does not exist
			modelledDevice.getDeviceState();

			fail("No exception thrown but one was expected");

		} catch (Exception ex) {
			assertEquals(MalcolmDeviceException.class, ex.getClass());
			assertTrue(ex.getMessage().contains("Failed to connect to device 'INVALID_DEVICE'"));
		} finally {
			System.setProperty("org.eclipse.scanning.malcolm.core.timeout", String.valueOf(5000));
		}
	}

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to get an attribute that doesn't exist.
	 * This should throw an exception with a message detailing that the attribute is not accessible.
	 * @throws Exception
	 */
	@Test
	public void getNonExistantAttribute() throws Exception {

		try {
			// Start the dummy test device
			DeviceRunner runner = new DeviceRunner();
			epicsv4Device = runner.start();

			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice(epicsv4Device.getRecordName());

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
	public void connectToValidDeviceButOfflineWhenConfigure() throws Exception {

		try {
			// Start the dummy test device
			DeviceRunner runner = new DeviceRunner();
			epicsv4Device = runner.start();

			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice(epicsv4Device.getRecordName());

			// Get the device state.
			DeviceState deviceState = modelledDevice.getDeviceState();

			assertEquals(DeviceState.READY, deviceState);

			List<IROI> regions = new LinkedList<>();
			regions.add(new CircularROI(2, 6, 1));

			IPointGeneratorService pgService = new PointGeneratorService();
			IPointGenerator<SpiralModel> temp = pgService.createGenerator(
					new SpiralModel("stage_x", "stage_y", 1, new BoundingBox(0, -5, 8, 3)), regions);
			IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);

			EPICSv4ExampleModel pmac1 = new EPICSv4ExampleModel();
			pmac1.setExposureTime(23.1);
			pmac1.setFileDir("/TestFile/Dir");

			// Set the generator on the device
			// Cannot set the generator from @PreConfigure in this unit test.
			((AbstractMalcolmDevice<?>) modelledDevice).setPointGenerator(scan);

			epicsv4Device.stop();

			try {
				// Call configure
				modelledDevice.configure(pmac1);
				fail("No exception thrown but one was expected");

			} catch (Exception ex) {
				assertEquals(MalcolmDeviceException.class, ex.getClass());
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains("Failed to connect to device"));
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains(epicsv4Device.getRecordName()));
			}

		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

	/**
	 * Starts an instance of the ExampleMalcolmDevice and then attempts to run the device after having stopped the device.
	 * Expect to get an error message saying it can't connect to the device.
	 * @throws Exception
	 */
	@Test
	public void connectToValidDeviceButOfflineWhenRun() throws Exception {

		try {
			// Start the dummy test device
			DeviceRunner runner = new DeviceRunner();
			epicsv4Device = runner.start();

			// Get the device
			IMalcolmDevice<EPICSv4ExampleModel> modelledDevice = service.getDevice(epicsv4Device.getRecordName());

			// Get the device state.
			DeviceState deviceState = modelledDevice.getDeviceState();

			assertEquals(DeviceState.READY, deviceState);

			List<IROI> regions = new LinkedList<>();
			regions.add(new CircularROI(2, 6, 1));

			IPointGeneratorService pgService = new PointGeneratorService();
			IPointGenerator<SpiralModel> temp = pgService
					.createGenerator(new SpiralModel("stage_x", "stage_y", 1, new BoundingBox(0, -5, 8, 3)), regions);
			IPointGenerator<?> scan = pgService.createCompoundGenerator(temp);

			EPICSv4ExampleModel pmac1 = new EPICSv4ExampleModel();
			pmac1.setExposureTime(23.1);
			pmac1.setFileDir("/TestFile/Dir");

			// Set the generator on the device
			// Cannot set the generator from @PreConfigure in this unit test.
			((AbstractMalcolmDevice<?>)modelledDevice).setPointGenerator(scan);

			// Call configure
			modelledDevice.configure(pmac1);

			epicsv4Device.stop();

			try {
				modelledDevice.run(null);
				fail("No exception thrown but one was expected");

			} catch (Exception ex) {
				assertEquals(MalcolmDeviceException.class, ex.getClass());
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains("Failed to connect to device"));
				assertTrue("Message was: " + ex.getMessage(), ex.getMessage().contains(epicsv4Device.getRecordName()));
			}

		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}


}
