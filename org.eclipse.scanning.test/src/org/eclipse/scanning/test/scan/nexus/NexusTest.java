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
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSignal;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertTarget;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.device.models.JythonModel;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.IScanService;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.detector.ConstantVelocityDevice;
import org.eclipse.scanning.example.detector.ConstantVelocityModel;
import org.eclipse.scanning.example.detector.DarkImageDetector;
import org.eclipse.scanning.example.detector.DarkImageModel;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.detector.PosDetector;
import org.eclipse.scanning.example.detector.PosDetectorModel;
import org.eclipse.scanning.example.detector.RandomLineDevice;
import org.eclipse.scanning.example.detector.RandomLineModel;
import org.eclipse.scanning.example.file.MockFilePathService;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.analysis.ClusterProcessingRunnableDevice;
import org.eclipse.scanning.sequencer.analysis.JythonDevice;
import org.eclipse.scanning.sequencer.analysis.ProcessingRunnableDevice;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.TmpTest;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockOperationService;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Attempts to mock out a few services so that we can run them in junit
 * not plugin tests.
 * 
 * @author Matthew Gerring
 *
 */
public abstract class NexusTest extends TmpTest {
	
	private static Logger logger = LoggerFactory.getLogger(NexusTest.class);
	
	protected static IScannableDeviceService connector;
	protected static IScanService            dservice;
	protected static IPointGeneratorService  gservice;
	protected static INexusFileFactory       fileFactory;


