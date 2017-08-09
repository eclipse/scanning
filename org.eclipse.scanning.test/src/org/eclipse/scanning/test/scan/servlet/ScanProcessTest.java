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
package org.eclipse.scanning.test.scan.servlet;

import static org.eclipse.dawnsci.nexus.NXpositioner.NX_VALUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.RepeatedPointModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.script.ScriptLanguage;
import org.eclipse.scanning.api.script.ScriptRequest;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.file.MockFilePathService;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.RepeatedPointIterator;
import org.eclipse.scanning.points.ScanPointGeneratorFactory;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.points.validation.ValidatorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.sequencer.watchdog.DeviceWatchdogService;
import org.eclipse.scanning.server.servlet.ScanProcess;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

public class ScanProcessTest {
	
	
	@BeforeClass
	public static void init() {
		ScanPointGeneratorFactory.init();
	}
	
	private IRunnableDeviceService      dservice;
	private IScannableDeviceService     connector;
	private IPointGeneratorService      gservice;
	private IEventService               eservice;
	private ILoaderService              lservice;
	private IDeviceWatchdogService      wservice;
	private MockScriptService           sservice;
	private MarshallerService           marshaller;
	private ValidatorService            validator;
	private IFilePathService            fpservice;
	private INexusFileFactory           fileFactory;

	@Before
	public void setUp() throws ScanningException {
		marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
				);
		ActivemqConnectorService.setJsonMarshaller(marshaller);
		eservice  = new EventServiceImpl(new ActivemqConnectorService());
		
		// We wire things together without OSGi here
		// DO NOT COPY THIS IN NON-TEST CODE
		connector = new MockScannableConnector(null);
		dservice  = new RunnableDeviceServiceImpl(connector);
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		
		MandelbrotModel model = new MandelbrotModel("p", "q");
		model.setName("mandelbrot");
		model.setExposureTime(0.00001);
		impl.createRunnableDevice(model);

		gservice  = new PointGeneratorService();
		wservice = new DeviceWatchdogService();
		lservice = new LoaderServiceMock();
		sservice = new MockScriptService();
		fpservice = new MockFilePathService();
		fileFactory = new NexusFileFactoryHDF5();
		
		// Provide lots of services that OSGi would normally.
		Services.setEventService(eservice);
		Services.setRunnableDeviceService(dservice);
		Services.setGeneratorService(gservice);
		Services.setConnector(connector);
		Services.setScriptService(sservice);
		Services.setWatchdogService(wservice);
		fpservice = null; // only used for testMalcolmValidation

		ServiceHolder.setTestServices(lservice, new DefaultNexusBuilderFactory(), null, null, gservice);
		org.eclipse.scanning.example.Services.setPointGeneratorService(gservice);
		org.eclipse.dawnsci.nexus.ServiceHolder.setNexusFileFactory(fileFactory);
		
