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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.dawnsci.nexus.builder.impl.DefaultNexusBuilderFactory;
import org.eclipse.dawnsci.remotedataset.test.mock.LoaderServiceMock;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.device.IDeviceController;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IPausableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanEstimator;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.sequencer.watchdog.DeviceWatchdogService;
import org.eclipse.scanning.server.application.Activator;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.AfterClass;

public abstract class AbstractAcquisitionTest {

	protected IRunnableDeviceService        sservice;
	protected IScannableDeviceService       connector;
	protected IPointGeneratorService        gservice;
	protected IEventService                 eservice;
	protected IWritableDetector<MockDetectorModel>       detector;
	protected List<IPosition>               positions;

	public void setupServices() throws Exception {

		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		ActivemqConnectorService.setJsonMarshaller(new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
				));
		eservice  = new EventServiceImpl(new ActivemqConnectorService());

		connector = new MockScannableConnector(null);
		sservice  = new RunnableDeviceServiceImpl(connector);
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)sservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		ServiceHolder.setRunnableDeviceService(sservice);

		MandelbrotModel model = new MandelbrotModel("xNex", "yNex");
		model.setName("mandelbrot");
		model.setExposureTime(0.05);
		impl.createRunnableDevice(model);

		gservice  = new PointGeneratorService();

		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setExposureTime(0.05);
		dmodel.setName("detector");
		detector = (IWritableDetector<MockDetectorModel>) sservice.createRunnableDevice(dmodel);
		
		positions = new ArrayList<>(20);
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
                //System.out.println("Ran mock detector @ "+evt.getPosition());
                positions.add(evt.getPosition());
			}
		});
		
		IDeviceWatchdogService wservice = new DeviceWatchdogService();
		ServiceHolder.setWatchdogService(wservice);
		Services.setWatchdogService(wservice);
	
		// Provide lots of services that OSGi would normally.
		Services.setEventService(eservice);
		Services.setRunnableDeviceService(sservice);
		Services.setGeneratorService(gservice);
		Services.setConnector(connector);
		org.eclipse.scanning.example.Services.setPointGeneratorService(gservice);
		org.eclipse.scanning.example.Services.setEventService(eservice);
		org.eclipse.scanning.example.Services.setRunnableDeviceService(sservice);
		org.eclipse.scanning.example.Services.setScannableDeviceService(connector);
		
		ServiceHolder.setTestServices(new LoaderServiceMock(), new DefaultNexusBuilderFactory(), null, null, gservice);
		org.eclipse.dawnsci.nexus.ServiceHolder.setNexusFileFactory(new NexusFileFactoryHDF5());

	}

	
	@AfterClass
	public static void cleanup() throws Exception {
		ServiceHolder.setRunnableDeviceService(null);
		ServiceHolder.setWatchdogService(null);
	}

	protected IDeviceController createTestScanner(IScannable<?> monitor) throws Exception {
		return createTestScanner(monitor, null, null, 2);
	}
	
	protected <T> IDeviceController createTestScanner(IScannable<?> monitor, IRunnableDevice<T> device, T dmodel, int dims) throws Exception {
		return createTestScanner(monitor, device, dmodel, dims, null, null);
	}
	
	protected <T> IDeviceController createTestScanner(IRunnableDevice<T> device, List<String> axisNames, String filePath) throws Exception {
		return createTestScanner(null, device, null, 2, axisNames, filePath);
	}
	
    private <T> IDeviceController createTestScanner(IScannable<?>     monitor, 
    		                                         IRunnableDevice<T> device, 
    		                                         T dmodel, 
    		                                         int dims,
    		                                         List<String> axisNames,
    		                                         String filePath) throws Exception {

		List<IScanPathModel> models = new ArrayList<>();
		if (dims>2) {
			for (int i = dims; i>2; i--) {
				models.add(new StepModel("T"+i, 290, 292, 1));
			}
		}
		// Create scan points for a grid and make a generator
		if (axisNames==null) axisNames = Arrays.asList("x", "y");
		GridModel gmodel = new GridModel(axisNames.get(0), axisNames.get(1));
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		models.add(gmodel);
		
		IPointGenerator<?> gen = gservice.createCompoundGenerator(new CompoundModel<>(models));
		
		if (dmodel!=null) {
			AnnotationManager manager = new AnnotationManager(Activator.createResolver());
			manager.addDevices(device);
			manager.addContext(new ScanInformation(new ScanEstimator(gen, (Map<String, Object>)null, 1)));
			
			manager.invoke(PreConfigure.class, dmodel, gen);
			if (device instanceof AbstractMalcolmDevice) {
				assertNotNull(((AbstractMalcolmDevice)device).getPointGenerator());
			}
			
			device.configure(dmodel);
			assertNotNull(device.getModel());
			assertEquals(dmodel, device.getModel());
			
			manager.invoke(PostConfigure.class, dmodel, gen);
		}

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
        smodel.setFilePath(filePath);
		
		if (device==null) device = (IRunnableDevice<T>)detector;
		smodel.setDetectors(device);
		if (monitor!=null) smodel.setMonitors(monitor);
		
		// Create a scan and run it without publishing events
		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = sservice.createRunnableDevice(smodel, null, false);
		IDeviceController controller = ServiceHolder.getWatchdogService().create((IPausableDevice<?>)scanner);
		smodel.setAnnotationParticipants(controller.getObjects());
		scanner.configure(smodel);
		
		return controller;
	}
	
	protected IRunnableEventDevice<?> runQuickie() throws Exception {
		return runQuickie(false);
	}

	protected IRunnableEventDevice<?> runQuickie(boolean asych) throws Exception {

		// x and y are level 3
		IDeviceController controller = createTestScanner(null);
		IRunnableEventDevice<?> scanner = (IRunnableEventDevice<?>)controller.getDevice();
		
		List<DeviceState> states = new ArrayList<>();
		// This run should get paused for beam and restarted.
		scanner.addRunListener(new IRunListener() {
			public void stateChanged(RunEvent evt) throws ScanningException {
				states.add(evt.getDeviceState());
			}
		});
		
		if (asych) {
			scanner.start(null);
		} else {
			scanner.run(null);
			
			assertFalse(states.contains(DeviceState.PAUSED));
			assertTrue(states.contains(DeviceState.RUNNING));
			assertFalse(states.contains(DeviceState.SEEKING));
		}
		return scanner;
	}

}