	@BeforeClass
	public static void setServices() throws Exception {
		
		//System.setProperty("org.eclipse.scanning.sequencer.AcquisitionDevice.Metrics", "true");
		connector   = new MockScannableConnector(null);
		dservice    = new RunnableDeviceServiceImpl(connector); // Not testing OSGi so using hard coded service.
		gservice    = new PointGeneratorService();
		fileFactory = new NexusFileFactoryHDF5();		
		
		ActivemqConnectorService.setJsonMarshaller(new MarshallerService(new PointsModelMarshaller()));
		IEventService eservice  = new EventServiceImpl(new ActivemqConnectorService());
		
		IRunnableDeviceService dservice  = new RunnableDeviceServiceImpl(connector);
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		impl._register(ConstantVelocityModel.class, ConstantVelocityDevice.class);
		impl._register(DarkImageModel.class, DarkImageDetector.class);
		impl._register(ProcessingModel.class, ProcessingRunnableDevice.class);
		impl._register(ClusterProcessingModel.class, ClusterProcessingRunnableDevice.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		impl._register(RandomLineModel.class, RandomLineDevice.class);
		impl._register(PosDetectorModel.class, PosDetector.class);
		impl._register(JythonModel.class, JythonDevice.class);
		
		// TODO Perhaps put service setting in super class or utility
		Services.setEventService(eservice);
		Services.setRunnableDeviceService(dservice);
		Services.setGeneratorService(gservice);
		org.eclipse.scanning.example.Services.setPointGeneratorService(gservice);
		Services.setConnector(connector);
		org.eclipse.dawnsci.nexus.ServiceHolder.setNexusFileFactory(fileFactory);
		org.eclipse.scanning.sequencer.ServiceHolder.setTestServices(new LoaderServiceMock(),
				new DefaultNexusBuilderFactory(), new MockOperationService(), new MockFilePathService(), gservice);
	
		org.eclipse.scanning.example.Services.setEventService(eservice);
		org.eclipse.scanning.example.Services.setPointGeneratorService(gservice);		
		org.eclipse.scanning.example.Services.setRunnableDeviceService(dservice);		
		org.eclipse.scanning.example.Services.setScannableDeviceService(connector);		
		
	    clearTmp();
	}


	protected File output;
	
	@Before
	public void createFile() throws IOException {
		output = File.createTempFile("test_nexus", ".nxs");
		output.deleteOnExit();
	}
	
	@After
	public void deleteFile() {
		try {
			output.delete();
		} catch (Exception ne) {
			logger.trace("Cannot delete file!", ne);
		}
	}

	protected static MandelbrotModel createMandelbrotModel() {
		MandelbrotModel model = new MandelbrotModel();
		model.setName("mandelbrot");
		model.setRealAxisName("xNex");
		model.setImaginaryAxisName("yNex");
		model.setColumns(64);
		model.setRows(64);
		model.setExposureTime(0.001);
		return model;
	}


	protected NXroot getNexusRoot(IRunnableDevice<ScanModel> scanner) throws Exception {
		String filePath = ((AbstractRunnableDevice<ScanModel>) scanner).getModel().getFilePath();

		try (NexusFile nf =  fileFactory.newNexusFile(filePath)) {
			nf.openToRead();
			
			TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
			return (NXroot) nexusTree.getGroupNode();
		}
	}
	
	protected NXentry checkNexusFile(IRunnableDevice<ScanModel> scanner, boolean snake, int... sizes) throws Exception {
		return checkNexusFile(scanner, snake, false, sizes);
	}
	
	/**
	 * A folded grid is where a non-rectangular region is applied, meaning that the two grid
     * dimensions are flattened into one. In this case the sizes array passed in should be
     * the expected dataset size.
	 */
	protected NXentry checkNexusFile(IRunnableDevice<ScanModel> scanner, boolean snake,
			boolean foldedGrid, int[] sizes) throws Exception {
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();
		assertEquals(DeviceState.ARMED, scanner.getDeviceState());

		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		// check that the scan points have been written correctly
		assertSolsticeScanGroup(entry, snake, foldedGrid, sizes);
		
		LinkedHashMap<String, List<String>> signalFieldAxes = new LinkedHashMap<>();
		// axis for additional dimensions of a datafield, e.g. image
		signalFieldAxes.put(NXdetector.NX_DATA, Arrays.asList("real", "imaginary"));
		signalFieldAxes.put("spectrum", Arrays.asList("spectrum_axis"));
		signalFieldAxes.put("value", Collections.emptyList());
		
		String detectorName = scanModel.getDetectors().get(0).getName();
		NXdetector nxDetector = instrument.getDetector(detectorName);
		assertEquals(((IDetectorModel)scanner.getModel().getDetectors().get(0).getModel()).getExposureTime(), 
				     nxDetector.getCount_timeScalar().doubleValue(), 1e-15);
		
		// map of detector data field to name of nxData group where that field is the @signal field
		Map<String, String> expectedDataGroupNames =
				signalFieldAxes.keySet().stream().collect(Collectors.toMap(Function.identity(),
				x -> detectorName + (x.equals(NXdetector.NX_DATA) ? "" : "_" + x)));

		// validate the main NXdata generated by the NexusDataBuilder
		Map<String, NXdata> nxDataGroups = entry.getChildren(NXdata.class);
		assertEquals(signalFieldAxes.size(), nxDataGroups.size());
		assertTrue(nxDataGroups.keySet().containsAll(
				expectedDataGroupNames.values()));
		for (String nxDataGroupName : nxDataGroups.keySet()) {
			NXdata nxData = entry.getData(nxDataGroupName);

			String sourceFieldName = nxDataGroupName.equals(detectorName) ? NXdetector.NX_DATA :
				nxDataGroupName.substring(nxDataGroupName.indexOf('_') + 1);
			assertSignal(nxData, sourceFieldName);
			// check the nxData's signal field is a link to the appropriate source data node of the detector
			DataNode dataNode = nxDetector.getDataNode(sourceFieldName);
			IDataset dataset = dataNode.getDataset().getSlice();
			assertSame(dataNode, nxData.getDataNode(sourceFieldName));
			assertTarget(nxData, sourceFieldName, rootNode, "/entry/instrument/" + detectorName
					+ "/" + sourceFieldName);

			// check that the other primary data fields of the detector haven't been added to this NXdata
			for (String primaryDataFieldName : signalFieldAxes.keySet()) {
				if (!primaryDataFieldName.equals(sourceFieldName)) {
					assertNull(nxData.getDataNode(primaryDataFieldName));
				}
			}

			int[] shape = dataset.getShape();
			for (int i = 0; i < sizes.length; i++)
				assertEquals(sizes[i], shape[i]);

			// Make sure none of the numbers are NaNs. The detector
			// is expected to fill this scan with non-nulls.
			final PositionIterator it = new PositionIterator(shape);
			while (it.hasNext()) {
				int[] next = it.getPos();
				assertFalse(Double.isNaN(dataset.getDouble(next)));
			}

			// Check axes
			final IPosition pos = scanModel.getPositionIterable().iterator().next();
			final List<String> scannableNames = pos.getNames();

			// Append _value_demand to each name in list, then add detector axis fields to result
			List<String> expectedAxesNames = Stream.concat(
					scannableNames.stream().
					filter(scannableName -> !(foldedGrid && scannableName.equals(scannableNames.get(scannableNames.size() - 2)))). // filter out inner grid scannable for folded grids 
					map(x -> x + "_value_set"),
					signalFieldAxes.get(sourceFieldName).stream()).collect(Collectors.toList());
			assertAxes(nxData, expectedAxesNames.toArray(new String[expectedAxesNames.size()]));

			int[] defaultDimensionMappings = IntStream.range(0, sizes.length).toArray();
			int i = -1;
			for (String  scannableName : scannableNames) {
				if (!foldedGrid || i != scannableNames.size() - 2) {
					i++; // don't increment if this is the last scannable of a folded grid scan
				}
				
				NXpositioner positioner = instrument.getPositioner(scannableName);
				assertNotNull(positioner);

				dataNode = positioner.getDataNode("value_set");
				dataset = dataNode.getDataset().getSlice();
				shape = dataset.getShape();
				assertEquals(1, shape.length);
				assertEquals(sizes[i], shape[0]);

				String nxDataFieldName = scannableName + "_value_set";
				assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
				assertIndices(nxData, nxDataFieldName, i);
				assertTarget(nxData, nxDataFieldName, rootNode,
						"/entry/instrument/" + scannableName + "/value_set");

				// Actual values should be scanD
				dataNode = positioner.getDataNode(NXpositioner.NX_VALUE);
				dataset = dataNode.getDataset().getSlice();
				shape = dataset.getShape();
				assertArrayEquals(sizes, shape);

				nxDataFieldName = scannableName + "_" + NXpositioner.NX_VALUE;
				assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
				assertIndices(nxData, nxDataFieldName, defaultDimensionMappings);
				assertTarget(nxData, nxDataFieldName, rootNode,
						"/entry/instrument/" + scannableName + "/"
								+ NXpositioner.NX_VALUE);
			}
		}
		
		return entry;
	}
	
	protected IRunnableDevice<ScanModel> createGridScan(final IRunnableDevice<?> detector, File file, boolean snake, int... size) throws Exception {
		
		ScanModel smodel = createGridScanModel(detector, file, snake, size);
		
		// Create a scan and run it without publishing events
		return dservice.createRunnableDevice(smodel, null);
	}
	
	protected IRunnableDevice<ScanModel> createGridScan(final IRunnableDevice<?> detector, File file, IROI region, boolean snake, int... size) throws Exception {
		
		ScanModel smodel = createGridScanModel(detector, file, region, snake, size);
		
		// Create a scan and run it without publishing events
		return dservice.createRunnableDevice(smodel, null);
	}
	
	protected ScanModel createGridScanModel(final IRunnableDevice<?> detector, File file, boolean snake, int... size) throws Exception {
		return createGridScanModel(detector, file, null, snake, size);
	}
	
	protected ScanModel createGridScanModel(final IRunnableDevice<?> detector, File file, IROI region, boolean snake, int... size) throws Exception {
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("xNex");
		gmodel.setFastAxisPoints(size[size.length-1]);
		gmodel.setSlowAxisName("yNex");
		gmodel.setSlowAxisPoints(size[size.length-2]);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		gmodel.setSnake(snake);
		
		IPointGenerator<?> gen = gservice.createGenerator(gmodel,
				region == null ? Collections.emptyList() : Arrays.asList(region));
		
		IPointGenerator<?>[] gens = new IPointGenerator<?>[size.length - 1];
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
				gens[dim] = step;
			}
		}
		gens[size.length - 2] = gen;

