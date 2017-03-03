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
package org.eclipse.scanning.test.validation;

import java.io.File;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.device.models.ProcessingModel;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.example.detector.ConstantVelocityDevice;
import org.eclipse.scanning.example.detector.ConstantVelocityModel;
import org.eclipse.scanning.example.detector.DarkImageDetector;
import org.eclipse.scanning.example.detector.DarkImageModel;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmTriggeredDetector;
import org.eclipse.scanning.example.malcolm.DummyMalcolmTriggeredModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.points.validation.ValidatorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.sequencer.analysis.ClusterProcessingRunnableDevice;
import org.eclipse.scanning.sequencer.analysis.ProcessingRunnableDevice;
import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.Before;

public abstract class AbstractValidationTest {

    protected ValidatorService validator;

	@Before
	public void before() throws Exception {
		
		// Make a validator.
		validator = new ValidatorService();
		
		IPointGeneratorService pservice = new PointGeneratorService();
		validator.setPointGeneratorService(pservice);
		
		IRunnableDeviceService dservice  = new RunnableDeviceServiceImpl(new MockScannableConnector(null));
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)dservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		impl._register(ConstantVelocityModel.class, ConstantVelocityDevice.class);
		impl._register(DarkImageModel.class, DarkImageDetector.class);
		impl._register(ProcessingModel.class, ProcessingRunnableDevice.class);
		impl._register(ClusterProcessingModel.class, ClusterProcessingRunnableDevice.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		impl._register(DummyMalcolmTriggeredModel.class, DummyMalcolmTriggeredDetector.class);
		
		validator.setRunnableDeviceService(dservice);
		Services.setValidatorService(validator);
		Services.setRunnableDeviceService(validator.getRunnableDeviceService());
		org.eclipse.scanning.example.Services.setRunnableDeviceService(validator.getRunnableDeviceService());
		
		// Make a few detectors and models...
		PseudoSpringParser parser = new PseudoSpringParser();
		parser.parse(getClass().getResourceAsStream("test_detectors.xml"));
		
		IRunnableDevice<DummyMalcolmModel> device = dservice.getRunnableDevice("malcolm");
		
		// Just for testing we give it a dir.
		File dir = File.createTempFile("fred", ".nxs").getParentFile();
		device.getModel().setFileDir(dir.getAbsolutePath());
		
		// Just for testing, we make the detector legal.
		AbstractMalcolmDevice<?> mdevice = (AbstractMalcolmDevice<?>)device;
		GridModel gmodel = new GridModel("stage_x", "stage_y");
		gmodel.setBoundingBox(new BoundingBox(10, -10, 100, -100));
		// Cannot set the generator from @PreConfigure in this unit test.
     	mdevice.setPointGenerator(pservice.createGenerator(gmodel));
		

	}

}
