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
package org.eclipse.scanning.test.annot;

import static org.junit.Assert.assertTrue;

import org.eclipse.scanning.api.device.IPausableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * This class simply runs a device implementing AnnotatedDevice in a scan.
 * 
 * @see org.eclipse.scanning.test.scan.ScanSpeedTest
 * 
 * @author Matthew Gerring
 * 
 */
public class AnnotatedDeviceTest {
	
	private RunnableDeviceServiceImpl dservice;
	private PointGeneratorService     gservice;
	private AnAnnotatedDetector detector;

	@Before
	public void before() throws Exception {
		
		dservice  = new RunnableDeviceServiceImpl(new MockScannableConnector());
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, AnAnnotatedDetector.class);

		gservice  = new PointGeneratorService();
		
		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setExposureTime(0.001);
		dmodel.setName("detector");
		this.detector = (AnAnnotatedDetector)dservice.createRunnableDevice(dmodel);
	}
	
	@Test
	public void testADetector() throws Exception {
	
		IRunnableDevice<ScanModel> scanner = createAScanner();
		IPosition pos = new MapPosition("T:100:0");
		scanner.run(pos);
  
		assertTrue(detector.contains("Scan start null"));
	}
	@Test
	public void testADetectorWithPause() throws Exception {
	
		IPausableDevice<ScanModel> scanner = (IPausableDevice<ScanModel>)createAScanner();
		scanner.start(new MapPosition("T:100:0"));
		Thread.sleep(50);
		scanner.pause();
		Thread.sleep(500);
		scanner.resume();

		scanner.latch();

		assertTrue(detector.contains("Scan paused"));
		assertTrue(detector.contains("Scan resumed"));
	}

	private IRunnableDevice<ScanModel> createAScanner() throws Exception {

		GridModel pmodel = new GridModel("x", "y");
		((GridModel) pmodel).setSlowAxisPoints(2);
		((GridModel) pmodel).setFastAxisPoints(2);
		((GridModel) pmodel).setBoundingBox(new BoundingBox(0,0,3,3));

		IPointGenerator<?> gen = gservice.createGenerator(pmodel);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);

		return dservice.createRunnableDevice(smodel, true);
	}
}
