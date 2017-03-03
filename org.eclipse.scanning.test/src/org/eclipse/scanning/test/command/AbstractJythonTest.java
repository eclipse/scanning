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
package org.eclipse.scanning.test.command;

import java.util.Arrays;
import java.util.Properties;

import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.eclipse.scanning.test.scan.mock.DummyOperationBean;
import org.python.util.PythonInterpreter;


public abstract class AbstractJythonTest extends BrokerTest{
	
	protected static PythonInterpreter pi;
	protected static MarshallerService           marshaller;
	
	public AbstractJythonTest() {
		super();
	}
	public AbstractJythonTest(boolean start) {
		super(start);
	}
	
	protected static void createMarshaller() {
		marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry(DummyOperationBean.class)),
				Arrays.asList(new PointsModelMarshaller())
				);
		ActivemqConnectorService.setJsonMarshaller(marshaller);
	}

	static {
		Properties postProperties = new Properties();

		// The following line fixes a Python import error seemingly arising
		// from using Jython in an OSGI environment.
		// See http://bugs.jython.org/issue2355 .
		postProperties.put("python.import.site", "false");

		PythonInterpreter.initialize(System.getProperties(), postProperties, new String[0]);
		pi = new PythonInterpreter();

		// FIXME: How to properly specify the path to the Python file?
		// At the moment we use a hack relying on the fact that the
		// JUnit working directory is org.eclipse.scanning.test/.
		pi.exec("import sys");
		pi.exec("sys.path.append('../org.eclipse.scanning.command/scripts/')");
		pi.exec("from mapping_scan_commands import *");
		pi.exec("from mapping_scan_commands import _instantiate");
		pi.exec("from org.eclipse.scanning.example.detector import MandelbrotModel");
		pi.set("my_scannable", new MockScannable("fred", 10));
		pi.set("another_scannable", new MockScannable("bill", 3));
		pi.exec("mandelbrot = lambda t: ('mandelbrot',"
			+	"_instantiate(MandelbrotModel,"
			+	"{'exposureTime': t, 'name': 'mandelbrot'}))");
	}
}
