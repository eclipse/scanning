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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingLine;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.OneDStepModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

public class LinearTest {

	private IPointGeneratorService service;
	
	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}
	
	@Test
	public void testOneDEqualSpacing() throws Exception {
		
		BoundingLine line = new BoundingLine();
		line.setxStart(0.0);
		line.setyStart(0.0);
		line.setLength(Math.hypot(3.0, 3.0));

        OneDEqualSpacingModel model = new OneDEqualSpacingModel();
        model.setPoints(10);
        model.setBoundingLine(line);
		
		// Get the point list
		IPointGenerator<OneDEqualSpacingModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
		
		assertEquals(pointList.size(), gen.size());
		assertEquals(pointList.size(), 10);
        GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test
	public void testIndicesOneDEqualSpacing() throws Exception {
        
        BoundingLine line = new BoundingLine();
        line.setxStart(0.0);
        line.setyStart(0.0);
        line.setLength(Math.hypot(3.0, 3.0));

        OneDEqualSpacingModel model = new OneDEqualSpacingModel();
        model.setPoints(10);
        model.setBoundingLine(line);
 		
		// Get the point list
		IPointGenerator<OneDEqualSpacingModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
		
		assertEquals(pointList.size(), gen.size());
		assertEquals(pointList.size(), 10);
        GeneratorUtil.testGeneratorPoints(gen);
        
        for (int i = 0; i < pointList.size(); i++) {
		    IPosition pos = pointList.get(i);
		    int xIndex = pos.getIndex(model.getFastAxisName());
		    int yIndex = pos.getIndex(model.getSlowAxisName());
		    
		    assertEquals(i, xIndex);
		    assertEquals(i, yIndex);
		    assertTrue(pos.getScanRank()==1);
		}
	}

	
	@Test
	public void testOneDEqualSpacingNoROI() throws GeneratorException {
		
		OneDEqualSpacingModel model = new OneDEqualSpacingModel();
		model.setPoints(10);
		BoundingLine bl = new BoundingLine();
		bl.setxStart(0);
		bl.setyStart(0);
		bl.setAngle(0);
		bl.setLength(10);
		model.setBoundingLine(bl);

		// Get the point list
		IPointGenerator<OneDEqualSpacingModel> gen = service.createGenerator(model);
		gen.createPoints();
	}

	@Test(expected = ModelValidationException.class)
	public void testOneDEqualSpacingNoPoints() throws Exception {
        
        BoundingLine line = new BoundingLine();
        line.setxStart(0.0);
        line.setyStart(0.0);
        line.setLength(Math.hypot(3.0, 3.0));

        OneDEqualSpacingModel model = new OneDEqualSpacingModel();
        model.setPoints(0);
        model.setBoundingLine(line);
		
		// Get the point list
		IPointGenerator<OneDEqualSpacingModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
        GeneratorUtil.testGeneratorPoints(gen);
	}

	
	@Test
	public void testOneDStep() throws Exception {
        
        BoundingLine line = new BoundingLine();
        line.setxStart(0.0);
        line.setyStart(0.0);
        line.setLength(Math.hypot(3.0, 3.0));

        OneDStepModel model = new OneDStepModel();
        model.setStep(0.3);
        model.setBoundingLine(line);
		
		// Get the point list
		IPointGenerator<OneDStepModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
		
		assertEquals(pointList.size(), gen.size());
		assertEquals(15, pointList.size());
        GeneratorUtil.testGeneratorPoints(gen);
	}

	@Test
	public void testOneDStepNoROI() throws GeneratorException {

		OneDStepModel model = new OneDStepModel();
		model.setStep(1);
		BoundingLine bl = new BoundingLine();
		bl.setxStart(0);
		bl.setyStart(0);
		bl.setAngle(0);
		bl.setLength(10);
		model.setBoundingLine(bl);

		// Get the point list
		IPointGenerator<OneDStepModel> gen = service.createGenerator(model);
		gen.createPoints();
	}

	@Test(expected = ModelValidationException.class)
	public void testOneDStepNoStep() throws Exception {
        
        BoundingLine line = new BoundingLine();
        line.setxStart(0.0);
        line.setyStart(0.0);
        line.setLength(Math.hypot(3.0, 3.0));

        OneDStepModel model = new OneDStepModel();
        model.setStep(0);
        model.setBoundingLine(line);
		
		// Get the point list
		IPointGenerator<OneDStepModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
        GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test(expected = ModelValidationException.class)
	public void testOneDStepNegativeStep() throws Exception {
        
        BoundingLine line = new BoundingLine();
        line.setxStart(0.0);
        line.setyStart(0.0);
        line.setLength(Math.hypot(3.0, 3.0));

		OneDStepModel model = new OneDStepModel();
		model.setStep(-0.3);
		model.setBoundingLine(line);

		// Get the point list
		IPointGenerator<OneDStepModel> gen = service.createGenerator(model);
		List<IPosition> pointList = gen.createPoints();
		GeneratorUtil.testGeneratorPoints(gen);
	}

	@Test
	public void testOneDStepWrongROI() throws Exception {
		
		try {
			RectangularROI roi = new RectangularROI(new double[]{0,0}, new double[]{3,3});
	        
	        BoundingLine line = new BoundingLine();
	        line.setxStart(0.0);
	        line.setyStart(0.0);
	        line.setLength(Math.hypot(3.0, 3.0));
	
	        OneDStepModel model = new OneDStepModel();
	        model.setStep(0);
	        model.setBoundingLine(line);
			
			// Get the point list
			IPointGenerator<OneDStepModel> gen = service.createGenerator(model, roi);
			List<IPosition> pointList = gen.createPoints();
	        GeneratorUtil.testGeneratorPoints(gen);
		} catch (ModelValidationException | GeneratorException e) {
			return;
		}
		throw new Exception("testOneDStepWrongROI did not throw an exception as expected!");
	}
}