		validator = new ValidatorService();
		validator.setPointGeneratorService(gservice);
		validator.setRunnableDeviceService(dservice);
		Services.setValidatorService(validator);
	}
	
	@After
	public void teardown() throws Exception {
		if (fpservice != null) {
			File nexusFile = new File(fpservice.getMostRecentPath());
			if (nexusFile.exists()) {
				nexusFile.delete();
				String filename = nexusFile.getName();
				String malcolmDirName = filename.substring(0, filename.lastIndexOf('.'));
				File malcolmOutputDir = new File(nexusFile.getParentFile(), malcolmDirName);
				if (malcolmOutputDir.exists()) {
					for (File file : malcolmOutputDir.listFiles()) {
						file.delete();
					}
					malcolmOutputDir.delete();
				}
			}
		}
	}

	@Test
	public void testScriptFilesRun() throws Exception {
		// Arrange
		ScanBean scanBean = new ScanBean();
		ScanRequest<?> scanRequest = new ScanRequest<>();
		scanRequest.setCompoundModel(new CompoundModel<>(new StepModel("fred", 0, 9, 1)));
		
		ScriptRequest before = new ScriptRequest();
		before.setFile("/path/to/before.py");
		before.setLanguage(ScriptLanguage.PYTHON);
		scanRequest.setBefore(before);
		
		ScriptRequest after = new ScriptRequest();
		after.setFile("/path/to/after.py");
		after.setLanguage(ScriptLanguage.PYTHON);
		scanRequest.setAfter(after);
		
		scanBean.setScanRequest(scanRequest);
		ScanProcess process = new ScanProcess(scanBean, null, true);
		
		// Act
		process.execute();

		// Assert
		List<ScriptRequest> scriptRequests = ((MockScriptService) sservice).getScriptRequests();
		assertThat(scriptRequests.size(), is(2));
		assertThat(scriptRequests, hasItems(before, after));
	}
	
	@Test
	public void testSimpleNest() throws Exception {
		// Arrange
		ScanBean scanBean = new ScanBean();
		ScanRequest<?> scanRequest = new ScanRequest<>();
		
		CompoundModel cmodel = new CompoundModel<>(Arrays.asList(new StepModel("T", 290, 291, 2), new GridModel("xNex", "yNex",2,2)));
		cmodel.setRegions(Arrays.asList(new ScanRegion<IROI>(new RectangularROI(0, 0, 3, 3, 0), "xNex", "yNex")));
		scanRequest.setCompoundModel(cmodel);
		
		final Map<String, Object> dmodels = new HashMap<String, Object>(3);
		MandelbrotModel model = new MandelbrotModel("xNex", "yNex");
		model.setName("mandelbrot");
		model.setExposureTime(0.001);
		dmodels.put("mandelbrot", model);
		scanRequest.setDetectors(dmodels);
		
		final File tmp = File.createTempFile("scan_nested_test", ".nxs");
		tmp.deleteOnExit();
		scanRequest.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		scanBean.setScanRequest(scanRequest);
		ScanProcess process = new ScanProcess(scanBean, null, true);
		
		// Act
		process.execute();

		// Assert
		
	}
	
	@Test
	public void testScannableAndMonitor() throws Exception {
		// Arrange
		ScanBean scanBean = new ScanBean();
		ScanRequest<?> scanRequest = new ScanRequest<>();
		
		CompoundModel cmodel = new CompoundModel<>(Arrays.asList(new StepModel("T", 290, 300, 2), new GridModel("xNex", "yNex",2,2)));
		cmodel.setRegions(Arrays.asList(new ScanRegion<IROI>(new RectangularROI(0, 0, 3, 3, 0), "xNex", "yNex")));
		scanRequest.setCompoundModel(cmodel);
		
		final Map<String, Object> dmodels = new HashMap<String, Object>(3);
		MandelbrotModel model = new MandelbrotModel("xNex", "yNex");
		model.setName("mandelbrot");
		model.setExposureTime(0.001);
		dmodels.put("mandelbrot", model);
		scanRequest.setDetectors(dmodels);
		scanRequest.setMonitorNames(Arrays.asList("T"));
		
		final File tmp = File.createTempFile("scan_nested_test", ".nxs");
		tmp.deleteOnExit();
		scanRequest.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		scanBean.setScanRequest(scanRequest);
		ScanProcess process = new ScanProcess(scanBean, null, true);
		
		// Act
		process.execute();

		// Assert
		NexusFile nf = fileFactory.newNexusFile(tmp.getAbsolutePath());
		nf.openToRead();
		
		TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
		nf.close();
		NXroot root = (NXroot) nexusTree.getGroupNode();
		NXentry entry = root.getEntry();
		NXinstrument instrument = entry.getInstrument();
		NXpositioner tPos = instrument.getPositioner("T");
		IDataset tempDataset = tPos.getValue();
		assertThat(tempDataset, is(notNullValue()));
		assertThat(tempDataset.getShape(), is(equalTo(new int[] { 6, 2, 2 })));
		
		NXdata mandelbrot = entry.getData("mandelbrot");
		assertThat(mandelbrot, is(notNullValue()));
		assertThat(mandelbrot.getDataNode("T"), is(nullValue()));
	}
	
	@Test
	public void testSimpleNestWithSleepInIterator() throws Exception {
		// Arrange
		ScanBean scanBean = new ScanBean();
		ScanRequest<?> scanRequest = new ScanRequest<>();
		
		final int numPoints = 5;
		CompoundModel cmodel = new CompoundModel<>(Arrays.asList(
				new RepeatedPointModel("T1", numPoints, 290.2, 100), new GridModel("xNex", "yNex",2,2)));
		cmodel.setRegions(Arrays.asList(new ScanRegion<IROI>(
				new RectangularROI(0, 0, 3, 3, 0), "xNex", "yNex")));
		scanRequest.setCompoundModel(cmodel);
		
		final Map<String, Object> dmodels = new HashMap<String, Object>(3);
		MandelbrotModel model = new MandelbrotModel("xNex", "yNex");
		model.setName("mandelbrot");
		model.setExposureTime(0.001);
		dmodels.put("mandelbrot", model);
		scanRequest.setDetectors(dmodels);
		
		final File tmp = File.createTempFile("scan_nested_test", ".nxs");
		tmp.deleteOnExit();
		scanRequest.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		scanBean.setScanRequest(scanRequest);
		ScanProcess process = new ScanProcess(scanBean, null, true);
		
		RepeatedPointIterator._setCountSleeps(true);
		
		// Act
		long before = System.currentTimeMillis();
		process.execute();
		long after = System.currentTimeMillis();

		// Assert
		assertTrue("The time to do a scan of roughly 500ms of sleep was "+(after-before), (10000 > (after-before)));
		
		// Important: the number of sleeps must be five
		// NOTE: there are currently ten sleeps, as we iterate through the points in the scan
		// twice to get the scan shape.
		// TODO: DAQ-754. Find some way to avoid iterating through all the points to get the shape.
		assertEquals(numPoints * 2, RepeatedPointIterator._getSleepCount());
	}

	
	@Test
	public void testStartAndEndPos() throws Exception {
		// Arrange
		ScanBean scanBean = new ScanBean();
		ScanRequest<?> scanRequest = new ScanRequest<>();
		scanRequest.setCompoundModel(new CompoundModel<>(new StepModel("fred", 0, 9, 1)));
		
		final MapPosition start = new MapPosition();
		start.put("p", 1.0);
		start.put("q", 2.0);
		start.put("r", 3.0);
		scanRequest.setStart(start);
		
		final MapPosition end = new MapPosition();
		end.put("p", 6.0);
		end.put("q", 7.0);
		end.put("r", 8.0);
		scanRequest.setEnd(end);
		
		scanBean.setScanRequest(scanRequest);
		ScanProcess process = new ScanProcess(scanBean, null, true);
		
		// Act
		process.execute();
		
		// Assert
		for (String scannableName : start.getNames()) {
			final Number startPos = start.getValue(scannableName);
			final Number endPos = end.getValue(scannableName);
			
			IScannable<Number> scannable = connector.getScannable(scannableName);
			MockScannable mockScannable = (MockScannable) scannable;
			
			mockScannable.verify(start.getValue(scannableName), start);
			mockScannable.verify(end.getValue(scannableName), end);
			
			final List<Number> values = mockScannable.getValues();
			assertThat(values.get(0), is(equalTo(startPos)));
			assertThat(values.get(values.size() - 1), is(equalTo(endPos)));
		}
	}
	
	@Ignore("Got broken by scisoft change...")
	@Test
	public void testMalcolmValidation() throws Exception {
		// Arrange
		fpservice = new MockFilePathService();
		Services.setFilePathService(fpservice);
		new ServiceHolder().setFilePathService(fpservice);
		
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("stage_x");
		gmodel.setFastAxisPoints(5);
		gmodel.setSlowAxisName("stage_y");
		gmodel.setSlowAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		
		DummyMalcolmModel dmodel = new DummyMalcolmModel();
		dmodel.setName("malcolm");
		dmodel.setExposureTime(0.1);
		dservice.createRunnableDevice(dmodel);
		
		ScanBean scanBean = new ScanBean();
		ScanRequest<?> scanRequest = new ScanRequest<>();
		scanRequest.setCompoundModel(new CompoundModel<>(gmodel));
		scanRequest.putDetector("malcolm", dmodel);
		
		scanBean.setScanRequest(scanRequest);
		ScanProcess process = new ScanProcess(scanBean, null, true);
		
		// Act
		process.execute();
		
		// Nothing to assert. This test was written to check that the malcolm device is 
		// properly initialized before validation occurs. If this didn't happen, an
		// exception would be thrown by DummyMalcolmDevice.validate()
	}

}
