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

import org.eclipse.scanning.api.event.IEventService;
import org.junit.Before;

/**
 * Scan Event Test but with OSGi container.
 * @author Matthew Gerring
 *
 */
public class ScanEventPluginTest extends AbstractScanEventTest{

    private static IEventService service;

	public static IEventService getService() {
		return service;
	}

	public static void setService(IEventService service) {
		ScanEventPluginTest.service = service;
	}

	@Before
	public void createServices() throws Exception {
		
		eservice = service;
		
		// We use the long winded constructor because we need to pass in the connector.
		// In production we would normally 
		publisher  = eservice.createPublisher(uri, IEventService.SCAN_TOPIC);		
		subscriber = eservice.createSubscriber(uri, IEventService.SCAN_TOPIC);
	}
}
