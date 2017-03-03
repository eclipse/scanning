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
package org.eclipse.scanning.test.malcolm.mock;

import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.connector.epics.EpicsV4ConnectorService;
import org.eclipse.scanning.test.malcolm.AbstractMultipleClientMalcolmTest;
import org.eclipse.scanning.test.malcolm.device.MockedMalcolmService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

@Ignore("TODO Get this running but needs more work.")
public class MockMultipleClientTest extends AbstractMultipleClientMalcolmTest {
	
	@Override
	@Before
	public void create() throws Exception {
		this.connectorService = new EpicsV4ConnectorService();
		this.service      = new MockedMalcolmService(true);
		this.device       =  service.getDevice("zebra");
	}

	@Override
	@After
	public void dispose() throws Exception {
		if (device!=null)     device.dispose();
		((MockedMalcolmService)service).dispose();
	}
	
	@Override
	protected IMalcolmDevice createAdditionalConnection() throws Exception {
		return service.getDevice("zebra");
	}

}
