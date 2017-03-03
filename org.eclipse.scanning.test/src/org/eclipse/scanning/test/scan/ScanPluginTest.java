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

import static org.junit.Assert.assertNotNull;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.Before;

public class ScanPluginTest extends AbstractScanTest {
	
	private static IRunnableDeviceService  runnableDeviceService;
	private static IPointGeneratorService generatorService;
	private static IEventService     eventService;

	public static IEventService getEventService() {
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		ScanPluginTest.eventService = eventService;
	}

	@Before
	public void setup() throws ScanningException {
		
		eservice  = ScanPluginTest.eventService;
		connector = new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC));
		dservice  = ScanPluginTest.runnableDeviceService;
		gservice  = ScanPluginTest.generatorService;
		
		assertNotNull(runnableDeviceService);
		assertNotNull(generatorService);
		assertNotNull(eventService);
		
		if (dservice instanceof RunnableDeviceServiceImpl) {
			((RunnableDeviceServiceImpl)dservice).setDeviceConnectorService(connector);
		}
	}

	public static IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}

	public static void setRunnableDeviceService(IRunnableDeviceService scanningService) {
		ScanPluginTest.runnableDeviceService = scanningService;
	}

	public static IPointGeneratorService getGeneratorService() {
		return generatorService;
	}

	public static void setGeneratorService(IPointGeneratorService generatorService) {
		ScanPluginTest.generatorService = generatorService;
	}
	
	
}
