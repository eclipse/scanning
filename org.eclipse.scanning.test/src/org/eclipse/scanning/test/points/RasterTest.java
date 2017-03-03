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
package org.eclipse.scanning.test.points;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

public class RasterTest {

	
	private IPointGeneratorService service;
	
	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}

	@Test
	public void testFillingBoundingRectangle() throws Exception {
		// Create a simple bounding rectangle
		RectangularROI boundingRectangle = new RectangularROI(0, 0, 3, 3, 0);

		// Create a raster scan path
		RasterModel model = new RasterModel("x", "y");
		model.setFastAxisStep(1);
		model.setSlowAxisStep(1);

		// Get the point list
		IPointGenerator<RasterModel> gen = service.createGenerator(model, boundingRectangle);
		List<IPosition> pointList = gen.createPoints();

		// Check correct number of points
		assertEquals(16, pointList.size());

		// Check some random points are correct
		assertEquals(0.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		assertEquals(3.0, pointList.get(3).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(3).getValue("Y"), 1e-8);

		assertEquals(3.0, pointList.get(7).getValue("X"), 1e-8);
		assertEquals(1.0, pointList.get(7).getValue("Y"), 1e-8);

		assertEquals(3.0, pointList.get(11).getValue("X"), 1e-8);
		assertEquals(2.0, pointList.get(11).getValue("Y"), 1e-8);
		
        GeneratorUtil.testGeneratorPoints(gen);
	}

	@Test
	public void testSimpleBox() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(5);
		box.setSlowAxisLength(5);

		RasterModel model = new RasterModel("x", "y");
		model.setFastAxisStep(1);
		model.setSlowAxisStep(1);
		model.setBoundingBox(box);

		IPointGenerator<RasterModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (0, 0).
		assertEquals(0.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		// First point is (1, 0).
		assertEquals(1.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(1).getValue("Y"), 1e-8);
	}

	@Test
	public void testNegativeStep() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(5);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(-5);
		box.setSlowAxisLength(5);

		RasterModel model = new RasterModel("x", "y");

		model.setFastAxisStep(-1);
		// Okay to do this here because there is "negative width"
		// for the points to protrude into.

		model.setSlowAxisStep(1);
		model.setBoundingBox(box);

		IPointGenerator<RasterModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (5, 0).
		assertEquals(5.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		// First point is (4, 0).
		assertEquals(4.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(1).getValue("Y"), 1e-8);
	}

	@Test(expected=ModelValidationException.class)
	public void testBackwardsStep() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(5);
		box.setSlowAxisLength(5);

		RasterModel model = new RasterModel("x", "y");

		model.setFastAxisStep(-1);
		// Not okay to do this here because there is no "negative width"
		// for the points to protrude into.

		model.setSlowAxisStep(1);
		model.setBoundingBox(box);

		IPointGenerator<RasterModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
	}

	// Note this is a bit of a integration test not a strict unit test
	@Test
	public void testFillingAMoreComplicatedBoundingRectangle() throws Exception {
		double xStart = 0.0;
		double xStop = 25.5;
		double yStart = 0.0;
		double yStop = 33.33;

		double xStep = 0.4;
		double yStep = 0.6;

		RectangularROI roi = new RectangularROI();
		roi.setPoint(Math.min(xStart, xStop), Math.min(yStart, yStop));
		roi.setLengths(Math.abs(xStop - xStart), Math.abs(yStop - yStart));

	
		RasterModel model = new RasterModel("x", "y");
		model.setFastAxisStep(xStep);
		model.setSlowAxisStep(yStep);

		// Get the point list
		IPointGenerator<RasterModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();

		int rows = (int) (Math.floor((xStop - xStart) / xStep) + 1);
		int cols = (int) (Math.floor((yStop - yStart) / yStep) + 1);
		// Check the list size
		assertEquals("Point list size should be correct", rows * cols, pointList.size());

		// Check some points
		assertEquals(new Point(0, xStart, 0, yStart), pointList.get(0));
		assertEquals(xStart + 3 * xStep, pointList.get(3).getValue("X"), 1e-8);
		// TODO more
		
        GeneratorUtil.testGeneratorPoints(gen);
	}

	// Note this is a bit of a integration test not a strict unit test
	@Test
	public void testFillingACircle() throws Exception {
		double xCentre = 0;
		double yCentre = 0;
		double radius = 1;

		CircularROI roi = new CircularROI();
		roi.setPoint(xCentre, yCentre);
		roi.setRadius(radius);

		RasterModel model = new RasterModel("x", "y");
		model.setFastAxisStep(1);
		model.setSlowAxisStep(1);

		// Get the point list
		IPointGenerator<RasterModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();

		// Check the length of the lists are equal
		assertEquals(5, pointList.size());

		// Check the points are correct and the order is maintained
        assertEquals(new Point(1, 0.0, 0, -1.0), pointList.get(0));
        assertEquals(new Point(0, -1.0, 1, 0.0), pointList.get(1));
        assertEquals(new Point(1, 0.0, 1, 0.0), pointList.get(2));
        assertEquals(new Point(2, 1.0, 1, 0.0), pointList.get(3));
        assertEquals(new Point(1, 0.0, 2, 1.0), pointList.get(4));
		
        GeneratorUtil.testGeneratorPoints(gen, 3, 2);
	}

	
	@Test
	public void testNestedNeXus() throws Exception {
		
		int[] size = {8,5};
		
		// Create scan points for a grid and make a generator
		RasterModel rmodel = new RasterModel("x", "y");
		rmodel.setFastAxisName("xNex");
		rmodel.setFastAxisStep(3d/size[1]);
		rmodel.setSlowAxisName("yNex");
		rmodel.setSlowAxisStep(3d/size[0]);
		rmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		
		IPointGenerator<?> gen = service.createGenerator(rmodel);
    
		IPosition first = gen.iterator().next();
		assertEquals(0d, first.get("xNex"));
		assertEquals(0d, first.get("yNex"));
		
		IPosition last = null;
		Iterator<IPosition> it = gen.iterator();
		while(it.hasNext()) last = it.next();
		
		assertEquals(3d, last.get("xNex"));
		assertEquals(3d, last.get("yNex"));
		
	}

	@Test
	public void testFillingRectangleAwayFromOrigin() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(-10, 5, 2.5, 3.0, 0.0);

		// Create a raster scan path
		RasterModel model = new RasterModel("x", "y");
		model.setFastAxisStep(1);
		model.setSlowAxisStep(1);

		// Get the point list
		IPointGenerator<RasterModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();
        
		assertEquals(12, pointList.size());

		// Check some points
		assertEquals(new Point(0, -10.0, 0, 5.0), pointList.get(0));
		assertEquals(new Point(1, -9.0, 0, 5.0), pointList.get(1));
		assertEquals(new Point(0, -10.0, 1, 6.0), pointList.get(3));
		assertEquals(new Point(1, -9.0, 2, 7.0), pointList.get(7));
	}

	@Test
	public void testFillingRectangleWithSnake() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(1, 1, 2, 2, 0);

		// Create a raster scan path
		RasterModel model = new RasterModel("x", "y");
		model.setFastAxisStep(1);
		model.setSlowAxisStep(1);
		model.setSnake(true);

		// Get the point list
		IPointGenerator<RasterModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(9, pointList.size());

		// Check some points
		assertEquals(new Point(0, 1.0, 0, 1.0), pointList.get(0));
		assertEquals(new Point(1, 2.0, 0, 1.0), pointList.get(1));
		assertEquals(new Point(2, 3.0, 1, 2.0), pointList.get(3));
		assertEquals(new Point(1, 2.0, 2, 3.0), pointList.get(7));
	}

}
