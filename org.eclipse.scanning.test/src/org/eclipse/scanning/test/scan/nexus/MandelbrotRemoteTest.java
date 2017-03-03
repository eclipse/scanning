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
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSignal;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertTarget;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.dawnsci.analysis.api.io.IRemoteDatasetService;
import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.dawnsci.remotedataset.client.RemoteDatasetServiceImpl;
import org.eclipse.dawnsci.remotedataset.server.DataServer;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.DataEvent;
import org.eclipse.january.dataset.IDataListener;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.IDatasetConnector;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


@Ignore("You must have jetty in the runner path to run this and travis does not")
public class MandelbrotRemoteTest extends NexusTest {
	
	private static IRemoteDatasetService   dataService;


	private static IWritableDetector<MandelbrotModel> detector;

	private static DataServer server;


	private MandelbrotModel model;

	@Before
	public void before() throws Exception {
		
	
        // Start the DataServer
		int port   = getFreePort(8080);
		server = new DataServer();
		server.setPort(port);
		server.start(false);
		
		System.out.println("Started DataServer on port "+port);
		
		this.model = createMandelbrotModel();
		model.setExposureTime(0.1);
		
		detector = (IWritableDetector<MandelbrotModel>)dservice.createRunnableDevice(model);
		assertNotNull(detector);
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
                //System.out.println("Ran mandelbrot detector @ "+evt.getPosition());
			}
		});

		dataService = new RemoteDatasetServiceImpl();
		org.eclipse.dawnsci.remotedataset.ServiceHolder.setLoaderService(new LoaderServiceMock());
	}
	
	@After
	public void stop() {
		server.stop();
	}

	@Test
	public void test2DNexusScan() throws Exception {
		testScan(8,5);
	}
	
	@Test
	public void test3DNexusScan() throws Exception {
		testScan(3,2,5);
	}
	
	@Test
	public void test4DNexusScan() throws Exception {
		testScan(2,3,2,5);
	}
	
	private void testScan(int... shape) throws Exception {
		
		IRunnableDevice<ScanModel> scanner = createGridScan(detector, shape); // Outer scan of another scannable, for instance temp.
		((AbstractRunnableDevice<ScanModel>)scanner).start(null); // Does the scan in a thread.
		
		// We now connect a remote dataset looking at the scan file and see if we get information about it.
		final ScanModel mod = ((AbstractRunnableDevice<ScanModel>)scanner).getModel();
		String filePath = ((AbstractRunnableDevice<ScanModel>)scanner).getModel().getFilePath();
		
		IDatasetConnector remote = dataService.createRemoteDataset("localhost", server.getPort());
		remote.setPath(filePath);
		remote.setDatasetName("/entry/instrument/"+mod.getDetectors().get(0).getName()+"/data");
		remote.setWritingExpected(true); // We know that we are writing to this file, so we declare it.
		
		remote.connect();
		scanner.latch(1, TimeUnit.SECONDS);
		try {
			
			final List<DataEvent> events = new ArrayList<DataEvent>(7);
			remote.addDataListener(new IDataListener() {				
				@Override
				public void dataChangePerformed(DataEvent evt) {
					System.out.println("Data change notified for "+evt.getFilePath());
					System.out.println("Shape is "+Arrays.toString(evt.getShape()));
					events.add(evt);
				}
			});
			
			
			// Wait until the scan end.
			final CountDownLatch latch = new CountDownLatch(1);
			((AbstractRunnableDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
				@Override
				public void runPerformed(RunEvent evt) throws ScanningException{
					try {
						scanner.latch(500, TimeUnit.MILLISECONDS); // Finish off writing nexus file. The Remote dataset event lags slightly while nexus flushes.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					latch.countDown();
				}
			});
			latch.await();
			
			assertTrue("There should be some events, there were: "+events.size(), events.size()>0); // There won't be the full 40 but there should be a lot of them.
			
			int[] fshape = ArrayUtils.addAll(shape, new int[]{model.getRows(), model.getColumns()});
			int[] eshape = events.get(events.size()-1).getShape();
			assertTrue(Arrays.equals(fshape, eshape));
			
		} finally {
			remote.disconnect();
		}
		
		// Check we reached ready (it will normally throw an exception on error)
		checkNexusFile(scanner, shape); // Step model is +1 on the size
	}

	private void checkNexusFile(IRunnableDevice<ScanModel> scanner, int... sizes) throws NexusException, ScanningException, DatasetException {
		
		final ScanModel mod = ((AbstractRunnableDevice<ScanModel>)scanner).getModel();
		assertEquals(DeviceState.READY, scanner.getDeviceState());
		
		String filePath = ((AbstractRunnableDevice<ScanModel>)scanner).getModel().getFilePath();
		NexusFile nf = fileFactory.newNexusFile(filePath);
		nf.openToRead();
		
		TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
		NXroot rootNode = (NXroot) nexusTree.getGroupNode();
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		// check that the scan points have been written correctly
		assertSolsticeScanGroup(entry, false, sizes);
		
		LinkedHashMap<String, List<String>> detectorDataFields = new LinkedHashMap<>();
		// axis for additional dimensions of a datafield, e.g. image
		detectorDataFields.put(NXdetector.NX_DATA, Arrays.asList("real", "imaginary"));
		detectorDataFields.put("spectrum", Arrays.asList("spectrum_axis"));
		detectorDataFields.put("value", Collections.emptyList());
		
		String detectorName = mod.getDetectors().get(0).getName();
		NXdetector detector = instrument.getDetector(detectorName);
		// map of detector data field to name of nxData group where that field is the @signal field
		Map<String, String> expectedDataGroupNames = 
				detectorDataFields.keySet().stream().collect(Collectors.toMap(Function.identity(),
						x -> detectorName + (x.equals(NXdetector.NX_DATA) ? "" : "_" + x)));
		
		// validate the NXdata generated by the NexusDataBuilder
		Map<String, NXdata> nxDataGroups = entry.getChildren(NXdata.class);
		assertEquals(detectorDataFields.size(), nxDataGroups.size());
		assertTrue(nxDataGroups.keySet().containsAll(expectedDataGroupNames.values()));
		
		for (String nxDataGroupName : nxDataGroups.keySet()) {
			NXdata nxData = entry.getData(nxDataGroupName);
			String sourceFieldName = nxDataGroupName.equals(detectorName) ? NXdetector.NX_DATA :
				nxDataGroupName.substring(nxDataGroupName.indexOf('_') + 1);
			assertSignal(nxData, sourceFieldName);
			// check the nxData's signal field is a link to the data node of the detector
			DataNode dataNode = detector.getDataNode(sourceFieldName);
			IDataset dataset = dataNode.getDataset().getSlice();
			assertSame(dataNode, nxData.getDataNode(sourceFieldName));
			
			int[] shape = dataset.getShape();
	
			for (int i = 0; i < sizes.length; i++) assertEquals(sizes[i], shape[i]);
	
			// Make sure none of the numbers are NaNs. The detector
			// is expected to fill this scan with non-nulls.
			final PositionIterator it = new PositionIterator(shape);
			while (it.hasNext()) {
				int[] next = it.getPos();
				assertFalse(Double.isNaN(dataset.getDouble(next)));
			}
	
			// Check axes
			final IPosition pos = mod.getPositionIterable().iterator().next();
			final Collection<String> names = pos.getNames();
	
			// Append _value_demand to each name in list, then add detector axis fields to result
			List<String> expectedAxesNames = Stream.concat(
					names.stream().map(x -> x + "_value_set"),
					detectorDataFields.get(sourceFieldName).stream()).collect(Collectors.toList());
			assertAxes(nxData, expectedAxesNames.toArray(new String[expectedAxesNames.size()]));
			
			int[] defaultDimensionMappings = IntStream.range(0, sizes.length).toArray();
			int i = -1;
			for (String  positionerName : names) {
				
			    i++;
				NXpositioner positioner = instrument.getPositioner(positionerName);
				assertNotNull(positioner);
				dataNode = positioner.getDataNode("value_set");
				
				dataset = dataNode.getDataset().getSlice();
				shape = dataset.getShape();
				assertEquals(1, shape.length);
				assertEquals(sizes[i], shape[0]);
	
				String nxDataFieldName = positionerName + "_value_set";
				assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
				assertIndices(nxData, nxDataFieldName, i);
				assertTarget(nxData, nxDataFieldName, rootNode,
						"/entry/instrument/" + positionerName + "/value_set");
				
				// Actual values should be scanD
				dataNode = positioner.getDataNode(NXpositioner.NX_VALUE);
				dataset = dataNode.getDataset().getSlice();
				shape = dataset.getShape();
				assertArrayEquals(sizes, shape);
				
				nxDataFieldName = positionerName + "_" + NXpositioner.NX_VALUE;
				assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
				assertIndices(nxData, nxDataFieldName, defaultDimensionMappings);
				assertTarget(nxData, nxDataFieldName, rootNode,
						"/entry/instrument/" + positionerName + "/" + NXpositioner.NX_VALUE);
			}
		}
	}

	private IRunnableDevice<ScanModel> createGridScan(final IRunnableDevice<?> detector, int... size) throws Exception {
		
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
		smodel.setDetectors(detector);
		
		// Create a file to scan into.
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to "+smodel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		
		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
					public void runWillPerform(RunEvent evt)
							throws ScanningException {
						try {
							System.out.println("Running acquisition scan of size "+fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}

	public static INexusFileFactory getFileFactory() {
		return fileFactory;
	}

	public static void setFileFactory(INexusFileFactory fileFactory) {
		MandelbrotRemoteTest.fileFactory = fileFactory;
	}

	public static int getFreePort(final int startPort) {
		
	    int port = startPort;
	    while(!isPortFree(port)) port++;
	    	
	    return port;
	}


	/**
	 * Checks if a port is free.
	 * @param port
	 * @return
	 */
	public static boolean isPortFree(int port) {

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    	// Swallow this, it's not free
	    	return false;
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	}

	public static IRemoteDatasetService getDataService() {
		return dataService;
	}

	public static void setDataService(IRemoteDatasetService dataService) {
		MandelbrotRemoteTest.dataService = dataService;
	}

}
