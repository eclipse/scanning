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

import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.junit.Before;

/**
 * Designed to be run outside OSGi
 *
 * @author Matthew Gerring
 *
 */
public class ScanEventTest extends AbstractScanEventTest{


	@Before
	public void createServices() throws Exception {

		// We wire things together without OSGi here
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();

		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!

		// We use the long winded constructor because we need to pass in the connector.
		// In production we would normally
		publisher  = eservice.createPublisher(uri, IEventService.SCAN_TOPIC);
		subscriber = eservice.createSubscriber(uri, IEventService.SCAN_TOPIC);
	}

}