		gen = gservice.createCompoundGenerator(gens);
	
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		
		// Create a file to scan into.
		smodel.setFilePath(file.getAbsolutePath());
		System.out.println("File writing to "+smodel.getFilePath());

		return smodel;
	}
	
	protected IRunnableDevice<ScanModel> createSpiralScan(final IRunnableDevice<?> detector, File file) throws Exception {
		ScanModel smodel = createSpiralScanModel(detector, file);
		// Create a scan and run it without publishing events
		return dservice.createRunnableDevice(smodel, null);
	}
		
	protected ScanModel createSpiralScanModel(final IRunnableDevice<?> detector, File file) throws Exception {
		SpiralModel spmodel = new SpiralModel("xNex","yNex");
		spmodel.setScale(2.0);
		spmodel.setBoundingBox(new BoundingBox(0,0,1,1));
	
		IPointGenerator<?> gen = gservice.createGenerator(spmodel);

		final StepModel  model = new StepModel("neXusScannable1", 0,3,1);
		final IPointGenerator<?> step = gservice.createGenerator(model);

		gen = gservice.createCompoundGenerator(new IPointGenerator<?>[]{step,gen});
		
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		
		// Create a file to scan into.
		smodel.setFilePath(file.getAbsolutePath());

		return smodel;
	}

}
