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
package org.eclipse.scanning.test.scan.legacy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.dawnsci.nexus.ServiceHolder;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.Before;
import org.junit.Test;

public class LegacyDeviceSupportScanTest {

	private IRunnableDeviceService runnableDeviceService;
	private IScannableDeviceService connector;
	private IPointGeneratorService pointGeneratorService;
	private INexusFileFactory fileFactory;
	
	@Before
	public void before() throws Exception {
		fileFactory = new NexusFileFactoryHDF5();
		ServiceHolder.setNexusFileFactory(fileFactory);
		(new org.eclipse.scanning.sequencer.ServiceHolder()).setFactory(new DefaultNexusBuilderFactory());
		connector = new MockLegacyScannableConnector();
		runnableDeviceService = new RunnableDeviceServiceImpl(connector);
		pointGeneratorService = new PointGeneratorService();
	}
	
	@Test
	public void testLegacyDeviceSupportScan() throws Exception {
		int[] shape = new int[] { 8, 5 };
		IRunnableDevice<ScanModel> scanner = createStepScan(shape);
		scanner.run(null);
		checkNexusFile(scanner, shape);
	}
	
	private void checkNexusFile(final IRunnableDevice<ScanModel> scanner, int... sizes) throws Exception {
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();
		assertEquals(DeviceState.READY, scanner.getDeviceState());
		
		String filePath = ((AbstractRunnableDevice<ScanModel>) scanner).getModel().getFilePath();
		NexusFile nf = fileFactory.newNexusFile(filePath);
		nf.openToRead();
		
		TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
		NXroot rootNode = (NXroot) nexusTree.getGroupNode();
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		// check the expected metadata scannables have been included in the scan
		// global metadata scannables: a, b, c, requires d, e, f
		// required by nexusScannable1: x, requires y, z
		// required by nexusScannable2: p, requires q, r
		String[] expectedPositionerNames = new String[] {
				"a", "b", "c", "d", "e", "f",
				"neXusScannable1", "neXusScannable2",
				"p", "q", "r", "x", "y", "z"
		};
		String[] actualPositionerNames = instrument.getAllPositioner().keySet().stream().
				sorted().toArray(String[]::new);
		assertArrayEquals(expectedPositionerNames, actualPositionerNames);
	}
	
	private IRunnableDevice<ScanModel> createStepScan(int... size) throws Exception {
		IPointGenerator<?> gen = null;
		
		// We add the outer scans, if any
		for (int dim = size.length-1; dim>-1; dim--) {
			final StepModel model;
			if (size[dim]-1>0) {
				model = new StepModel("neXusScannable"+(dim+1), 10,20,9.9d/(size[dim]-1));
			} else {
				model = new StepModel("neXusScannable"+(dim+1), 10,20,30); // Will generate one value at 10
			}
			final IPointGenerator<?> step = pointGeneratorService.createGenerator(model);
			if (gen!=null) {
				gen = pointGeneratorService.createCompoundGenerator(step, gen);
			} else {
				gen = step;
			}
		}
	
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
//		if (monitor!=null) smodel.setMonitors(monitor); // TODO remove
//		if (metadataScannable != null) smodel.setMetadataScannables(metadataScannable);
		
		// Create a file to scan into.
		File output = File.createTempFile("test_simple_nexus", ".nxs");
		output.deleteOnExit();
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to " + smodel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = runnableDeviceService.createRunnableDevice(smodel, null);
		
		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException{
                try {
					System.out.println("Running acquisition scan of size "+fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}
}
