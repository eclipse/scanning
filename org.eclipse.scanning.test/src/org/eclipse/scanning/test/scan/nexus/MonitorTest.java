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

import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertAxes;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertIndices;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertScanNotFinished;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSignal;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertTarget;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.ConstantVelocityModel;
import org.junit.Before;
import org.junit.Test;

public class MonitorTest extends NexusTest {

		
	private static IWritableDetector<ConstantVelocityModel> detector;

	@Before
	public void before() throws Exception {
				
		ConstantVelocityModel model = new ConstantVelocityModel("cv scan", 100, 200, 25);
		model.setName("cv device");
			
		detector = (IWritableDetector<ConstantVelocityModel>)dservice.createRunnableDevice(model);
		assertNotNull(detector);
		
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
                //System.out.println("Ran cv device detector @ "+evt.getPosition());
			}
		});

	}
	
	@Test
	public void test1DOuter() throws Exception {		
		testScan(8);
	}
	
	@Test
	public void testPerPoint() throws Exception {		
		testScan(MonitorRole.PER_POINT, MonitorRole.PER_POINT, 2); // They all are anyway
	}
	@Test
	public void testPerPointIsPerScanToo() throws Exception {		
		testScan(MonitorRole.PER_POINT, MonitorRole.PER_SCAN, 2); // They all are anyway
	}

	@Test
	public void testPerScan() throws Exception {		
		testScan(MonitorRole.PER_SCAN, MonitorRole.PER_SCAN, 2);
	}
	@Test(expected=AssertionError.class)
	public void testPerScanIsNotPerPoint() throws Exception {		
		testScan(MonitorRole.PER_SCAN, MonitorRole.PER_POINT, 2);
	}
	
	@Test
	public void testMixture() throws Exception {
		
	    // NOTE That they must be MockNeXusScannables which we test.
		List<String> perPoint = Arrays.asList("monitor0", "monitor3");
		for (String monName : perPoint) connector.getScannable(monName).setMonitorRole(MonitorRole.PER_POINT);

		List<String> perScan = Arrays.asList("z", "monitor1");
		for (String monName : perScan) connector.getScannable(monName).setMonitorRole(MonitorRole.PER_SCAN);
	
		IRunnableDevice<ScanModel> scanner = runScan(Arrays.asList("monitor0", "monitor1", "monitor3", "z"), 5);
		
        assertPerPointMonitors(scanner, perPoint, 5);
        assertPerScanMonitors(scanner, perScan);
	}

	
	@Test
	public void test2DOuter() throws Exception {		
		testScan(5, 8);
	}
	
	@Test
	public void test3DOuter() throws Exception {		
		testScan(2, 2, 2);
	}
	
	@Test
	public void test8DOuter() throws Exception {		
		testScan(2, 1, 1, 1, 1, 1, 1, 1);
	}

	private NXroot getNexusRoot(IRunnableDevice<ScanModel> scanner) throws Exception {
		String filePath = ((AbstractRunnableDevice<ScanModel>) scanner).getModel().getFilePath();

		NexusFile nf = fileFactory.newNexusFile(filePath);
		nf.openToRead();
		
		TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
		return (NXroot) nexusTree.getGroupNode();
	}

	private void testScan(int... shape) throws Exception {
		
		testScan(MonitorRole.PER_POINT, MonitorRole.PER_POINT, shape);
	}


	private void testScan(MonitorRole mrole, MonitorRole testedRole, int... shape) throws Exception {
		final List<String>        monitors = Arrays.asList("monitor1", "monitor2");
		for (String monName : monitors) connector.getScannable(monName).setMonitorRole(mrole);
		
		IRunnableDevice<ScanModel> scanner = runScan(monitors, shape);
		
		// Check we reached ready (it will normally throw an exception on error)
        checkNexusFile(scanner, monitors, testedRole, shape); // Step model is +1 on the size
	}	
	
	private IRunnableDevice<ScanModel>runScan(List<String> monitors, int... shape) throws Exception {

		
		IRunnableDevice<ScanModel> scanner = createNestedStepScanWithMonitors(detector, monitors, shape); // Outer scan of another scannable, for instance temp.
		assertScanNotFinished(getNexusRoot(scanner).getEntry());

		scanner.run(null);
	    return scanner;
	}

	private void checkNexusFile(IRunnableDevice<ScanModel> scanner, List<String> monitorNames, MonitorRole role, int... sizes) throws Exception {
		
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>)scanner).getModel();                      
		assertEquals(DeviceState.READY, scanner.getDeviceState());                                                
		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		// check that the scan points have been written correctly
		assertSolsticeScanGroup(entry, false, sizes);
		
		String detectorName = scanModel.getDetectors().get(0).getName();
		NXdetector detector = instrument.getDetector(detectorName);
		DataNode dataNode = detector.getDataNode(NXdetector.NX_DATA);          
		IDataset dataset = dataNode.getDataset().getSlice();                                                            
		int[] shape = dataset.getShape();
		
		// validate the NXdata generated by the NexusDataBuilder
		NXdata nxData = entry.getData(detectorName);
		assertNotNull(nxData);
		assertSignal(nxData, NXdetector.NX_DATA);
		assertSame(dataNode, nxData.getDataNode(NXdetector.NX_DATA));
                                                                                                            
		for (int i = 0; i < sizes.length; i++) assertEquals(sizes[i], shape[i]);                            
		                                                                                                    
		// Make sure none of the numbers are NaNs. The detector                                             
		// is expected to fill this scan with non-nulls.                                                    
        final PositionIterator it = new PositionIterator(shape);                                            
        while(it.hasNext()) {
        	int[] next = it.getPos();
        	assertFalse(Double.isNaN(dataset.getDouble(next)));
        }

		// Check axes
        final IPosition      pos = scanModel.getPositionIterable().iterator().next();
 
        // Append _value_demand to each name in scannable names list, and appends
        // the item "." 3 times to the resulting list
        String[] expectedAxesNames = Stream.concat(pos.getNames().stream().map(x -> x + "_value_set"),
        		Collections.nCopies(3, ".").stream()).toArray(String[]::new);
        assertAxes(nxData, expectedAxesNames);
        
        if (role == MonitorRole.PER_POINT) {
            assertPerPointMonitors(scanner, monitorNames, sizes);
        } else if (role == MonitorRole.PER_SCAN) {
        	assertPerScanMonitors(scanner, monitorNames);
        }
	}

	private void assertPerPointMonitors(IRunnableDevice<ScanModel> scanner, 
			                            final List<String> monitorNames,
			                            int... sizes) throws Exception {
		
        final IPosition      pos = scanner.getModel().getPositionIterable().iterator().next();

        NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		String detectorName = scanner.getModel().getDetectors().get(0).getName();
		NXdata nxData = entry.getData(detectorName);

        final Collection<String> scannableNames = pos.getNames();
        final List<String> allNames = new ArrayList<>(scannableNames);
        allNames.addAll(monitorNames);

        int[] defaultDimensionMappings = IntStream.range(0, sizes.length).toArray();
        for (int i = 0; i < allNames.size(); i++) {
        	String deviceName = allNames.get(i);
        	// This test uses NXpositioner for all scannables and monitors
        	NXpositioner positioner = instrument.getPositioner(deviceName);
        	assertNotNull(positioner);
        	String nxDataFieldName;
        	
        	DataNode dataNode = positioner.getDataNode("value_set");
        	IDataset dataset = dataNode.getDataset().getSlice();
        	int[] shape = dataset.getShape();
			assertEquals(1, shape.length);
			if (i < scannableNames.size()) {
				// in practise monitors wouldn't have the 'demand' field
				assertEquals(sizes[i], shape[0]);
				nxDataFieldName = deviceName + "_value_set";
				assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
				assertIndices(nxData, nxDataFieldName, i);
				assertTarget(nxData, nxDataFieldName, rootNode,
						"/entry/instrument/" + deviceName + "/value_set");
			}
			
			// Actual values should be scanD
			dataNode = positioner.getDataNode(NXpositioner.NX_VALUE);
    		dataset = dataNode.getDataset().getSlice();
    		shape = dataset.getShape();
    		assertArrayEquals("The value of monitor, '"+deviceName+"' is incorrect", sizes, shape);
    		
    		nxDataFieldName = deviceName + "_" + NXpositioner.NX_VALUE;
    		assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
    		assertIndices(nxData, nxDataFieldName, defaultDimensionMappings);
    		assertTarget(nxData, nxDataFieldName, rootNode,
    				"/entry/instrument/" + deviceName + "/" + NXpositioner.NX_VALUE);
		}
	}
	
	private void assertPerScanMonitors(IRunnableDevice<ScanModel> scanner, List<String> perScanNames) throws Exception {
		
        NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();

		Collection<IScannable<?>> perScan  = scanner.getModel().getMonitors().stream().filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_SCAN).collect(Collectors.toList());

		// check each metadata scannable has been written correctly
		for (IScannable<?> scannable : perScan) {
	
			String name = scannable.getName();
			//System.out.println("Checking '"+name+"' is ok!");
			assertTrue(perScanNames.contains(name));
			
			NXobject nexusObject = (NXobject) instrument.getGroupNode(name);

			// Check that the nexus object is of the expected base class
			assertNotNull("The scannable '"+name+"' could not be found.", nexusObject);
			assertEquals(name, nexusObject.getString("name"));
			assertEquals(0, nexusObject.getNumberOfGroupNodes());
			assertEquals(1, nexusObject.getNumberOfAttributes());

        	DataNode dataNode = nexusObject.getDataNode("value");
        	IDataset dataset = dataNode.getDataset().getSlice();
        	
        	assertEquals(0, dataset.getRank()); // A scalar value not the shape of the scan.
			//System.out.println("The scannable '"+name+"' was ok!");
		}
	}


	private IRunnableDevice<ScanModel> createNestedStepScanWithMonitors(final IRunnableDevice<?> detector, List<String> monitorNames, int... size) throws Exception {
		
		// Create scan points for a grid and make a generator
		StepModel smodel;
		int ySize = size[size.length-1];
		if (ySize-1>0) {
			smodel = new StepModel("yNex", 10,20,11d/ySize);
		} else {
			smodel = new StepModel("yNex", 10,20,30); // Will generate one value at 10
		}
		
		IPointGenerator<?> stepGen = gservice.createGenerator(smodel);
		assertEquals(ySize, stepGen.size());

		IPointGenerator<?>[] gens = new IPointGenerator<?>[size.length];
		// We add the outer scans, if any
		if (size.length > 1) { 
			for (int dim = size.length-2; dim>-1; dim--) {
				final StepModel model;
				if (size[dim]-1>0) {
				    model = new StepModel("neXusScannable"+(dim+1), 10,20,11d/(size[dim]));
				} else {
					model = new StepModel("neXusScannable"+(dim+1), 10,20,30); // Will generate one value at 10
				}
				final IPointGenerator<?> step = gservice.createGenerator(model);
				gens[dim] = step;
			}
		}
		
		gens[size.length - 1] = stepGen;
		IPointGenerator<?> gen = gservice.createCompoundGenerator(gens);
	
		// Create the model for a scan.
		final ScanModel  scanModel = new ScanModel();
		scanModel.setPositionIterable(gen);
		scanModel.setDetectors(detector);
		scanModel.setMonitors(createMonitors(monitorNames));
		
		// Create a file to scan into.
		scanModel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to "+scanModel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(scanModel, null);
		
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

	private List<IScannable<?>> createMonitors(List<String> monitorNames) throws ScanningException {
		final List<IScannable<?>> ret = new ArrayList<IScannable<?>>(monitorNames.size());
		for (String name : monitorNames) ret.add(connector.getScannable(name));
		return ret;
	}


	public static INexusFileFactory getFileFactory() {
		return fileFactory;
	}

	public static void setFileFactory(INexusFileFactory fileFactory) {
		MonitorTest.fileFactory = fileFactory;
	}

}
