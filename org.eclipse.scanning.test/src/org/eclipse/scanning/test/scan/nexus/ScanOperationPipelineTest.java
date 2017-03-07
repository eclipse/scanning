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

import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.api.processing.model.ValueModel;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.junit.Ignore;
import org.junit.Test;

public class ScanProcessingTest extends NexusTest {

	@Ignore("It's gone flakey")
	@Test
	public void testNexusScan() throws Exception {
		testScan(2, 2);
	}
	
	private void testScan(int... shape) throws Exception {
		
		clearTmp();
		IRunnableDevice<ScanModel> scanner = createGridScan(shape); // Outer scan of another scannable, for instance temp.
		NexusAssert.assertScanNotFinished(getNexusRoot(scanner).getEntry());
		scanner.run(null);
	
		scanner.latch(100, TimeUnit.MILLISECONDS);
		// Check we reached ready (it will normally throw an exception on error)
		checkNexusFile(scanner, shape); // Step model is +1 on the size
	}

	
	private IRunnableDevice<ScanModel> createGridScan(int... size) throws Exception {
		
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("xNex");
		gmodel.setFastAxisPoints(size[size.length-1]);
		gmodel.setSlowAxisName("yNex");
		gmodel.setSlowAxisPoints(size[size.length-2]);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		
		IPointGenerator<?> gen = gservice.createGenerator(gmodel);
		
		// We add the outer scans, if any
		if (size.length > 2) { 
			for (int dim = size.length-3; dim>-1; dim--) {
				final StepModel model;
				if (size[dim]-1>0) {
				    model = new StepModel("neXusScannable"+(dim+1), 10,20,9.99d/(size[dim]-1));
				} else {
					model = new StepModel("neXusScannable"+(dim+1), 10,20,30); // Will generate one value at 10
				}
				final IPointGenerator<?> step = gservice.createGenerator(model);
				gen = gservice.createCompoundGenerator(step, gen);
			}
		}
	
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		
		// Create a file to scan into.
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to "+smodel.getFilePath());
		
		// Setup the Mandelbrot
		MandelbrotModel model = createMandelbrotModel();
		IWritableDetector<MandelbrotModel> detector = (IWritableDetector<MandelbrotModel>)dservice.createRunnableDevice(model);
		assertNotNull(detector);
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
                //System.out.println("Ran mandelbrot detector @ "+evt.getPosition());
			}
		});
		
		// Setup the in-line processing
		// This can be from file or from a direct array of operations.
		final IOperationService oservice = ServiceHolder.getOperationService();
		final ProcessingModel pmodel = new ProcessingModel();
		pmodel.setName("subtract");
		pmodel.setDataFile(output.getAbsolutePath());
		pmodel.setDetectorName(detector.getName()); // We process the output from this detector.
		pmodel.setTimeout(5*60); // five minutes for debugging...
		
		// We manually tell the subtract operation to be used and to subtract 100.
		final IOperation<ValueModel,?> subtract = (IOperation<ValueModel,?>)oservice.findFirst("subtractOperation");
		subtract.setModel(new ValueModel(100));
		
		// We save the file so that it is used as the processing
		final File pfile = File.createTempFile("test_operations", ".h5");
		pfile.deleteOnExit();
		
		if (ServiceHolder.getPersistenceService()!=null) { // If run as plugin test, will not be null
			final IPersistentFile file = ServiceHolder.getPersistenceService().createPersistentFile(pfile);
			file.setOperations(subtract);
			file.close();
			pmodel.setOperationsFile(pfile.getAbsolutePath());
		} else {
			pmodel.setOperation(subtract);
		}
		
		final IRunnableDevice<ProcessingModel> processor = dservice.createRunnableDevice(pmodel);
		
		// Assign the detectors
		smodel.setDetectors(detector, processor);

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);

		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				try {
					System.out.println("Running acquisition scan of size "+fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}

	private NXroot getNexusRoot(IRunnableDevice<ScanModel> scanner) throws Exception {
		String filePath = ((AbstractRunnableDevice<ScanModel>) scanner).getModel().getFilePath();

		NexusFile nf = fileFactory.newNexusFile(filePath);
		nf.openToRead();
		
		TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
		return (NXroot) nexusTree.getGroupNode();
	}

	private void checkNexusFile(IRunnableDevice<ScanModel> scanner, int... sizes) throws Exception {
		
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();
		assertEquals(DeviceState.READY, scanner.getDeviceState());

		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		
		// check that the scan points have been written correctly
		assertSolsticeScanGroup(entry, false, sizes);
		
		LinkedHashMap<String, Integer> detectorDataFields = new LinkedHashMap<>();
		detectorDataFields.put(NXdetector.NX_DATA, 2); // num additional dimensions
		detectorDataFields.put("subtract", 2); // num additional dimensions
		detectorDataFields.put("spectrum", 1);
		detectorDataFields.put("value",    0);
		
		List<String> dnames = new ArrayList<String>(2);
		for (IRunnableDevice<?> device : scanModel.getDetectors()) dnames.add(device.getName());
		
		Map<String, String> expectedDataGroupNames = new HashMap<String,String>(5);
		for (String detectorName : dnames) {
			// map of detector data field to name of nxData group where that field is the @signal field
			expectedDataGroupNames.putAll(detectorDataFields.keySet().stream().collect(Collectors.toMap(Function.identity(),
					x -> detectorName + (x.equals(NXdetector.NX_DATA) ? "" : "_" + x))));

		}

		// validate the main NXdata generated by the NexusDataBuilder
		Map<String, NXdata> nxDataGroups = entry.getChildren(NXdata.class);
		assertEquals(detectorDataFields.size(), nxDataGroups.size());
	
		ProcessingModel pmodel = (ProcessingModel)scanModel.getDetectors().get(1).getModel();
		final ILoaderService lservice = ServiceHolder.getLoaderService();
		final IDataHolder holder = lservice.getData(pmodel.getDataFile(), null);
		
		final ILazyDataset mdata = holder.getLazyDataset("/entry/instrument/mandelbrot/data");
		assertTrue(mdata!=null);
		final ILazyDataset sdata = holder.getLazyDataset("/entry/instrument/subtract/data");
		assertTrue(sdata!=null);
		
		// Same shape
		assertTrue(Arrays.equals(mdata.getShape(), sdata.getShape()));
		
		// All mdata+100=sdata
		final IDataset mi = mdata.getSlice(new int[]{1,1,0,0}, new int[]{2,2,64,64}, new int[]{1,1,1,1});
		final IDataset si = sdata.getSlice(new int[]{1,1,0,0}, new int[]{2,2,64,64}, new int[]{1,1,1,1});
		
		assertTrue(Arrays.equals(mi.getShape(), si.getShape()));

	}

	public static INexusFileFactory getFileFactory() {
		return fileFactory;
	}

	public static void setFileFactory(INexusFileFactory fileFactory) {
		ScanProcessingTest.fileFactory = fileFactory;
	}

}
