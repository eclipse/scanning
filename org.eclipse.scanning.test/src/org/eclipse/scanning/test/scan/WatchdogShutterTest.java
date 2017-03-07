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
package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IDeviceController;
import org.eclipse.scanning.api.device.IDeviceWatchdog;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.models.DeviceWatchdogModel;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.sequencer.expression.ServerExpressionService;
import org.eclipse.scanning.sequencer.watchdog.ExpressionWatchdog;
import org.eclipse.scanning.server.servlet.Services;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WatchdogShutterTest extends AbstractWatchdogTest {

	
	private static IDeviceWatchdog dog;

	@BeforeClass
	public static void createWatchdogs() throws Exception {
		
		assertNotNull(connector.getScannable("beamcurrent"));
		assertNotNull(connector.getScannable("portshutter"));

		ExpressionWatchdog.setTestExpressionService(new ServerExpressionService());

		// We create a device watchdog (done in spring for real server)
		DeviceWatchdogModel model = new DeviceWatchdogModel();
		model.setExpression("beamcurrent >= 1.0 && !portshutter.equalsIgnoreCase(\"Closed\")");
		
		dog = new ExpressionWatchdog(model);
		dog.setName("expr1");
		dog.activate();
	}
	
	@Before
	public void before() throws Exception {
		assertNotNull(connector.getScannable("beamcurrent"));
		assertNotNull(connector.getScannable("portshutter"));
	
		connector.getScannable("beamcurrent").setPosition(5d);
		connector.getScannable("portshutter").setPosition("Open");
	}

	
	@Test
	public void dogsSame() {
		assertEquals(dog, Services.getWatchdogService().getWatchdog("expr1"));
	}
	
	@Test
	public void expressionDeactivated() throws Exception {

		try {
			// Deactivate!=disabled because deactivate removes it from the service.
			dog.deactivate(); // Are a testing a pausing monitor here
			runQuickie();
		} finally {
			dog.activate();
		}
	}
	
	
	@Test
	public void expressionDisabled() throws Exception {

		try {
			dog.setEnabled(false); // Are a testing a pausing monitor here
			runQuickie();
		} finally {
			dog.setEnabled(true); 
		}
	}

	
	@Test
	public void beamLostInScan() throws Exception {

		// x and y are level 3
		IDeviceController controller = createTestScanner(null);
		IRunnableEventDevice<?> scanner = (IRunnableEventDevice<?>)controller.getDevice();
		
		List<DeviceState> states = new ArrayList<>();
		// This run should get paused for beam and restarted.
		scanner.addRunListener(new IRunListener() {
			public void stateChanged(RunEvent evt) throws ScanningException {
				states.add(evt.getDeviceState());
			}
		});
		
		scanner.start(null);
		scanner.latch(200, TimeUnit.MILLISECONDS);
		
		final IScannable<Number>   mon  = connector.getScannable("beamcurrent");
		mon.setPosition(0.1);
		Thread.sleep(100);
		
		assertTrue(states.get(states.size()-1)==DeviceState.PAUSED);
		
		mon.setPosition(2.1);
		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertTrue(states.get(states.size()-1)==DeviceState.RUNNING);

	}
	
	@Test
	public void shutterClosedInScan() throws Exception {

		// x and y are level 3
		IDeviceController controller = createTestScanner(null);
		IRunnableEventDevice<?> scanner = (IRunnableEventDevice<?>)controller.getDevice();
		
		List<DeviceState> states = new ArrayList<>();
		// This run should get paused for beam and restarted.
		scanner.addRunListener(new IRunListener() {
			public void stateChanged(RunEvent evt) throws ScanningException {
				states.add(evt.getDeviceState());
			}
		});
		
		scanner.start(null);
		scanner.latch(500, TimeUnit.MILLISECONDS);
		
		final IScannable<String>   mon  = connector.getScannable("portshutter");
		mon.setPosition("Closed");
		scanner.latch(100, TimeUnit.MILLISECONDS);
		
		assertTrue(states.get(states.size()-1)==DeviceState.PAUSED);
		
		mon.setPosition("Open");
		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertTrue(states.get(states.size()-1)==DeviceState.RUNNING);

	}
	
	@Test(expected=ScanningException.class)
	public void scanDuringShutterClosed() throws Exception {

		// Stop topup, we want to control it programmatically.
		final IScannable<String>   mon  = connector.getScannable("portshutter");
		mon.setPosition("Closed");
		
		IDeviceController controller = createTestScanner(null);
		IRunnableEventDevice<?> scanner = (IRunnableEventDevice<?>)controller.getDevice();
		
		Set<DeviceState> states = new HashSet<>();
		// This run should get paused for beam and restarted.
		scanner.addRunListener(new IRunListener() {
			public void stateChanged(RunEvent evt) throws ScanningException {
				states.add(evt.getDeviceState());
			}
		});
		
		scanner.start(null);
		scanner.latch(250, TimeUnit.MILLISECONDS); // Wait for an exception
	}
	
	@Test
	public void monitorWithExternalPauseSimple() throws Exception {

		// x and y are level 3
		IDeviceController controller = createTestScanner(null);
		IRunnableEventDevice<?> scanner = (IRunnableEventDevice<?>)controller.getDevice();
		
		Set<DeviceState> states = new HashSet<>();
		// This run should get paused for beam and restarted.
		scanner.addRunListener(new IRunListener() {
			public void stateChanged(RunEvent evt) throws ScanningException {
				states.add(evt.getDeviceState());
			}
		});
		
		scanner.start(null);
		scanner.latch(200, TimeUnit.MILLISECONDS);
		controller.pause("test", null);  // Pausing externally should override any watchdog resume.
		
		final IScannable<String>   mon  = connector.getScannable("portshutter");
		mon.setPosition("Closed");
		mon.setPosition("Open");
		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertEquals(scanner.getDeviceState(), DeviceState.PAUSED);
		
		controller.abort("test");
		
		mon.setPosition("Closed");
		assertNotEquals(scanner.getDeviceState(), DeviceState.PAUSED);
	}
	
	@Test
	public void monitorWithExternalPauseComplex() throws Exception {

		// x and y are level 3
		IDeviceController controller = createTestScanner(null);
		IRunnableEventDevice<?> scanner = (IRunnableEventDevice<?>)controller.getDevice();
		
		Set<DeviceState> states = new HashSet<>();
		// This run should get paused for beam and restarted.
		scanner.addRunListener(new IRunListener() {
			public void stateChanged(RunEvent evt) throws ScanningException {
				states.add(evt.getDeviceState());
			}
		});
		
		scanner.start(null);
		scanner.latch(200, TimeUnit.MILLISECONDS);
		controller.pause("test", null);  // Pausing externally should override any watchdog resume.
		
		final IScannable<String>   mon  = connector.getScannable("portshutter");
		mon.setPosition("Closed");
		mon.setPosition("Open");
		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertEquals(DeviceState.PAUSED, scanner.getDeviceState());
		
		controller.resume("test");
		
		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertNotEquals(DeviceState.PAUSED, scanner.getDeviceState());

		mon.setPosition("Closed");
		
		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertEquals(DeviceState.PAUSED, scanner.getDeviceState());
		
		controller.resume("test"); // The external resume should still not resume it

		scanner.latch(100, TimeUnit.MILLISECONDS);
		assertEquals(DeviceState.PAUSED, scanner.getDeviceState());

		controller.abort("test");
	}

}
