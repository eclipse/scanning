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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

public class BenchmarkScanTest extends BrokerTest {
	
	
	private static long nexusSmall, nexusMedium, nexusSmallEvents, nexusMediumEvents;
	@BeforeClass
	public static void ensureLambdasLoaded() {
		// This is required so that we don't benchmark lambda loading.
		Arrays.asList(1,2,3).stream().map(x -> x+1).collect(Collectors.toList());
	}
		
	@AfterClass
	public static void checkTimes() throws Exception {
		assertTrue(nexusSmallEvents<(nexusSmall+20));
		assertTrue(nexusMediumEvents<(nexusMedium+20));
	}
	
	private IRunnableDeviceService      dservice;
	private IScannableDeviceService     connector;
	private IPointGeneratorService      gservice;
	private IEventService               eservice;
	private ILoaderService              lservice;
	
	@Before
	public void start() throws Exception {
		
		setUpNonOSGIActivemqMarshaller();
		eservice  = new EventServiceImpl(new ActivemqConnectorService());

		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		connector = new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC));
		dservice  = new RunnableDeviceServiceImpl(connector);
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);

		gservice  = new PointGeneratorService();
		lservice  = new LoaderServiceMock();
		
		// Provide lots of services that OSGi would normally.
		Services.setEventService(eservice);
		Services.setRunnableDeviceService(dservice);
		Services.setGeneratorService(gservice);
		Services.setConnector(connector);
		ServiceHolder.setTestServices(lservice, new DefaultNexusBuilderFactory(), null);
		org.eclipse.dawnsci.nexus.ServiceHolder.setNexusFileFactory(new NexusFileFactoryHDF5());
	}
	
	/**
	 * Required unless we use a benchmarking framework. However
	 * the test measures each increase in size and uses multiples
	 * plus the fudge. This avoids some of the benchmarking issues.
	 */
	private final static long fudge = 1200;

	@Test
	public void testLinearScanNoNexus() throws Exception {
	
		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setCreateImage(false);  // Would put our times off.
		dmodel.setExposureTime(0.001); // Sleep 1ms on the mock detector.
		dmodel.setName("detector");
		IRunnableDevice<MockDetectorModel> detector = dservice.createRunnableDevice(dmodel);

		benchmarkStep(new BenchmarkBean(256, 2000l, 1, true, detector)); // set things up
		
		// Benchmark things. A good idea to do nothing much else on your machine for this...
		long point1     = benchmarkStep(new BenchmarkBean(1,     200, 1, detector)); // should take not more than 2ms sleep + scan time
		
		// should take not more than 64*point1 + scan time
		long point10    = benchmarkStep(new BenchmarkBean(10,    (10*point1)+fudge,  10, detector));  
		
		// should take not more than 4*point64 sleep + scan time
		long point100   = benchmarkStep(new BenchmarkBean(100,   (10*point10)+fudge, 12L,   10, detector));  
		
//		// should take not more than 4*point64 sleep + scan time
//		long point1000  = benchmarkStep(new BenchmarkBean(1000,  (10*point100)+fudge, 10L, 10, detector));  
//		
//		// should take not more than 4*point64 sleep + scan time
//		long point10000 = benchmarkStep(new BenchmarkBean(10000, (10*point1000)+fudge, 10L, 10, detector));  
	}
	
	@Test
	public void testLinearScanNexusSmall() throws Exception {
		System.out.println(">> testLinearScanNexusSmall");
	    nexusSmall = benchmarkNexus(64, 25L);
		System.out.println(">> done");
	}

	@Test
	public void testLinearScanNexusMedium() throws Exception {
		System.out.println(">> testLinearScanNexusMedium");
	    nexusMedium = benchmarkNexus(256, 50L);
		System.out.println(">> done");
	}

	private long benchmarkNexus(int imageSize, long max)  throws Exception {
		
		MandelbrotModel model = new MandelbrotModel();
		model.setName("mandelbrot");
		model.setRealAxisName("xNex");
		model.setImaginaryAxisName("yNex");
		model.setColumns(imageSize);
		model.setRows(imageSize);
		model.setMaxIterations(1);
		model.setExposureTime(0.0);

		IRunnableDevice<MandelbrotModel> detector = dservice.createRunnableDevice(model);

		final BenchmarkBean bean = new BenchmarkBean(256, 5000l, 1, true, detector);
		File output = File.createTempFile("test_mandel_nexus", ".nxs");
		output.deleteOnExit();
		bean.setFilePath(output.getAbsolutePath());
		
		try {
			benchmarkStep(bean); // set things up
			
			// Benchmark things. A good idea to do nothing much else on your machine for this...
			long point1     = benchmarkStep(new BenchmarkBean(1, 100, 1, detector, output)); // should take not more than 2ms sleep + scan time
			
			// should take not more than 64*point1 + scan time
			long point10    = benchmarkStep(new BenchmarkBean(10,    (10*point1)+fudge, max,   10, detector, output));  
			
			// should take not more than 4*point64 sleep + scan time
			long point100   = benchmarkStep(new BenchmarkBean(100,   (10*point10)+fudge, max,  10, detector, output));  
			
			return point100/100;
			
		} finally {
		    output.delete();
		}
	}
	
	@Test
	public void testLinearScanNexusSmallWithEvents() throws Exception {
		System.out.println(">> testLinearScanNexusSmallWithEvents");
		nexusSmallEvents = benchmarkNexusWithEvents(64, 50L);
		System.out.println(">> done");
	}

	@Test
	public void testLinearScanNexusMediumWithEvents() throws Exception {
		System.out.println(">> testLinearScanNexusMediumWithEvents");
		nexusMediumEvents = benchmarkNexusWithEvents(256, 50L);
		System.out.println(">> done");
	}

	private long benchmarkNexusWithEvents(int imageSize, long max)  throws Exception {
		
		// We create a publisher and subscriber for the scan.
		final IPublisher<ScanBean> publisher = eservice.createPublisher(uri, IEventService.STATUS_TOPIC);
		
		final ISubscriber<IScanListener> subscriber = eservice.createSubscriber(uri, IEventService.STATUS_TOPIC);
		final Set<DeviceState> states = new HashSet<DeviceState>(5);
		subscriber.addListener(new IScanListener() {		
			@Override
			public void scanStateChanged(ScanEvent evt) {
//				System.out.println("State Change : "+evt.getBean().getDeviceState());
				states.add(evt.getBean().getDeviceState());
			}
			@Override
			public void scanEventPerformed(ScanEvent evt) {

			}
		});
		
		File output = null;
		try {
			MandelbrotModel model = new MandelbrotModel();
			model.setName("mandelbrot");
			model.setRealAxisName("xNex");
			model.setImaginaryAxisName("yNex");
			model.setColumns(imageSize);
			model.setRows(imageSize);
			model.setMaxIterations(1);
			model.setExposureTime(0.0);
		
			IRunnableDevice<MandelbrotModel> detector = dservice.createRunnableDevice(model);
	
			final BenchmarkBean bean = new BenchmarkBean(256, 5000l, 1, true, detector);
			output = File.createTempFile("test_mandel_nexus", ".nxs");
			output.deleteOnExit();
			bean.setFilePath(output.getAbsolutePath());
			bean.setPublisher(publisher);
		
			benchmarkStep(bean); // set things up
			
			// Benchmark things. A good idea to do nothing much else on your machine for this...
			long point1     = benchmarkStep(new BenchmarkBean(1, 200, 1, detector, output)); // should take not more than 2ms sleep + scan time
			
			// should take not more than 64*point1 + scan time
			long point10    = benchmarkStep(new BenchmarkBean(10,    (10*point1)+fudge, max,   10, detector, output, publisher));  
			
			// should take not more than 4*point64 sleep + scan time
			long point100   = benchmarkStep(new BenchmarkBean(100,   (10*point10)+fudge, max,   10, detector, output, publisher));  
			
			assertTrue(states.size()==3); // CONFIGURING, READY, RUNNING 
			return point100/100;
			
		} finally {
			if (publisher!=null) publisher.disconnect();
		    if (output!=null) output.delete();
		}
	}


	/**
	 * 
	 * @param size
	 * @param reqTime
	 * @param tries - we try several times to get the time because sometimes the gc will run.
	 * @param silent
	 * @return
	 * @throws Exception
	 */
	private long benchmarkStep(final BenchmarkBean bean) throws Exception {
				
		if (!bean.isSilent()) System.out.println("\nChecking that "+bean.getSize()+" points take "+bean.getReqTime()+"ms or less to run. Using "+bean.getTries()+" tries.");

		// Before, run, after, check time.
		final StepModel smodel = new StepModel(bean.getScannableName(), 0, bean.getSize(), 1);
		final GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("xNex");
		gmodel.setSlowAxisName("yNex");
		gmodel.setFastAxisPoints(1);
		gmodel.setSlowAxisPoints(1);
		
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(2);
		box.setSlowAxisStart(2);
		box.setFastAxisLength(5);
		box.setSlowAxisLength(5);
		gmodel.setBoundingBox(box);
	
		ScanModel scanModel = createTestScanner(bean.getDetector(), smodel, gmodel);
		if (bean.getFilePath()!=null) {
			scanModel.setFilePath(bean.getFilePath());
			System.out.println("File writing to "+bean.getFilePath());
		}
		
		// Create configured device.
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(scanModel, bean.getPublisher());
		
		long time = 0l;
		for (int i = 0; i < bean.getTries(); i++) {
			long before = System.currentTimeMillis();
			if (scanModel.getFilePath()!=null) {
				System.out.println("Scanning to "+(new File(scanModel.getFilePath())).getName());
			}
			scanner.run(null);
			long after = System.currentTimeMillis();

			time = (after-before);

			if (time>bean.getReqTime()) {
				continue;
			}
			
			break; // We got it low enough
		}
		
		final IDetectorModel dmodel = bean.getDetector().getModel();
		if (!bean.isSilent()) {
			System.out.println(bean.getSize()+" point(s) took "+time+"ms with detector exposure set to "+dmodel.getExposureTime()+"s");
			
			long pointTime = (time/bean.getSize());
			System.out.println("That's "+pointTime+"ms per point");
			assertTrue("It should not take longer than "+bean.getReqTime()+"ms to scan "+bean.getSize()+" points with mock devices set to 1 ms exposure.", 
				    time<bean.getReqTime());
			
			assertTrue("The average scan point time is over "+bean.getMaxPointTime()+" it's "+pointTime, pointTime<=bean.getMaxPointTime());
		}
		
		if (scanModel.getFilePath()!=null) {
			final IDataHolder   dh = lservice.getData(scanModel.getFilePath(), null);
			final ILazyDataset  lz = dh.getLazyDataset("/entry/instrument/mandelbrot/data");	
			System.out.println("Wrote dataset of shape: "+Arrays.toString(lz.getShape()));
		}
		
		// Attempt to make the VM roughly do the same thing each run.
		System.gc();
		System.runFinalization();
		Thread.sleep(100); // Hopefully something happens, but probably not unless we intentionally fill the heap.
		                   // We just need to avoid a gc during the benchmarking phase.
		
		return time;
	}

	private ScanModel createTestScanner(IRunnableDevice<?> detector, IScanPathModel... models) throws Exception {
				
		final IPointGenerator<?>[] gens = new IPointGenerator[models.length];
		for (int i = 0; i < models.length; i++)  gens[i] = gservice.createGenerator(models[i]);

		IPointGenerator<?> gen = gservice.createCompoundGenerator(gens);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		
		// Create a scan and run it without publishing events
		return smodel;
	}

}
