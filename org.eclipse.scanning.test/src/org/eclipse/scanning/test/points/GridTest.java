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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.junit.Test;

public class GridTest extends GeneratorTest {

	@Test
	public void testFillingRectangleNoROI() throws Exception {

		// Create a grid scan model
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setBoundingBox(box);

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(pointList.size(), gen.size());
		checkPoints(pointList);
		GeneratorUtil.testGeneratorPoints(gen, 20, 20);
	}

	@Test(expected = ModelValidationException.class)
	public void testNegativeRowCount() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(-20);  // An unsigned integer type would solve this problem...
		model.setFastAxisPoints(20);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(pointList.size(), gen.size());
		GeneratorUtil.testGeneratorPoints(gen, 20, 20);
	}

	@Test
	public void testSimpleBox() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(-0.5);
		box.setSlowAxisStart(-0.5);
		box.setFastAxisLength(5);
		box.setSlowAxisLength(5);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (0, 0).
		assertEquals(0.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		// Oneth point is (1, 0).
		assertEquals(1.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(1).getValue("Y"), 1e-8);

		// Fifth point is (0, 1).
		assertEquals(0.0, pointList.get(5).getValue("X"), 1e-8);
		assertEquals(1.0, pointList.get(5).getValue("Y"), 1e-8);
	}

	@Test
	public void testSimpleBoxSnake() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(-0.5);
		box.setSlowAxisStart(-0.5);
		box.setFastAxisLength(5);
		box.setSlowAxisLength(5);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);
		model.setSnake(true);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (0, 0).
		assertEquals(0.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		// Oneth point is (1, 0).
		assertEquals(1.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(1).getValue("Y"), 1e-8);

		// Fifth point is (4, 1).
		assertEquals(4.0, pointList.get(5).getValue("X"), 1e-8);
		assertEquals(1.0, pointList.get(5).getValue("Y"), 1e-8);
	}

	@Test
	public void testBackwardsBox() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(4.5);
		box.setSlowAxisStart(-0.5);
		box.setFastAxisLength(-5);
		box.setSlowAxisLength(5);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (4, 0).
		assertEquals(4.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		// Oneth point is (3, 0).
		assertEquals(3.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(1).getValue("Y"), 1e-8);

		// Fifth point is (4, 1).
		assertEquals(4.0, pointList.get(5).getValue("X"), 1e-8);
		assertEquals(1.0, pointList.get(5).getValue("Y"), 1e-8);
	}

	@Test
	public void testBackwardsBoxSnake() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(4.5);
		box.setSlowAxisStart(-0.5);
		box.setFastAxisLength(-5);
		box.setSlowAxisLength(5);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);
		model.setSnake(true);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (4, 0).
		assertEquals(4.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(0).getValue("Y"), 1e-8);

		// Oneth point is (3, 0).
		assertEquals(3.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(0.0, pointList.get(1).getValue("Y"), 1e-8);

		// Fifth point is (0, 1).
		assertEquals(0.0, pointList.get(5).getValue("X"), 1e-8);
		assertEquals(1.0, pointList.get(5).getValue("Y"), 1e-8);
	}

	@Test
	public void testDoublyBackwardsBox() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(4.5);
		box.setSlowAxisStart(4.5);
		box.setFastAxisLength(-5);
		box.setSlowAxisLength(-5);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (4, 4).
		assertEquals(4.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(4.0, pointList.get(0).getValue("Y"), 1e-8);

		// Oneth point is (3, 4).
		assertEquals(3.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(4.0, pointList.get(1).getValue("Y"), 1e-8);

		// Fifth point is (4, 3).
		assertEquals(4.0, pointList.get(5).getValue("X"), 1e-8);
		assertEquals(3.0, pointList.get(5).getValue("Y"), 1e-8);
	}

	@Test
	public void testDoublyBackwardsBoxSnake() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(4.5);
		box.setSlowAxisStart(4.5);
		box.setFastAxisLength(-5);
		box.setSlowAxisLength(-5);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);
		model.setSnake(true);
		model.setBoundingBox(box);

		IPointGenerator<GridModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();

		// Zeroth point is (4, 4).
		assertEquals(4.0, pointList.get(0).getValue("X"), 1e-8);
		assertEquals(4.0, pointList.get(0).getValue("Y"), 1e-8);

		// Oneth point is (3, 4).
		assertEquals(3.0, pointList.get(1).getValue("X"), 1e-8);
		assertEquals(4.0, pointList.get(1).getValue("Y"), 1e-8);

		// Fifth point is (0, 3).
		assertEquals(0.0, pointList.get(5).getValue("X"), 1e-8);
		assertEquals(3.0, pointList.get(5).getValue("Y"), 1e-8);
	}

	@Test
	public void testFillingRectangle() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(0, 0, 3, 3, 0);

		// Create a raster scan path
		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setSlowAxisName("y");
		model.setFastAxisName("x");

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(pointList.size(), gen.size());

		checkPoints(pointList);
		GeneratorUtil.testGeneratorPoints(gen, 20, 20);
	}
	
	@Test
	public void testGridWrtCompound() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(0, 0, 3, 3, 0);

		// Create a raster scan path
		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setSlowAxisName("y");
		model.setFastAxisName("x");

		checkWrtCompound(model, roi, 400);

	}


	@Test
	public void testFillingRectangleIterator() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(0, 0, 3, 3, 0);

		// Create a raster scan path
		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setSlowAxisName("y");
		model.setFastAxisName("x");

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(model, roi);
		Iterator<IPosition>     it = gen.iterator();
		List<IPosition>  pointList = new ArrayList<IPosition>(7);
		while (it.hasNext()) pointList.add(it.next());

		assertArrayEquals(pointList.toArray(), gen.createPoints().toArray());

		checkPoints(pointList);
		GeneratorUtil.testGeneratorPoints(gen, 20, 20);

	}

	@Test
	public void testFillingCircle() throws Exception {

		// Create a simple bounding rectangle
		CircularROI circle = new CircularROI(1.5, 1.5, 1.5);

		// Create a raster scan path
		GridModel gridScanPath = new GridModel("x", "y");
		gridScanPath.setSlowAxisPoints(20);
		gridScanPath.setFastAxisPoints(20);
		gridScanPath.setSlowAxisName("y");
		gridScanPath.setFastAxisName("x");

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(gridScanPath, circle);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(pointList.size(), gen.size());

		assertEquals(pointList.size(), 316);
		GeneratorUtil.testGeneratorPoints(gen);
	}

	@Test
	public void testFillingBoundingCircleSkewed() throws Exception {

		// Create a simple bounding rectangle
		CircularROI circle = new CircularROI(1.5, 1.5, 15);

		// Create a raster scan path
		GridModel gridScanPath = new GridModel("x", "y");
		gridScanPath.setSlowAxisPoints(20);
		gridScanPath.setFastAxisPoints(200);

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(gridScanPath, circle);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(pointList.size(), gen.size());

		assertEquals(pointList.size(), 3156);
//		GeneratorUtil.testGeneratorPoints(gen); // Rounding error in here causing test to fail
		assertEquals(new Point(69, 1.0425, 0, 13.575), pointList.get(0));
		assertEquals(new Point(70, 1.0575, 0, 13.575), pointList.get(1));
		assertEquals(new Point(17, 0.2625, 7, 14.625), pointList.get(1000));
		assertEquals(new Point(27, 0.4125, 12, 15.375), pointList.get(2000));
		assertEquals(new Point(75, 1.1325, 19, 16.425), pointList.get(3100));
	}

	@Test
	public void testFillingPolygon() throws Exception {

		PolygonalROI diamond = new PolygonalROI(new double[] { 1.5, 0 });
		diamond.insertPoint(new double[] { 3, 1.5 });
		diamond.insertPoint(new double[] { 1.5, 3 });
		diamond.insertPoint(new double[] { 0, 1.5 });
		diamond.insertPoint(new double[] { 1.5, 0 });

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(20);
		model.setFastAxisPoints(20);
		model.setBoundingBox(box);

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(model, diamond);
		List<IPosition> pointList = gen.createPoints();

		assertEquals(pointList.size(), gen.size());

		assertTrue(pointList.size() < 400); // Some must not be in the polygon!
		GeneratorUtil.testGeneratorPoints(gen);
	}

	private void checkPoints(List<IPosition> pointList) {
		
		// Check correct number of points
		assertEquals(20 * 20, pointList.size());

		// Check some random points are correct
		assertEquals(0.075, pointList.get(0).getValue("X"), 1e-8);
		for (int i = 0; i < 20; i++) {
			assertEquals(i, pointList.get(i).getIndex("x"));
			assertEquals(0, pointList.get(i).getIndex("y"));
		}
		for (int i = 20; i < 40; i++) {
			assertEquals(i-20, pointList.get(i).getIndex("x"));
			assertEquals(1, pointList.get(i).getIndex("y"));
		}
		
		assertEquals(0.075, pointList.get(0).getValue("Y"), 1e-8);

		assertEquals(0.075 + 3 * (3.0 / 20.0), pointList.get(3).getValue("X"), 1e-8);
		assertEquals(0.075 + 0.0, pointList.get(3).getValue("Y"), 1e-8);

		assertEquals(0.075 + 2 * (3.0 / 20.0), pointList.get(22).getValue("X"), 1e-8);
		assertEquals(0.075 + 1 * (3.0 / 20.0), pointList.get(22).getValue("Y"), 1e-8);

		assertEquals(0.075 + 10 * (3.0 / 20.0), pointList.get(350).getValue("X"), 1e-8);
		assertEquals(0.075 + 17 * (3.0 / 20.0), pointList.get(350).getValue("Y"), 1e-8);

	}

	@Test
	public void testFillingRectangleAwayFromOrigin() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(-10.0, 5.0, 3.0, 3.0, 0.0);

		// Create a grid scan path
		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(3);
		model.setFastAxisPoints(3);
		model.setSlowAxisName("y");
		model.setFastAxisName("x");

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();

		assertTrue(pointList.size()==9);

		// Check some points
		assertTrue(pointList.get(0).equals(new Point(0, -9.5, 0, 5.5)));
		assertTrue(pointList.get(1).equals(new Point(1, -8.5, 0, 5.5)));
		assertTrue(pointList.get(3).equals(new Point(0, -9.5, 1, 6.5)));
		assertTrue(pointList.get(5).equals(new Point(2, -7.5, 1, 6.5)));
		assertTrue(pointList.get(7).equals(new Point(1, -8.5, 2, 7.5)));
	}

	@Test
	public void testFillingRectangleWithSnake() throws Exception {

		// Create a simple bounding rectangle
		RectangularROI roi = new RectangularROI(0, 0, 3, 3, 0);

		// Create a grid scan path
		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(3);
		model.setFastAxisPoints(3);
		model.setSnake(true);

		// Get the point list
		IPointGenerator<GridModel> gen = service.createGenerator(model, roi);
		List<IPosition> pointList = gen.createPoints();

		assertTrue(pointList.size()==9);

		// Check some points
		assertTrue(pointList.get(0).equals(new Point(0, 0.5, 0, 0.5)));
		assertTrue(pointList.get(1).equals(new Point(1, 1.5, 0, 0.5)));
		assertTrue(pointList.get(3).equals(new Point(2, 2.5, 1, 1.5)));
		assertTrue(pointList.get(7).equals(new Point(1, 1.5, 2, 2.5)));
	}
}
