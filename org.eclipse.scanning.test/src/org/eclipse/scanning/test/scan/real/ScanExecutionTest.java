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
package org.eclipse.scanning.test.scan.real;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Test;

/**
 * This class is an object which can be started by sprig on the GDA server.
 * 
 * I receives commands and runs a simple test scan.
 * 
 * @author fri44821
 *
 */
public class ScanExecutionTest extends BrokerTest {
	
	private static IEventService     eventService;
	private static IPointGeneratorService generatorService;
	private static IRunnableDeviceService  runnableDeviceService;
	private static IScannableDeviceService connector;
	private static IMalcolmService   malcService;
	

	public static IScannableDeviceService getConnector() {
		return connector;
	}

	public static void setConnector(IScannableDeviceService connector) {
		ScanExecutionTest.connector = connector;
	}

	public ScanExecutionTest() {
		
	}
	
	/**
	 * 
	 * @param uri - for activemq, for instance BrokerTest.uri
	 * @throws URISyntaxException 
	 * @throws EventException 
	 */
	public ScanExecutionTest(String uri) throws URISyntaxException, EventException {
		this();
		ISubscriber<IBeanListener<TestScanBean>> sub = eventService.createSubscriber(new URI(uri), "org.eclipse.scanning.test.scan.real.test");
		sub.addListener(new IBeanListener<TestScanBean>() {
			@Override
			public void beanChangePerformed(BeanEvent<TestScanBean> evt) {
				try {
					executeTestScan(evt.getBean());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	protected void executeTestScan(TestScanBean bean) throws Exception {
				
		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setExposureTime(0.1);
		dmodel.setName("swmr");
		IWritableDetector<?> detector = (IWritableDetector<?>) runnableDeviceService.createRunnableDevice(dmodel);
		assertNotNull(detector);
		
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
                System.out.println("Ran detector @ "+evt.getPosition());
			}
		});

		IRunnableDevice<ScanModel> scanner = createGridScan(detector, 8, 5); // Outer scan of another scannable, for instance temp.
		scanner.run(null);
		System.out.println("done");
	}
	
	private IRunnableDevice<ScanModel> createGridScan(final IRunnableDevice<?> detector, int... size) throws Exception {
		
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("smx");
		gmodel.setFastAxisPoints(size[size.length-2]);
		gmodel.setSlowAxisName("smy");
		gmodel.setSlowAxisPoints(size[size.length-1]);
		gmodel.setBoundingBox(new BoundingBox(0,0,2,2));
		
		IPointGenerator<?> gen = generatorService.createGenerator(gmodel);
		
		// We add the outer scans, if any
		if (size.length > 2) { 
			for (int dim = size.length-3; dim>-1; dim--) {
				final StepModel model;
				if (size[dim]-1>0) {
				    model = new StepModel("neXusScannable"+(dim+1), 10,20,11d/(size[dim]-1));
				} else {
					model = new StepModel("neXusScannable"+(dim+1), 10,20,30); // Will generate one value at 10
				}
				final IPointGenerator<?> step = generatorService.createGenerator(model);
				gen = generatorService.createCompoundGenerator(step, gen);
			}
		}
	
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		
		// Create a file to scan into.
		File output = File.createTempFile("test_mandel_nexus", ".nxs");
		output.deleteOnExit();
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to "+smodel.getFilePath());

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

	public static IEventService getEventService() {
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		ScanExecutionTest.eventService = eventService;
	}

	public static IPointGeneratorService getGeneratorService() {
		return generatorService;
	}

	public static void setGeneratorService(IPointGeneratorService generatorService) {
		ScanExecutionTest.generatorService = generatorService;
	}

	public static IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}

	public static void setRunnableDeviceService(IRunnableDeviceService scanService) {
		ScanExecutionTest.runnableDeviceService = scanService;
	}

	public static IMalcolmService getMalcService() {
		return malcService;
	}

	public static void setMalcService(IMalcolmService malcService) {
		ScanExecutionTest.malcService = malcService;
	}
	
	/**
	 * This class is designed to be run as a spring object.
	 * It can also be run as a junit plugin test to check OSGi services are injected.
	 */
	@Test
	public void checkServices() throws Exception {
		assertNotNull(eventService);
		assertNotNull(generatorService);
		assertNotNull(runnableDeviceService);
		assertNotNull(connector);
		assertNotNull(malcService);
	}
}
