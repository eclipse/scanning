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
package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.IConfigurable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.example.scannable.MockScannableModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Uses extension points so please run as plugin test.
 * 
 * @author Matthew Gerring
 *
 */
public class LowLevelDetectorPluginTest {
	

	private static INexusFileFactory factory;

	public static INexusFileFactory getFactory() {
		return factory;
	}

	public static void setFactory(INexusFileFactory factory) {
		LowLevelDetectorPluginTest.factory = factory;
	}

	private IRunnableDeviceService        service;
	private IPointGeneratorService       gservice;
	private IScannableDeviceService connector;
	
	@Before
	public void before() {
		connector = new MockScannableConnector(null);
		service   = new RunnableDeviceServiceImpl(connector); // Not testing OSGi so using hard coded service.
		gservice  = new PointGeneratorService();
	}
	
	@Test
	@Ignore
	public void testDetector() throws Exception {
		// FIXME: This test currently broken as shape contains -1
		
		// Create Nexus File
		NexusFile file = createNexusFile();
		final MockWritingMandlebrotModel model = new MockWritingMandlebrotModel();
		model.setFile(file);
		
		// Create the device
		IRunnableDevice<MockWritingMandlebrotModel> det = service.createRunnableDevice(model);
		model.getFile().close();
		assertNotNull(det);
	}
	
	@Test
	public void testMandelbrotScan() throws Exception {

		// Create Nexus File
		NexusFile file = createNexusFile();
		
		// Configure a detector with a collection time.
		final MockWritingMandlebrotModel model = new MockWritingMandlebrotModel();
		model.setFile(file);
		model.setxSize(8); // This could also come from the generator made in createTestScanner(...) but it is hard coded
		model.setySize(5); // This could also come from the generator made in createTestScanner(...) but it is hard coded

		// Make sure that the scannables will write too
		MockScannableModel smod = new MockScannableModel();
		smod.setFile(file);
		smod.setSize(8);
		IScannable<Number> x = connector.getScannable("x");
		((IConfigurable)x).configure(smod);
		
		smod = new MockScannableModel();
		smod.setFile(file);
		smod.setSize(5);
		IScannable<Number> y = connector.getScannable("y");
		((IConfigurable)y).configure(smod);
		
		IRunnableDevice<MockWritingMandlebrotModel> det = service.createRunnableDevice(model);
		model.getFile().close();
		
		IRunnableDevice<ScanModel> scanner = createTestScanner(det, 5, 8);
		scanner.run(null);
		
		// Check we reached ready (it will normally throw an exception on error)
		assertEquals(DeviceState.READY, scanner.getDeviceState());
		

		// Check what was written. Quite a bit to do here, it is not written in the 
		// correct locations or with the correct attributes for now...
		NexusFile nf = factory.newNexusFile(model.getFile().getFilePath());
		nf.openToRead();
		
		DataNode d = nf.getData("/entry1/instrument/detector/"+model.getName());
		IDataset ds = d.getDataset().getSlice().squeeze();
		int[] shape = ds.getShape();

		assertEquals(8, shape[0]);
		assertEquals(5, shape[1]);
		
		// Make sure none of the numbers are NaNs. The detector
		// is expected to fill this scan with non-nulls.
        final PositionIterator it = new PositionIterator(shape);
        while(it.hasNext()) {
        	int[] next = it.getPos();
        	assertFalse(Double.isNaN(ds.getDouble(next)));
        }
        
		d     = nf.getData("/entry1/instrument/axes/"+x.getName());
		ds    = d.getDataset().getSlice().squeeze();
		shape = ds.getShape();
		assertEquals(8, shape[0]);

		
		d     = nf.getData("/entry1/instrument/axes/"+y.getName());
		ds    = d.getDataset().getSlice().squeeze();
		shape = ds.getShape();
		assertEquals(5, shape[0]);

	}

	private NexusFile createNexusFile() throws Exception {
		
		File output = File.createTempFile("test_nexus", ".nxs");
		output.deleteOnExit();
		
		NexusFile file = factory.newNexusFile(output.getAbsolutePath(), true);  // DO NOT COPY!
		file.openToWrite(true); // DO NOT COPY!
		return file;
	}

	private IRunnableDevice<ScanModel> createTestScanner(final IRunnableDevice<?> detector, int... size) throws Exception {
		
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel();
		gmodel.setSlowAxisPoints(size[0]);
		gmodel.setFastAxisPoints(size[1]);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		IPointGenerator<?> gen = gservice.createGenerator(gmodel);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		
		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = service.createRunnableDevice(smodel, null);
		return scanner;
	}

}
