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

import java.io.File;
import java.io.IOException;

import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.scan.IScanService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.detector.ConstantVelocityDevice;
import org.eclipse.scanning.example.detector.ConstantVelocityModel;
import org.eclipse.scanning.example.detector.DarkImageDetector;
import org.eclipse.scanning.example.detector.DarkImageModel;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.file.MockFilePathService;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.analysis.ClusterProcessingRunnableDevice;
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
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * 
 * Attempts to mock out a few services so that we can run them in junit
 * not plugin tests.
 * 
 * @author Matthew Gerring
 *
 */
public class NexusTest extends TmpTest {
	
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

	protected MandelbrotModel createMandelbrotModel() {
		MandelbrotModel model = new MandelbrotModel();
		model.setName("mandelbrot");
		model.setRealAxisName("xNex");
		model.setImaginaryAxisName("yNex");
		model.setColumns(64);
		model.setRows(64);
		model.setExposureTime(0.001);
		return model;
	}

}
