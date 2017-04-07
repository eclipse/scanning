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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanEstimator;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ScanShapeTest {
	
	private static IPointGeneratorService service;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		service = new PointGeneratorService();
	}
	
	@Test
	public void testShapeGrid2D() throws Exception {
		// note first arg is number of extra dimensions on top of inner grid
		gridTest(0, false);
	}
	
	@Test
	public void testShapeGrid3D() throws Exception {
		gridTest(1, false);
	}
	
	@Test
	public void testShapeGrid4D() throws Exception {
		gridTest(2, false);
	}
	
	@Test
	public void testShapeGrid5D() throws Exception {
		gridTest(3, false);
	}
	
	@Test
	@Ignore
	public void testShapeGrid8D() throws Exception {
		gridTest(6, false);
	}
	
	@Test
	public void testShapeSnakeGrid2D() throws Exception {
		gridTest(0, true);
	}
	
	@Test
	public void testShapeSnakeGrid3D() throws Exception {
		gridTest(1, true);
	}
	
	@Test
	public void testShapeSnakeGrid4D() throws Exception {
		gridTest(2, true);
	}
	
	@Test
	public void testShapeSnakeGrid5D() throws Exception {
		gridTest(3, true);
	}
	
	@Test
	@Ignore
	public void testShapeSnakeGrid8D() throws Exception {
		gridTest(6, true);
	}
	
	@Test
	public void testShapeGridCircularRegion2D() throws Exception {
		gridWithCircularRegionTest(0, false);
	}
	
	@Test
	public void testShapeGridCircularRegion3D() throws Exception {
		gridWithCircularRegionTest(1, false);
	}
	
	@Test
	public void testShapeGridCircularRegion4D() throws Exception {
		gridWithCircularRegionTest(2, false);
	}
	
	@Test
	public void testShapeGridCircularRegion5D() throws Exception {
		gridWithCircularRegionTest(3, false);
	}
	
	@Test
	@Ignore
	public void testShapeGridCircularRegion8D() throws Exception {
		gridWithCircularRegionTest(6, false);
	}
	
	@Test
	public void testShapeSnakeGridCircularRegion2D() throws Exception {
		gridWithCircularRegionTest(0, true);
	}
	
	@Test
	public void testShapeSnakeGridCircularRegion3D() throws Exception {
		gridWithCircularRegionTest(1, true);
	}
	
	@Test
	public void testShapeSnakeGridCircularRegion4D() throws Exception {
		gridWithCircularRegionTest(2, true);
	}
	
	@Test
	public void testShapeSnakeGridCircularRegion5D() throws Exception {
		gridWithCircularRegionTest(3, true);
	}
	
	@Test
	@Ignore
	public void testShapeSnakeGridCircularRegion8D() throws Exception {
		gridWithCircularRegionTest(6, true);
	}
	
	@Test
	public void testShapeGridPolygonRegion2D() throws Exception {
		gridWithPolygonRegionTest(0, false);
	}
	
	@Test
	public void testShapeGridPolygonRegion3D() throws Exception {
		gridWithPolygonRegionTest(1, false);
	}
	
	@Test
	public void testShapeGridPolygonRegion4D() throws Exception {
		gridWithPolygonRegionTest(2, false);
	}
	
	@Test
	public void testShapeGridPolygonRegion5D() throws Exception {
		gridWithPolygonRegionTest(3, false);
	}
	
	@Test
	@Ignore
	public void testShapeGridPolygonRegion8D() throws Exception {
		gridWithPolygonRegionTest(6, false);
	}
	
	@Test
	public void testShapeSnakeGridPolygonRegion2D() throws Exception {
		gridWithPolygonRegionTest(0, true);
	}
	
	@Test
	public void testShapeSnakeGridPolygonRegion3D() throws Exception {
		gridWithPolygonRegionTest(1, true);
	}
	
	@Test
	public void testShapeSnakeGridPolygonRegion4D() throws Exception {
		gridWithPolygonRegionTest(2, true);
	}
	
	@Test
	public void testShapeSnakeGridPolygonRegion5D() throws Exception {
		gridWithPolygonRegionTest(3, true);
	}
	
	@Test
	@Ignore
	public void testShapeSnakeGridPolygonRegion8D() throws Exception {
		gridWithPolygonRegionTest(6, true);
	}
	
	@Test
	public void testShapeSpiral2D() throws Exception {
		spiralTest(0);
	}
	
	@Test
	public void testShapeSpiral3D() throws Exception {
		spiralTest(1);
	}
	
	@Test
	public void testShapeSpiral4D() throws Exception {
		spiralTest(2);
	}
	
	@Test
	public void testShapeSpiral5D() throws Exception {
		spiralTest(3);
	}
	
	@Test
	@Ignore
	public void testShapeSpiral8D() throws Exception {
		spiralTest(6);
	}
	
	@Test
	public void testShapeLine1D() throws Exception {
		lineTest(0);
	}
	
	@Test
	public void testShapeLine2D() throws Exception {
		lineTest(1);
	}
	
	@Test
	public void testShapeLine3D() throws Exception {
		lineTest(2);
	}
	
	@Test
	public void testShapeLine4D() throws Exception {
		lineTest(3);
	}
	
	@Test
	@Ignore
	public void testShapeLine7D() throws Exception {
		lineTest(6);
	}
	
	private void gridTest(int nestCount, boolean snake) throws Exception {
		ScanRequest<Object> req = createGridScanRequest(nestCount, snake);
		
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		final int expectedRank = nestCount + 2;
		assertEquals(expectedRank, scanInfo.getRank());
		int[] shape = scanInfo.getShape();
		assertEquals(expectedRank, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(4, shape[shape.length - 2]);
		assertEquals(25, shape[shape.length - 1]);
	}
	
	private void gridWithCircularRegionTest(int nestCount, boolean snake) throws Exception {
		ScanRequest<Object> req = createGridWithCircleRegionScanRequest(nestCount, snake);
		
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		int[] shape = scanInfo.getShape();
		assertEquals(nestCount + 1, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(84, shape[shape.length - 1]);
	}
	
	private void gridWithPolygonRegionTest(int nestCount, boolean snake) throws Exception {
		ScanRequest<Object> req = createGridWithPolygonRegionScanRequest(nestCount, snake);
		
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		int[] shape = scanInfo.getShape();
		assertEquals(nestCount + 1, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(52, shape[shape.length - 1]);
	}

	private ScanRequest<Object> createGridScanRequest(int nestCount, boolean snake) {
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		GridModel gridModel = new GridModel("x", "y");
		gridModel.setSlowAxisPoints(4);
		gridModel.setFastAxisPoints(25);
		gridModel.setBoundingBox(box);
		gridModel.setSnake(snake);
		
		Object[] models = new Object[nestCount + 1];
		for (int i = 0; i < nestCount; i++) {
			models[i] = new StepModel("T" + (nestCount - 1 - i), 100, 100 + (10 * i), 10);
		}
		models[nestCount] = gridModel;
		CompoundModel<Object> compoundModel = new CompoundModel<>(models);

		//System.out.println("The number of points will be: "+gen.size());
		
		ScanRequest<Object> req = new ScanRequest<>();
		req.setCompoundModel(compoundModel);
		return req;
	}
	
	private ScanRequest<Object> createGridWithCircleRegionScanRequest(int nestCount, boolean snake) {
		ScanRequest<Object> req = createGridScanRequest(nestCount, snake);
		
		CircularROI roi = new CircularROI(2, 1, 1);
		ScanRegion<Object> circleRegion = new ScanRegion<Object>(roi, "x", "y");
		req.getCompoundModel().setRegions(Arrays.asList(circleRegion));
		
		return req;
	}
	
	private ScanRequest<Object> createGridWithPolygonRegionScanRequest(int nestCount, boolean snake) {
		ScanRequest<Object> req = createGridScanRequest(nestCount, snake);
		
		PolygonalROI diamond = new PolygonalROI(new double[] { 1.5, 0 });
		diamond.insertPoint(new double[] { 3, 1.5 });
		diamond.insertPoint(new double[] { 1.5, 3 });
		diamond.insertPoint(new double[] { 0, 1.5 });

		ScanRegion<Object> circleRegion = new ScanRegion<Object>(diamond, "x", "y");
		req.getCompoundModel().setRegions(Arrays.asList(circleRegion));
		
		return req;
	}
	
	private void spiralTest(int nestCount) throws Exception {
		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(0);
		box.setSlowAxisStart(0);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(3);

		SpiralModel spiralModel = new SpiralModel("x", "y");
		spiralModel.setBoundingBox(box);
		
		Object[] models = new Object[nestCount + 1];
		for (int i = 0; i < nestCount; i++) {
			models[i] = new StepModel("T" + (nestCount - 1- i), 100, 100 + (10 * i), 10);
		}
		models[nestCount] = spiralModel;
		CompoundModel<Object> compoundModel = new CompoundModel<>(models);
		
		ScanRequest<Object> req = new ScanRequest<>();
		req.setCompoundModel(compoundModel);
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		final int expectedRank = nestCount + 1;
		assertEquals(expectedRank, scanInfo.getRank());
		int[] shape = scanInfo.getShape();
		assertEquals(expectedRank, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(15, shape[shape.length - 1]);
	}
	
	private void lineTest(int nestCount) throws Exception {
		LinearROI roi = new LinearROI(new double[] { 0, 0 }, new double [] { 3, 3 });
		ScanRegion<Object> region = new ScanRegion<>(roi, "x", "y");
		// TODO: we need to give the region to the point generator somehow, but the
		// scan estimator doesn't have it at present
		OneDEqualSpacingModel lineModel = new OneDEqualSpacingModel();
		lineModel.setPoints(10);
		lineModel.setFastAxisName("x");
		lineModel.setSlowAxisName("y");
		
		Object[] models = new Object[nestCount + 1];
		for (int i = 0; i < nestCount; i++) {
			models[i] = new StepModel("T" + (nestCount - 1- i), 100, 100 + (10 * i), 10);
		}
		models[nestCount] = lineModel;
		CompoundModel<Object> compoundModel = new CompoundModel<>(models);
		compoundModel.setRegions(Arrays.asList(region));
		
		ScanRequest<Object> req = new ScanRequest<>();
		req.setCompoundModel(compoundModel);
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		final int expectedRank = nestCount + 1;
		assertEquals(expectedRank, scanInfo.getRank());
		int[] shape = scanInfo.getShape();
		assertEquals(expectedRank, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(10, shape[shape.length - 1]);
	}

}
