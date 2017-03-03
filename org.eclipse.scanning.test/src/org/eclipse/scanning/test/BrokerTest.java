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
package org.eclipse.scanning.test;

import java.net.URI;
import java.util.Arrays;

import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.xcen.classregistry.XcenBeanClassRegistry;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * Doing this works better than using vm:// uris.
 * 
 * Please do not use vm:// as it does not work when many tests are started and stopped
 * in a big unit testing system because each test uses the same in VM broker.
 *
 *
 *  TODO Should have static start of broker or per test start for problematic tests
 * 
 * @author Matthew Gerring.
 *
 */
public class BrokerTest extends TmpTest {

	protected static URI uri;     

	private static BrokerDelegate delegate;

	private boolean startEveryTime;
	
	protected BrokerTest() {
		this(false);
	}
	
	protected BrokerTest(boolean startEveryTime) {
		this.startEveryTime = startEveryTime;
	}

	@BeforeClass
	public final static void startBroker() throws Exception {
		delegate = new BrokerDelegate();
		delegate.start();
		uri      = delegate.getUri();
	}
	
	@Before
	public final void startLocalBroker() throws Exception {
		if (startEveryTime) {
			if (delegate!=null) delegate.stop();
			delegate = new BrokerDelegate();
			delegate.start();
			uri      = delegate.getUri();
		}
	}
	
	public final static void setUpNonOSGIActivemqMarshaller(Class<?>...extras) {
		ActivemqConnectorService.setJsonMarshaller(new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						      new ScanningExampleClassRegistry(),
						      new XcenBeanClassRegistry(),
						      new ScanningTestClassRegistry(extras)),
				Arrays.asList(new PointsModelMarshaller())
        ));
	}

	@AfterClass
	public final static void stopBroker() throws Exception {
		if (delegate!=null) delegate.stop();
	}

}
