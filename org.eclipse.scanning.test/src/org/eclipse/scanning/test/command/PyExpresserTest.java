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
package org.eclipse.scanning.test.command;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.scanning.api.device.models.ClusterProcessingModel;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.api.points.models.RepeatedPointModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.command.ParserServiceImpl;
import org.eclipse.scanning.command.factory.PyExpressionFactory;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;


public class PyExpresserTest {
	
	private PyExpressionFactory factory;

	@Before
	public void services() {
		ParserServiceImpl.setPointGeneratorService(new PointGeneratorService());
		this.factory = new PyExpressionFactory();
	}

	@Test
	public void testScanRequestWithMonitor_Step() throws Exception {

		StepModel smodel = new StepModel();
		smodel.setStart(0);
		smodel.setStop(10);
		smodel.setStep(1);
		smodel.setName("fred");


		Collection<String> monitors = new ArrayList<>();
		monitors.add("someMonitor");

		ScanRequest<IROI> request = new ScanRequest<>();
		request.setCompoundModel(new CompoundModel(smodel));
		request.setMonitorNames(monitors);

		assertEquals(  // Concise.
				"mscan(step('fred', 0.0, 10.0, 1.0), 'someMonitor')",
				factory.pyExpress(request, false));
		assertEquals(  // Verbose.
				"mscan(path=[step(axis='fred', start=0.0, stop=10.0, step=1.0)], mon=['someMonitor'])",
				factory.pyExpress(request, true));
	}
	
	@Test
	public void testScanRequestWithMonitor_Repeat()
			throws Exception {

		RepeatedPointModel rmodel = new RepeatedPointModel();
		rmodel.setCount(10);
		rmodel.setValue(2.2);
		rmodel.setSleep(25);
		rmodel.setName("fred");

		Collection<String> monitors = new ArrayList<>();
		monitors.add("someMonitor");

		ScanRequest<IROI> request = new ScanRequest<>();
		request.setCompoundModel(new CompoundModel(rmodel));
		request.setMonitorNames(monitors);

		assertEquals(  // Concise.
				"mscan(repeat('fred', 10, 2.2, 25), 'someMonitor')",
				factory.pyExpress(request, false));
		assertEquals(  // Verbose.
				"mscan(path=[repeat(axis='fred', count=10, value=2.2, sleep=25)], mon=['someMonitor'])",
				factory.pyExpress(request, true));
	}


	@Test
	public void testScanRequestWithROI()
			throws Exception {

		BoundingBox bbox = new BoundingBox();
		bbox.setFastAxisStart(0);
		bbox.setSlowAxisStart(1);
		bbox.setFastAxisLength(10);
		bbox.setSlowAxisLength(11);

		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("myFast");
		gmodel.setSlowAxisName("mySlow");
		gmodel.setBoundingBox(bbox);
		gmodel.setFastAxisPoints(3);
		gmodel.setSlowAxisPoints(4);

		CircularROI croi = new CircularROI();
		ScanRequest<IROI> request = new ScanRequest<>();

		CompoundModel cmodel = new CompoundModel();
		cmodel.setData(gmodel, croi);
		request.setCompoundModel(cmodel);

		assertEquals(  // Concise.
				"mscan(grid(('myFast', 'mySlow'), (0.0, 1.0), (10.0, 12.0), count=(3, 4), snake=False, roi=circ((0.0, 0.0), 1.0)))",
				factory.pyExpress(request, false));
		assertEquals(  // Verbose.
				"mscan(path=[grid(axes=('myFast', 'mySlow'), start=(0.0, 1.0), stop=(10.0, 12.0), count=(3, 4), snake=False, roi=[circ(origin=(0.0, 0.0), radius=1.0)])])",
				factory.pyExpress(request, true));
	}

	@Test
	public void testCompoundScanRequest()
			throws Exception {

		StepModel smodel = new StepModel();
		smodel.setStart(0);
		smodel.setStop(10);
		smodel.setStep(1);
		smodel.setName("fred");

		ArrayModel amodel = new ArrayModel();
		amodel.setName("fred");
		amodel.setPositions(0.1);

		ScanRequest<IROI> request = new ScanRequest<>();
		request.setCompoundModel(new CompoundModel(smodel, amodel));

		assertEquals(  // Concise.
				"mscan([step('fred', 0.0, 10.0, 1.0), val('fred', 0.1)])",
				factory.pyExpress(request, false));
		assertEquals(  // Verbose.
				"mscan(path=[step(axis='fred', start=0.0, stop=10.0, step=1.0), array(axis='fred', values=[0.1])])",
				factory.pyExpress(request, true));
	}

	@Test
	public void testStepModel() throws Exception{

		StepModel smodel = new StepModel();
		smodel.setStart(0);
		smodel.setStop(10);
		smodel.setStep(1);
		smodel.setName("fred");

		assertEquals(  // Concise.
				"step('fred', 0.0, 10.0, 1.0)",
				factory.pyExpress(smodel, false));
		assertEquals(  // Verbose.
				"step(axis='fred', start=0.0, stop=10.0, step=1.0)",
				factory.pyExpress(smodel, true));
	}

