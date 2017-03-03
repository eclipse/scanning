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

import java.util.List;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

public class MultiStepTest {
	
	private IPointGeneratorService service;
	
	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}
	
	@Test(expected = ModelValidationException.class)
	public void testNoName() throws Exception {
		MultiStepModel model = new MultiStepModel();
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}

	@Test(expected = ModelValidationException.class)
	public void testEmpty() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}

	@Test
	public void testSingleForward() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(10, 20, 2);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
		List<IPosition> pointList = gen.createPoints();
		
		assertEquals(pointList.size(), gen.size());
		assertEquals(6, pointList.size());
		for (int i = 0; i < pointList.size(); i++) {
			assertEquals(new Scalar<>("x", i, 10.0 + (2 * i)), pointList.get(i));
		}
	}
	
	@Test(expected = ModelValidationException.class)
	public void testSingleForwardStepsWrongDir() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(10.0, 20.0, -1.0);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test
	public void testSingleBackward() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(20.0, 10.0, -2.0);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
		List<IPosition> pointList = gen.createPoints();
		assertEquals(pointList.size(), gen.size());
		
		assertEquals(6, pointList.size());
		for (int i = 0; i < pointList.size(); i++) {
			assertEquals(new Scalar<>("x", i, 20.0 - (2 * i)), pointList.get(i));
		}
	}
	
	@Test(expected = ModelValidationException.class)
	public void testSingleBackwardStepsWrongDir() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(20, 10, 2);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test
	public void testMultipleForward() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(10, 20, 2);
		model.addRange(25, 50, 5);
		model.addRange(100, 500, 50);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
		
		List<IPosition> pointList = gen.createPoints();
		assertEquals(pointList.size(), gen.size());
		assertEquals(21, pointList.size()); // 6 + 6 + 9
		
		for (int i = 0; i < pointList.size(); i++) {
			double expected;
			if (i < 6) expected = 10 + 2 * i;
			else if (i < 12) expected = 25 + 5 * (i - 6);
			else expected = 100 + 50 * (i - 12);
			assertEquals(new Scalar<>("x", i, expected), pointList.get(i));
		}
	}
	
	@Test
	public void testMultipleBackward() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(500, 100, -50);
		model.addRange(50, 25, -5);
		model.addRange(20, 10, -2);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);

		List<IPosition> pointList = gen.createPoints();
		assertEquals(pointList.size(), gen.size());
		assertEquals(21, pointList.size()); // 9 + 6 + 6
		
		for (int i = 0; i < pointList.size(); i++) {
			double expected;
			if (i < 9) expected = 500 - 50 * i;
			else if (i < 15) expected = 50 - 5 * (i - 9);
			else expected = 20 - 2 * (i - 15);
			assertEquals(new Scalar<>("x", i, expected), pointList.get(i));
		}
	}
	
	@Test
	public void testForwardNoGap() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(10, 20, 2); // 6
		model.addRange(20, 100, 5); // 17
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
		
		List<IPosition> pointList = gen.createPoints();
		assertEquals(pointList.size(), gen.size());
		assertEquals(22, pointList.size()); // 6 + 17 - 1, as 20 should only appear once
		
		for (int i = 0; i < pointList.size(); i++) {
			double expected;
			if (i < 6) expected = 10 + 2 * i;
			else expected = 20 + 5 * (i - (6 - 1));
			assertEquals(new Scalar<>("x", i, expected), pointList.get(i));
		}
	}
	
	@Test
	public void testBackwardNoGap() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(100, 20, -5); // 17
		model.addRange(20, 10, -2); // 6
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
		
		List<IPosition> pointList = gen.createPoints();
		assertEquals(pointList.size(), gen.size());
		assertEquals(22, pointList.size()); // 17 + 6 - 1, as 20 should only appear once
		
		for (int i = 0; i < pointList.size(); i++) {
			double expected;
			if (i < 17) expected = 100 - 5 * i;
			else expected = 20 - 2 * (i - (17 - 1));
			assertEquals(new Scalar<>("x", i, expected), pointList.get(i));
		}
	}
	
	@Test(expected = ModelValidationException.class)
	public void testForwardOverlapping() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(10, 20, 2);
		model.addRange(15, 50, 5);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test(expected = ModelValidationException.class)
	public void testBackwardOverlapping() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(50, 20, -5);
		model.addRange(22, 10, -2);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test(expected = ModelValidationException.class)
	public void testForwardThenBackward() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(10, 20, 2);
		model.addRange(50, 25, -5);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}
	
	@Test(expected = ModelValidationException.class)
	public void testBackwardThenForward() throws Exception {
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		
		model.addRange(50, 25, -5);
		model.addRange(10, 20, 2);
		
		IPointGenerator<MultiStepModel> gen = service.createGenerator(model);
		GeneratorUtil.testGeneratorPoints(gen);
	}
	
}
