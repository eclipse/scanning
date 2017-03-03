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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.OneDStepModel;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.api.points.models.RepeatedPointModel;
import org.eclipse.scanning.api.points.models.SinglePointModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.command.Services;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDevice;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;


public class ScanRequestCreationTest extends AbstractJythonTest {
	
	private IPointGeneratorService service;
	
	public ScanRequestCreationTest() {
		super(false);
	}
	
	@Before
	public void before() throws Exception {
		
		createMarshaller();
		service = new PointGeneratorService();
		Services.setEventService(new EventServiceImpl(new ActivemqConnectorService()));
		
		RunnableDeviceServiceImpl impl = new RunnableDeviceServiceImpl(new MockScannableConnector(null));
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		impl._register(MandelbrotModel.class, MandelbrotDetector.class);
		impl._register(DummyMalcolmModel.class, DummyMalcolmDevice.class);
		
		final MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setName("detector");
		dmodel.setExposureTime(0.1);
		impl.createRunnableDevice(dmodel);

		MandelbrotModel model = new MandelbrotModel();
		model.setName("mandelbrot");
		model.setExposureTime(0.00001);
		impl.createRunnableDevice(model);
		Services.setRunnableDeviceService(impl);
	}

	@Test
	public void testGridCommandWithROI() throws Exception {
		pi.exec("sr =                             "
			+	"scan_request(                    "
			+	"    grid(                        "
			+	"        axes=(my_scannable, 'y'),"  // Can use Scannable objects or strings.
			+	"        start=(0, 2),            "
			+	"        stop=(10, 11),           "
			+	"        count=(5, 6),            "
			+	"        roi=circ((4, 6), 5)      "
			+	"    ),                           "
			+	"    det=detector('mandelbrot', 0.1),         "
			+	")                                ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Collection<Object> models = request.getCompoundModel().getModels();
		assertEquals(1, models.size());  // I.e. this is not a compound scan.

		Object model = models.iterator().next();
		assertEquals(GridModel.class, model.getClass());

		GridModel gmodel = (GridModel) model;
		assertEquals("fred", gmodel.getFastAxisName());
		assertEquals("y", gmodel.getSlowAxisName());
		assertEquals(5, gmodel.getFastAxisPoints());
		assertEquals(6, gmodel.getSlowAxisPoints());
		assertEquals(true, gmodel.isSnake());

		BoundingBox bbox = gmodel.getBoundingBox();
		assertEquals(0, bbox.getFastAxisStart(), 1e-8);
		assertEquals(2, bbox.getSlowAxisStart(), 1e-8);
		assertEquals(10, bbox.getFastAxisLength(), 1e-8);
		assertEquals(9, bbox.getSlowAxisLength(), 1e-8);

		Collection<IROI> regions = service.findRegions(gmodel, request.getCompoundModel().getRegions());
		assertEquals(1, regions.size());

		IROI region = regions.iterator().next();
		assertEquals(CircularROI.class, region.getClass());

		CircularROI cregion = (CircularROI) region;
		assertEquals(4, cregion.getCentre()[0], 1e-8);
		assertEquals(6, cregion.getCentre()[1], 1e-8);
		assertEquals(5, cregion.getRadius(), 1e-8);

		Map<String, Object> detectors = request.getDetectors();
		assertTrue(detectors.keySet().contains("mandelbrot"));

		Object dmodel = detectors.get("mandelbrot");
		assertEquals(MandelbrotModel.class, dmodel.getClass());

		MandelbrotModel mmodel = (MandelbrotModel) dmodel;
		assertEquals(0.1, mmodel.getExposureTime(), 1e-8);
	}

	@Test
	public void testGridCommandWithROINoDetector() throws Exception {
		pi.exec("sr =                             "
			+	"scan_request(                    "
			+	"    grid(                        "
			+	"        axes=(my_scannable, 'y'),"  // Can use Scannable objects or strings.
			+	"        start=(0, 2),            "
			+	"        stop=(10, 11),           "
			+	"        count=(5, 6),            "
			+	"        roi=circ((4, 6), 5)      "
			+	"    ),                           "
			+	")                                ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Collection<Object> models = request.getCompoundModel().getModels();
		assertEquals(1, models.size());  // I.e. this is not a compound scan.

		Object model = models.iterator().next();
		assertEquals(GridModel.class, model.getClass());

		GridModel gmodel = (GridModel) model;
		assertEquals("fred", gmodel.getFastAxisName());
		assertEquals("y", gmodel.getSlowAxisName());
		assertEquals(5, gmodel.getFastAxisPoints());
		assertEquals(6, gmodel.getSlowAxisPoints());
		assertEquals(true, gmodel.isSnake());

		BoundingBox bbox = gmodel.getBoundingBox();
		assertEquals(0, bbox.getFastAxisStart(), 1e-8);
		assertEquals(2, bbox.getSlowAxisStart(), 1e-8);
		assertEquals(10, bbox.getFastAxisLength(), 1e-8);
		assertEquals(9, bbox.getSlowAxisLength(), 1e-8);

		Collection<IROI> regions = service.findRegions(gmodel, request.getCompoundModel().getRegions());
		assertEquals(1, regions.size());

		IROI region = regions.iterator().next();
		assertEquals(CircularROI.class, region.getClass());

		CircularROI cregion = (CircularROI) region;
		assertEquals(4, cregion.getCentre()[0], 1e-8);
		assertEquals(6, cregion.getCentre()[1], 1e-8);
		assertEquals(5, cregion.getRadius(), 1e-8);
	}

	@Test
	public void testStepCommandWithMonitors() throws Exception {
		pi.exec("sr =                               "
			+	"scan_request(                      "
			+	"    step(my_scannable, -2, 5, 0.5),"
			+	"    mon=['x', another_scannable],  "  // Monitor two scannables.
			+	"    det=detector('mandelbrot', 0.1),           "
			+	")                                  ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = ((List<Object>) request.getCompoundModel().getModels()).get(0);
		assertEquals(StepModel.class, model.getClass());

		StepModel smodel = (StepModel) model;
		assertEquals("fred", smodel.getName());
		assertEquals(-2, smodel.getStart(), 1e-8);
		assertEquals(5, smodel.getStop(), 1e-8);
		assertEquals(0.5, smodel.getStep(), 1e-8);

		Collection<String> monitors = request.getMonitorNames();
		assertEquals(2, monitors.size());
		Iterator<String> monitorIterator = monitors.iterator();
		assertEquals("x", monitorIterator.next());
		assertEquals("bill", monitorIterator.next());
	}
	
	@Test
	public void testStepCommandWithMonitorsNoDetector() throws Exception {
		pi.exec("sr =                               "
			+	"scan_request(                      "
			+	"    step(my_scannable, -2, 5, 0.5),"
			+	"    mon=['x', another_scannable],  "  // Monitor two scannables.
			+	")                                  ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = ((List<Object>) request.getCompoundModel().getModels()).get(0);
		assertEquals(StepModel.class, model.getClass());

		StepModel smodel = (StepModel) model;
		assertEquals("fred", smodel.getName());
		assertEquals(-2, smodel.getStart(), 1e-8);
		assertEquals(5, smodel.getStop(), 1e-8);
		assertEquals(0.5, smodel.getStep(), 1e-8);

		Collection<String> monitors = request.getMonitorNames();
		assertEquals(2, monitors.size());
		Iterator<String> monitorIterator = monitors.iterator();
		assertEquals("x", monitorIterator.next());
		assertEquals("bill", monitorIterator.next());
	}

	@Test
	public void testRepeatCommandWithMonitors() throws Exception {
		pi.exec("sr =                               "
			+	"scan_request(                      "
			+	"    repeat(my_scannable, 10, 2.2, 25),"
			+	"    mon=['x', another_scannable],  "  // Monitor two scannables.
			+	"    det=detector('mandelbrot', 0.1),           "
			+	")                                  ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = ((List<Object>) request.getCompoundModel().getModels()).get(0);
		assertEquals(RepeatedPointModel.class, model.getClass());

		RepeatedPointModel smodel = (RepeatedPointModel) model;
		assertEquals("fred", smodel.getName());
		assertEquals(10, smodel.getCount(), 1e-8);
		assertEquals(2.2, smodel.getValue(), 1e-8);
		assertEquals(25, smodel.getSleep(), 1e-8);

		Collection<String> monitors = request.getMonitorNames();
		assertEquals(2, monitors.size());
		Iterator<String> monitorIterator = monitors.iterator();
		assertEquals("x", monitorIterator.next());
		assertEquals("bill", monitorIterator.next());
	}

	@Test
	public void testRepeatCommandWithMonitorsNoDetector() throws Exception {
		pi.exec("sr =                               "
			+	"scan_request(                      "
			+	"    repeat(my_scannable, 10, 2.2, 25),"
			+	"    mon=['x', another_scannable]  "  // Monitor two scannables.
			+	")                                  ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = ((List<Object>) request.getCompoundModel().getModels()).get(0);
		assertEquals(RepeatedPointModel.class, model.getClass());

		RepeatedPointModel smodel = (RepeatedPointModel) model;
		assertEquals("fred", smodel.getName());
		assertEquals(10, smodel.getCount(), 1e-8);
		assertEquals(2.2, smodel.getValue(), 1e-8);
		assertEquals(25, smodel.getSleep(), 1e-8);

		Collection<String> monitors = request.getMonitorNames();
		assertEquals(2, monitors.size());
		Iterator<String> monitorIterator = monitors.iterator();
		assertEquals("x", monitorIterator.next());
		assertEquals("bill", monitorIterator.next());
	}

	@Test
	public void testRasterCommandWithROIs() throws Exception {
		pi.exec("sr =                                 "
			+	"scan_request(                        "
			+	"    grid(                            "
			+	"        axes=('x', 'y'),             "
			+	"        start=(1, 2),                "
			+	"        stop=(8, 10),                "
			+	"        step=(0.5, 0.6),             "
			+	"        snake=True,                  "
			+	"        roi=[                        "
			+	"            circ((4, 4), 5),         "
			+	"            rect((3, 4), (3, 3), 0.1)"
			+	"        ]                            "
			+	"    ),                               "
			+	"    det=detector('mandelbrot', 0.1),             "
			+	")                                    ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = request.getCompoundModel().getModels().iterator().next();
		assertEquals(RasterModel.class, model.getClass());

		RasterModel rmodel = (RasterModel) model;
		assertEquals(0.5, rmodel.getFastAxisStep(), 1e-8);

		Collection<IROI> regions = service.findRegions(rmodel, request.getCompoundModel().getRegions());
		assertEquals(2, regions.size());

		Iterator<IROI> regionIterator = regions.iterator();
		CircularROI cregion = (CircularROI) regionIterator.next();
		assertEquals(4, cregion.getCentre()[0], 1e-8);
		assertEquals(4, cregion.getCentre()[1], 1e-8);
		assertEquals(5, cregion.getRadius(), 1e-8);

		RectangularROI rregion = (RectangularROI) regionIterator.next();
		assertEquals(3, rregion.getPoint()[0], 1e-8);
		assertEquals(4, rregion.getPoint()[1], 1e-8);
		assertEquals(3, rregion.getLengths()[0], 1e-8);
		assertEquals(3, rregion.getLengths()[0], 1e-8);
		assertEquals(0.1, rregion.getAngle(), 0.1);  // Radians.
	}

	@Test
	public void testRasterCommandWithROIsNoDetector() throws Exception {
		pi.exec("sr =                                 "
			+	"scan_request(                        "
			+	"    grid(                            "
			+	"        axes=('x', 'y'),             "
			+	"        start=(1, 2),                "
			+	"        stop=(8, 10),                "
			+	"        step=(0.5, 0.6),             "
			+	"        snake=True,                  "
			+	"        roi=[                        "
			+	"            circ((4, 4), 5),         "
			+	"            rect((3, 4), (3, 3), 0.1)"
			+	"        ]                            "
			+	"    )                                "
			+	")                                    ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = request.getCompoundModel().getModels().iterator().next();
		assertEquals(RasterModel.class, model.getClass());

		RasterModel rmodel = (RasterModel) model;
		assertEquals(0.5, rmodel.getFastAxisStep(), 1e-8);

		Collection<IROI> regions = service.findRegions(rmodel, request.getCompoundModel().getRegions());
		assertEquals(2, regions.size());

		Iterator<IROI> regionIterator = regions.iterator();
		CircularROI cregion = (CircularROI) regionIterator.next();
		assertEquals(4, cregion.getCentre()[0], 1e-8);
		assertEquals(4, cregion.getCentre()[1], 1e-8);
		assertEquals(5, cregion.getRadius(), 1e-8);

		RectangularROI rregion = (RectangularROI) regionIterator.next();
		assertEquals(3, rregion.getPoint()[0], 1e-8);
		assertEquals(4, rregion.getPoint()[1], 1e-8);
		assertEquals(3, rregion.getLengths()[0], 1e-8);
		assertEquals(3, rregion.getLengths()[0], 1e-8);
		assertEquals(0.1, rregion.getAngle(), 0.1);  // Radians.
	}

	@Test
	public void testArrayCommand() throws Exception {
		pi.exec("sr =                                 "
			+	"scan_request(                        "
			+	"    array('qty', [-3, 1, 1.5, 1e10]),"
			+	"    det=detector('mandelbrot', 0.1),             "
			+	")                                    ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = request.getCompoundModel().getModels().iterator().next();
		assertEquals(ArrayModel.class, model.getClass());

		ArrayModel amodel = (ArrayModel) model;
		assertEquals("qty", amodel.getName());
		assertEquals(-3, amodel.getPositions()[0], 1e-8);
		assertEquals(1, amodel.getPositions()[1], 1e-8);
		assertEquals(1.5, amodel.getPositions()[2], 1e-8);
		assertEquals(1e10, amodel.getPositions()[3], 1);
	}

	@Test
	public void testOneDEqualSpacingCommand() throws Exception {
		pi.exec("sr =                                                    "
			+	"scan_request(                                           "
			+	"    line(origin=(0, 4), length=10, angle=0.1, count=10),"
			+	"    det=[detector('mandelbrot', 0.1)],                              "
			+	")                                                       ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = request.getCompoundModel().getModels().iterator().next();
		assertEquals(OneDEqualSpacingModel.class, model.getClass());

		OneDEqualSpacingModel omodel = (OneDEqualSpacingModel) model;
		assertEquals(0, omodel.getBoundingLine().getxStart(), 1e-8);
		assertEquals(4, omodel.getBoundingLine().getyStart(), 1e-8);
		assertEquals(0.1, omodel.getBoundingLine().getAngle(), 1e-8);
		assertEquals(10, omodel.getBoundingLine().getLength(), 1e-8);
		assertEquals(10, omodel.getPoints());
	}

	@Test
	public void testOneDStepCommand() throws Exception {
		pi.exec("sr =                                                       "
			+	"scan_request(                                              "
			+	"    line(origin=(-2, 1.3), length=10, angle=0.1, step=0.5),"
			+	"    det=detector('mandelbrot', 0.1),                                   "
			+	")                                                          ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = request.getCompoundModel().getModels().iterator().next();
		assertEquals(OneDStepModel.class, model.getClass());

		OneDStepModel omodel = (OneDStepModel) model;
		assertEquals(-2, omodel.getBoundingLine().getxStart(), 1e-8);
		assertEquals(1.3, omodel.getBoundingLine().getyStart(), 1e-8);
		assertEquals(0.1, omodel.getBoundingLine().getAngle(), 1e-8);
		assertEquals(10, omodel.getBoundingLine().getLength(), 1e-8);
		assertEquals(0.5, omodel.getStep(), 1e-8);
	}

	@Test
	public void testSinglePointCommand() throws Exception {
		pi.exec("sr = scan_request(point(4, 5), det=mandelbrot(0.1))");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Object model = request.getCompoundModel().getModels().iterator().next();
		assertEquals(SinglePointModel.class, model.getClass());

		SinglePointModel spmodel = (SinglePointModel) model;
		assertEquals(4, spmodel.getX(), 1e-8);
		assertEquals(5, spmodel.getY(), 1e-8);

		Map<String, Object> detectors = request.getDetectors();
		assertTrue(detectors.keySet().contains("mandelbrot"));

		Object dmodel = detectors.get("mandelbrot");
		assertEquals(MandelbrotModel.class, dmodel.getClass());

		MandelbrotModel mmodel = (MandelbrotModel) dmodel;
		assertEquals(0.1, mmodel.getExposureTime(), 1e-8);
	}

	@Test
	public void testSquareBracketCombinations() throws Exception {
		pi.exec("sr0 = scan_request(point(4, 5), det=detector('mandelbrot', 0.1))");
		pi.exec("sr1 = scan_request([point(4, 5)], det=detector('mandelbrot', 0.1))");
		pi.exec("sr2 = scan_request(point(4, 5), det=[detector('mandelbrot', 0.1)])");
		pi.exec("sr3 = scan_request([point(4, 5)], det=[detector('mandelbrot', 0.1)])");

		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request1 = pi.get("sr0", ScanRequest.class);
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request2 = pi.get("sr1", ScanRequest.class);
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request3 = pi.get("sr2", ScanRequest.class);
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request4 = pi.get("sr3", ScanRequest.class);

		assertEquals(4, ((SinglePointModel) request1.getCompoundModel().getModels().iterator().next()).getX(), 1e-8);
		assertEquals(0.1, ((MandelbrotModel) request1.getDetectors().get("mandelbrot")).getExposureTime(), 1e-8);

		assertEquals(4, ((SinglePointModel) request2.getCompoundModel().getModels().iterator().next()).getX(), 1e-8);
		assertEquals(0.1, ((MandelbrotModel) request2.getDetectors().get("mandelbrot")).getExposureTime(), 1e-8);

		assertEquals(4, ((SinglePointModel) request3.getCompoundModel().getModels().iterator().next()).getX(), 1e-8);
		assertEquals(0.1, ((MandelbrotModel) request3.getDetectors().get("mandelbrot")).getExposureTime(), 1e-8);

		assertEquals(4, ((SinglePointModel) request4.getCompoundModel().getModels().iterator().next()).getX(), 1e-8);
		assertEquals(0.1, ((MandelbrotModel) request4.getDetectors().get("mandelbrot")).getExposureTime(), 1e-8);
	}

	@Test
	public void testCompoundCommand() throws Exception {
		pi.exec("sr =                                                                                 "
			+	"scan_request(                                                                        "
			+	"    path=[                                                                           "
			+	"        grid(axes=('x', 'y'), start=(0, 0), stop=(10, 10), count=(5, 5), snake=True),"
			+	"        step('qty', 0, 10, 1),                                                       "
			+	"    ],                                                                               "
			+	"    det=detector('mandelbrot', 0.1),                                                             "
			+	")                                                                                    ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Collection<Object> models = request.getCompoundModel().getModels();
		assertEquals(2, models.size());  // I.e. this is a compound scan with two components.

		Iterator<Object> modelIterator = models.iterator();
		GridModel gmodel = (GridModel) modelIterator.next();
		assertEquals(5, gmodel.getSlowAxisPoints());

		StepModel smodel = (StepModel) modelIterator.next();
		assertEquals(10, smodel.getStop(), 1e-8);
	}

	@Test
	public void testMoveToKeepStillCommand() throws Exception {
		pi.exec("sr =                                              "
			+	"scan_request(                                     "
			+	"    [step(my_scannable, -2, 5, 0.5), val('y', 5)],"
			+	"    det=detector('mandelbrot', 0.1),                          "
			+	")                                                 ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Collection<Object> models = request.getCompoundModel().getModels();
		assertEquals(2, models.size());

		Iterator<Object> modelIterator = models.iterator();
		modelIterator.next();  // Throw away the step model.

		ArrayModel amodel = (ArrayModel) modelIterator.next();
		assertEquals(1, amodel.getPositions().length);
		assertEquals(5, amodel.getPositions()[0], 1e-8);
	}
	
	@Test
	public void testMoveToKeepStillCommandNoDetector() throws Exception {
		pi.exec("sr =                                              "
			+	"scan_request(                                     "
			+	"    [step(my_scannable, -2, 5, 0.5), val('y', 5)]"
			+	")                                                 ");
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> request = pi.get("sr", ScanRequest.class);

		Collection<Object> models = request.getCompoundModel().getModels();
		assertEquals(2, models.size());

		Iterator<Object> modelIterator = models.iterator();
		modelIterator.next();  // Throw away the step model.

		ArrayModel amodel = (ArrayModel) modelIterator.next();
		assertEquals(1, amodel.getPositions().length);
		assertEquals(5, amodel.getPositions()[0], 1e-8);
	}


	@Ignore("ScanRequest<?>.equals() doesn't allow this test to work.")
	@Test
	public void testArgStyleInvariance() throws Exception {
		pi.exec("sr_full =                     "
			+	"scan_request(                 "
			+	"    path=grid(                "
			+	"        axes=('x', 'y'),      "
			+	"        start=(1, 2),         "
			+	"        stop=(8, 10),         "
			+	"        step=(0.5, 0.6),      "
			+	"        roi=[                 "
			+	"            circ(             "
			+	"                origin=(4, 4),"
			+	"                radius=5      "
			+	"            ),                "
			+	"            rect(             "
			+	"                origin=(3, 4),"
			+	"                size=(3, 3),  "
			+	"                angle=0.1     "
			+	"            ),                "
			+	"        ]                     "
			+	"    ),                        "
			+	"    det=mandelbrot(0.1),      "
			+	")                             ");
		pi.exec("sr_minimal =                                    "
			+	"scan_request(                                   "
			+	"    grid(                                       "
			+	"        ('x', 'y'), (1, 2), (8, 10), (0.5, 0.6),"
			+	"        roi=[                                   "
			+	"            circ((4, 4), 5),                    "
			+	"            rect((3, 4), (3, 3), 0.1),          "
			+	"        ]                                       "
			+	"    ),                                          "
			+	"    mandelbrot(0.1),                            "
			+	")                                               ");

		@SuppressWarnings("unchecked")
		ScanRequest<IROI> requestFullKeywords = pi.get("sr_full", ScanRequest.class);
		@SuppressWarnings("unchecked")
		ScanRequest<IROI> requestMinimalKeywords = pi.get("sr_minimal", ScanRequest.class);

		assertTrue(requestMinimalKeywords.equals(requestFullKeywords));
	}
}