	@Test
	public void testGridModel()
			throws Exception {

		BoundingBox bbox = new BoundingBox();
		bbox.setFastAxisStart(0);
		bbox.setSlowAxisStart(1);
		bbox.setFastAxisLength(10);
		bbox.setSlowAxisLength(11);

		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("myFast");
		gmodel.setSlowAxisName("mySlow");
		gmodel.setBoundingBox(bbox);
		gmodel.setFastAxisPoints(3);
		gmodel.setSlowAxisPoints(4);

		assertEquals(  // Concise.
				"grid(('myFast', 'mySlow'), (0.0, 1.0), (10.0, 12.0), count=(3, 4), snake=False)",
				factory.pyExpress(gmodel, new ArrayList<>(), false));
		assertEquals(  // Verbose.
				"grid(axes=('myFast', 'mySlow'), start=(0.0, 1.0), stop=(10.0, 12.0), count=(3, 4), snake=False)",
				factory.pyExpress(gmodel, new ArrayList<>(), true));
	}
	
	@Test
	public void testScanRequestWithProcessing() throws Exception {

		BoundingBox bbox = new BoundingBox();
		bbox.setFastAxisStart(0);
		bbox.setSlowAxisStart(1);
		bbox.setFastAxisLength(10);
		bbox.setSlowAxisLength(11);

		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("p");
		gmodel.setSlowAxisName("q");
		gmodel.setBoundingBox(bbox);
		gmodel.setFastAxisPoints(2);
		gmodel.setSlowAxisPoints(2);

		ScanRequest<IROI> request = new ScanRequest<>();
		request.setCompoundModel(new CompoundModel(Arrays.asList(gmodel)));
		Map<String,Object> detectors = new LinkedHashMap<>();
		detectors.put("mandelbrot", new MandelbrotModel("p", "q"));
		detectors.put("processing", new ClusterProcessingModel("processing", "mandelbrot", "/tmp/something.nxs"));
		request.setDetectors(detectors);

		String mscan = factory.pyExpress(request, false);
		String expected = "mscan(grid(('p', 'q'), (0.0, 1.0), (10.0, 12.0), count=(2, 2), snake=False), [detector('mandelbrot', 0.1), detector('processing', -1.0)])";
		assertEquals(expected, mscan);
		
		mscan = factory.pyExpress(request, true);
		expected = "mscan(path=[grid(axes=('p', 'q'), start=(0.0, 1.0), stop=(10.0, 12.0), count=(2, 2), snake=False)], det=[detector('mandelbrot', 0.1, maxIterations=500, escapeRadius=10.0, columns=301, rows=241, points=1000, maxRealCoordinate=1.5, maxImaginaryCoordinate=1.2, realAxisName='p', imaginaryAxisName='q', enableNoise=false, noiseFreeExposureTime=5.0, saveImage=true, saveSpectrum=true, saveValue=true), detector('processing', -1.0, detectorName='mandelbrot', processingFilePath='/tmp/something.nxs')])";
		assertEquals(expected, mscan);
	}

	@Test
	public void testClusterProcessingModel() throws Exception {

		ClusterProcessingModel cmodel = new ClusterProcessingModel("processing", "mandelbrot", "/tmp/something.nxs");

		String detector = factory.pyExpress(cmodel, true);
		String expected = "detector('processing', -1.0, detectorName='mandelbrot', processingFilePath='/tmp/something.nxs')";
		assertEquals(expected, detector);
	}

	@Test
	public void testMandelbrotModel() throws Exception {

		MandelbrotModel mmodel = new MandelbrotModel("p", "q");

		String detector = factory.pyExpress(mmodel, false);
		String expected = "detector('mandelbrot', 0.1)";
		assertEquals(expected, detector);
	}

	@Test
	public void testDummyMalcolmModel() throws Exception {

		DummyMalcolmModel mmodel = new DummyMalcolmModel();
		mmodel.setName("malcolm");
		mmodel.setExposureTime(0.1);
		mmodel.setAxesToMove(Arrays.asList("p", "q"));

		String detector = factory.pyExpress(mmodel, false);
		String expected = "detector('malcolm', 0.1)";
		assertEquals(expected, detector);
	}

	
	@Test
	public void testRasterModel() throws Exception {

		BoundingBox bbox = new BoundingBox();
		bbox.setFastAxisStart(0);
		bbox.setSlowAxisStart(1);
		bbox.setFastAxisLength(10);
		bbox.setSlowAxisLength(11);

		RasterModel rmodel = new RasterModel();
		rmodel.setFastAxisName("myFast");
		rmodel.setSlowAxisName("mySlow");
		rmodel.setBoundingBox(bbox);
		rmodel.setFastAxisStep(3);
		rmodel.setSlowAxisStep(4);
		rmodel.setSnake(true);

		assertEquals(  // Concise.
				"grid(('myFast', 'mySlow'), (0.0, 1.0), (10.0, 12.0), (3.0, 4.0))",
				factory.pyExpress(rmodel, null, false));
		assertEquals(  // Verbose.
				"grid(axes=('myFast', 'mySlow'), start=(0.0, 1.0), stop=(10.0, 12.0), step=(3.0, 4.0), snake=True)",
				factory.pyExpress(rmodel, null, true));
	}

	@Test
	public void testArrayModel() throws Exception {

		ArrayModel amodel = new ArrayModel();
		amodel.setName("fred");
		amodel.setPositions(0.1);

		assertEquals(  // Concise.
				"val('fred', 0.1)",
				factory.pyExpress(amodel, false));
		assertEquals(  // Verbose.
				"array(axis='fred', values=[0.1])",
				factory.pyExpress(amodel, true));

		amodel.setPositions(0.1, 0.2);
		assertEquals(  // Concise but with n>1 array values.
				"array('fred', [0.1, 0.2])",
				factory.pyExpress(amodel, false));
	}

}
