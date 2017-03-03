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

import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.Before;
import org.junit.Test;

public class RunnableDeviceServiceConfigureTest {

	private IRunnableDeviceService dservice;
	private IPointGeneratorService gservice;

	@Before
	public void setup() throws Exception {
		gservice  = new PointGeneratorService();
		dservice  = new RunnableDeviceServiceImpl(new MockScannableConnector());
		registerFive();
	}
	
	public void registerFive() throws Exception {
		
		for (int i = 0; i < 5; i++) {
			
			final MandelbrotModel model = new MandelbrotModel("x", "y");
			model.setName("mandelbrot"+i);
			model.setExposureTime(0.000001);
		
			final MandelbrotDetector det = new MandelbrotDetector();
			det.setModel(model);
			det.setName("mandelbrot"+i);
			
			DeviceInformation<MandelbrotModel> info = new DeviceInformation<>("mandelbrot"+i);
			info.setId("org.eclipse.scanning.example.mandelbrotDetectorInfo"+i);
			info.setLabel("Mandelbrot Detector "+i);
			info.setDescription("A Test Detector");
			info.setIcon("org.eclipse.scanning.example/icons/alarm-clock-select.png");
			det.setDeviceInformation(info);
			
			dservice.register(det);
			
		}
	}
	
	@Test
	public void testScanMandelbrot1() throws Exception {	
		IRunnableDevice<ScanModel> scan = createTestScanner("mandelbrot1");
		scan.run(null);
		checkRun(scan, 25);
	}
	
	@Test
	public void testScanMandelbrot4() throws Exception {		
		IRunnableDevice<ScanModel> scan = createTestScanner("mandelbrot4");
		scan.run(null);
		checkRun(scan, 25);
	}

	@Test
	public void testScanAFewMandelbrots() throws Exception {		
		IRunnableDevice<ScanModel> scan = createTestScanner("mandelbrot0", "mandelbrot1", "mandelbrot2", "mandelbrot3", "mandelbrot4");
		scan.run(null);
		checkRun(scan, 25);
	}

	
	private IRunnableDevice<ScanModel> createTestScanner(String... names) throws Exception {

		IRunnableDevice<?>[] detectors = new IRunnableDevice[names.length];
		for (int i = 0; i < names.length; i++) {
			detectors[i] = dservice.getRunnableDevice(names[i]);

		}
	
		// If none passed, create scan points for a grid.
		GridModel pmodel = new GridModel("x", "y");
		pmodel.setSlowAxisPoints(5);
		pmodel.setFastAxisPoints(5);
		pmodel.setBoundingBox(new BoundingBox(0,0,3,3));

		IPointGenerator<?> gen = gservice.createGenerator(pmodel);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detectors);

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel);
		return scanner;
	}
	
	private void checkRun(IRunnableDevice<ScanModel> scanner, int size) throws Exception {
		// Bit of a hack to get the generator from the model - should this be easier?
		// Do not copy this code
		ScanModel smodel = (ScanModel)((AbstractRunnableDevice)scanner).getModel();
		IPointGenerator<?> gen = (IPointGenerator<?>)smodel.getPositionIterable();
		assertEquals(gen.size(), size);
	}

}
