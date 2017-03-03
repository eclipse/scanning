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

import static org.eclipse.scanning.sequencer.analysis.ClusterProcessingRunnableDevice.PROCESSING_QUEUE_NAME;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertScanNotFinished;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.dry.FastRunCreator;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.scan.mock.DummyOperationBean;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ScanClusterProcessingTest extends NexusTest {
	
	private static IConsumer<StatusBean> consumer;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// called after NexusTest.beforeClass()
		BrokerTest.setUpNonOSGIActivemqMarshaller(DummyOperationBean.class);
		BrokerTest.startBroker();
		
		IEventService eventService = new EventServiceImpl(new ActivemqConnectorService());
		ServiceHolder.setEventService(eventService);
		
		URI uri = URI.create(CommandConstants.getScanningBrokerUri());
		consumer = eventService.createConsumer(uri, PROCESSING_QUEUE_NAME,
				"scisoft.operation.STATUS_SET", "scisoft.operation.STATUS_TOPIC");
		// we need a runner, but it doesn't have to do anything
		consumer.setRunner(new FastRunCreator(0, 1, 1, 10, false));
		consumer.start();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		consumer.cleanQueue(consumer.getSubmitQueueName());
		consumer.clearQueue(consumer.getStatusSetName());
		consumer.disconnect();
		BrokerTest.stopBroker();
	}
	
	@Test
	public void testNexusScanWithClusterProcessing() throws Exception {
		testScan(2, 2);
	}
	
	private void testScan(int... shape) throws Exception {
		
		ScanClusterProcessingChecker checker = new ScanClusterProcessingChecker(fileFactory, consumer);
		
		IRunnableDevice<ScanModel> scanner = createGridScan(shape);
		checker.setDevice(scanner);
		assertScanNotFinished(checker.getNexusRoot().getEntry());
		scanner.run(null);
		
		Thread.sleep(100);
		// Check the main nexus file
		checker.checkNexusFile(shape);
		
		// Check the processing bean was submitted successfully
		Thread.sleep(200);
		checker.checkSubmittedBean(true);
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
		gens[size.length - 2 ] = gen;
		
		gen = gservice.createCompoundGenerator(gens);
		
		// Create the model for a scan
		final ScanModel smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		
		// Create a file to scan into
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to " + smodel.getFilePath());
		
		// Set up the Mandelbrot
		MandelbrotModel model = createMandelbrotModel();
		IWritableDetector<MandelbrotModel> detector =
				(IWritableDetector<MandelbrotModel>) dservice.createRunnableDevice(model);
		assertNotNull(detector);
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
				//System.out.println("Ran mandelbrot detector @ "+evt.getPosition());
			}
		});
		
		// TODO set up the processing
		ClusterProcessingModel pmodel = new ClusterProcessingModel();
		pmodel.setDetectorName("mandelbrot");
		pmodel.setProcessingFilePath("/tmp/sum.nxs");
		pmodel.setName("sum");
		
		final IRunnableDevice<ClusterProcessingModel> processor = dservice.createRunnableDevice(pmodel);
		smodel.setDetectors(detector, processor);
		
		final IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		
		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				try {
					System.out.println("Running acquisition scan of size " + fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});
		
		return scanner;
	}

}
