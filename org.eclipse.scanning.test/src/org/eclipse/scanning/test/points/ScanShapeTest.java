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

import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
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
	public void testShapeGrid1D() throws Exception {
		gridTest(1, false);
	}
	
	@Test
	public void testShapeGrid2D() throws Exception {
		gridTest(2, false);
	}
	
	@Test
	public void testShapeGrid3D() throws Exception {
		gridTest(3, false);
	}
	
	@Test
	@Ignore
	public void testShapeGrid8D() throws Exception {
		gridTest(8, false);
	}
	
	@Test
	public void testShapeSnakeGrid1D() throws Exception {
		gridTest(1, true);
	}
	
	@Test
	public void testShapeSnakeGrid2D() throws Exception {
		gridTest(2, true);
	}
	
	@Test
	public void testShapeSnakeGrid3D() throws Exception {
		gridTest(3, true);
	}
	
	@Test
	@Ignore
	public void testShapeSnakeGrid8D() throws Exception {
		gridTest(8, true);
	}
	
	@Test
	public void testShapeSpiral1D() throws Exception {
		spiralTest(1);
	}
	
	@Test
	public void testShapeSpiral2D() throws Exception {
		spiralTest(2);
	}
	
	@Test
	public void testShapeSpiral3D() throws Exception {
		spiralTest(3);
	}
	
	@Test
	@Ignore
	public void testShapeSpiral8D() throws Exception {
		spiralTest(8);
	}
	
	@Test
	@Ignore
	public void testShapeLine1D() throws Exception {
		lineTest(1);
	}
	
	@Test
	@Ignore
	public void testShapeLine2D() throws Exception {
		lineTest(2);
	}
	
	@Test
	@Ignore
	public void testShapeLine3D() throws Exception {
		lineTest(3);
	}
	
	@Test
	@Ignore
	public void testShapeLine8D() throws Exception {
		lineTest(8);
	}
	
	private void gridTest(int nestCount, boolean snake) throws Exception {
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
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		int[] shape = scanInfo.getShape();
		assertEquals(nestCount + 2, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(4, shape[shape.length - 2]);
		assertEquals(25, shape[shape.length - 1]);
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
		
		int[] shape = scanInfo.getShape();
		assertEquals(nestCount + 1, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(15, shape[shape.length - 1]);
	}
	
	private void lineTest(int nestCount) throws Exception {
		LinearROI roi = new LinearROI(new double[] { 0, 0 }, new double [] { 3, 3 });
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
		
		ScanRequest<Object> req = new ScanRequest<>();
		req.setCompoundModel(compoundModel);
		ScanEstimator scanEstimator = new ScanEstimator(service, req);
		ScanInformation scanInfo = new ScanInformation(scanEstimator);
		
		int[] shape = scanInfo.getShape();
		assertEquals(nestCount + 1, shape.length);
		for (int i = 0; i < nestCount; i++) {
			assertEquals(i + 1, shape[i]);
		}
		assertEquals(10, shape[shape.length - 1]);
	}

}
