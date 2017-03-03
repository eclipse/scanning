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
package org.eclipse.scanning.test.scan.preprocess;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.malcolm.models.MapMalcolmModel;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.process.IPreprocessor;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.preprocess.ExamplePreprocessor;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Before;
import org.junit.Test;

public class PreprocessTest {

	protected IPreprocessor preprocessor;

	@Before
	public void before() {
		preprocessor = new ExamplePreprocessor();
	}

	@Test
	public void testSimplePreprocess() throws Exception {
		
		ScanRequest<?> req = createStepRequest();
		req = preprocessor.preprocess(req);
		assertNotNull(req);

		StepModel step = (StepModel)req.getCompoundModel().getModels().toArray()[0];
		assertTrue(step.getName().equals("xfred"));
	}
	
	@Test
	public void testGridPreprocess() throws Exception {
		
		ScanRequest<?> req = createGridRequest();
		req = preprocessor.preprocess(req);
		assertNotNull(req);

		GridModel grid = (GridModel)req.getCompoundModel().getModels().toArray()[0];
		assertTrue(grid.getFastAxisName().equals("xfred"));
		assertTrue(grid.getSlowAxisName().equals("yfred"));
	}
	
	@Test
	public void testGridStepPreprocess() throws Exception {
		
		ScanRequest<?> req = createStepGridRequest(5);
		req = preprocessor.preprocess(req);
		assertNotNull(req);

		// TODO 
	}

	@Test
	public void testMalcolmPreprocess() throws Exception {
		
		ScanRequest<?> req = createMalcolmRequest();
		req = preprocessor.preprocess(req);
		assertNotNull(req);

		// TODO 
	}
	
	private ScanRequest<?> createStepRequest() throws IOException {
		
		final ScanRequest<?> req = new ScanRequest<IROI>();
		req.setCompoundModel(new CompoundModel(new StepModel("fred", 0, 9, 1)));
		req.setMonitorNames(Arrays.asList("monitor"));

		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.1);
		req.putDetector("detector", dmodel);

		return req;
	}

	private ScanRequest<?> createStepGridRequest(int outerScanNum) throws IOException {
		
		
		final ScanRequest<?> req = new ScanRequest<IROI>();
		// Create a grid scan model
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel gmodel = new GridModel();
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(box);
		gmodel.setFastAxisName("xNex");
		gmodel.setSlowAxisName("yNex");

		// 2 models
		List<IScanPathModel> models = new ArrayList<>(outerScanNum+1);
		for (int i = 0; i < outerScanNum; i++) {
			models.add(new StepModel("neXusScannable"+i, 1, 2, 1));
		}
		models.add(gmodel);
		req.setCompoundModel(new CompoundModel(models));
		req.setMonitorNames(Arrays.asList("monitor"));
		
		final File tmp = File.createTempFile("scan_servlet_test", ".nxs");
		tmp.deleteOnExit();
		req.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		// 2 detectors
		final MandelbrotModel mandyModel = new MandelbrotModel();
		mandyModel.setName("mandelbrot");
		mandyModel.setRealAxisName("xNex");
		mandyModel.setImaginaryAxisName("yNex");
		mandyModel.setExposureTime(0.01);
		req.putDetector("mandelbrot", mandyModel);
		
		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.01);
		req.putDetector("detector", dmodel);

		return req;
	}

	private ScanRequest<?> createGridRequest() throws IOException {
		
		final ScanRequest<?> req = new ScanRequest<IROI>();
		// Create a grid scan model
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel gmodel = new GridModel();
		gmodel.setSlowAxisPoints(5);
		gmodel.setFastAxisPoints(5);
		gmodel.setBoundingBox(box);
		gmodel.setFastAxisName("xNex");
		gmodel.setSlowAxisName("yNex");

		req.setCompoundModel(new CompoundModel(gmodel));
		req.setMonitorNames(Arrays.asList("monitor"));
		
		final File tmp = File.createTempFile("scan_servlet_test", ".nxs");
		tmp.deleteOnExit();
		req.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.
		
		final MandelbrotModel mandyModel = new MandelbrotModel();
		mandyModel.setName("mandelbrot");
		mandyModel.setRealAxisName("xNex");
		mandyModel.setImaginaryAxisName("yNex");
		req.putDetector("mandelbrot", mandyModel);
		
		return req;
	}

	private ScanRequest<?> createMalcolmRequest() throws Exception {
		
		final ScanRequest<?> req = new ScanRequest<IROI>();
		req.setCompoundModel(new CompoundModel(new StepModel("temperature", 0, 9, 1)));
		req.setMonitorNames(Arrays.asList("monitor"));
		
		final File tmp = File.createTempFile("scan_servlet_test_malc", ".nxs");
		tmp.deleteOnExit();
		req.setFilePath(tmp.getAbsolutePath()); // TODO This will really come from the scan file service which is not written.

		final MapMalcolmModel malcModel = new MapMalcolmModel();
		// Test params for starting the device
		fillParameters(malcModel.getParameterMap(), -1, 10);

		req.putDetector("zebra", malcModel);

		return req;
	}
	
	private void fillParameters(Map<String, Object> config, long configureSleep, int imageCount) throws Exception {
		
		// Params for driving mock mode
		config.put("nframes", imageCount); // IMAGE_COUNT images to write
		config.put("shape", new int[]{64,64});
		
		final File temp = File.createTempFile("testingFile", ".hdf5");
		temp.deleteOnExit();
		config.put("file", temp.getAbsolutePath());
		
		// The exposure is in seconds
		config.put("exposure", 0.5);
		
		double csleep = configureSleep/1000d;
		if (configureSleep>0) config.put("configureSleep", csleep); // Sleeps during configure

	}
}
