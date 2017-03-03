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

import org.eclipse.scanning.api.points.StaticPosition;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

public class StaticTest {
	
	private IPointGeneratorService service;
	
	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}
	
	@Test
	public void testSingleStatic() throws Exception {
		StaticModel model = new StaticModel();
		IPointGenerator<StaticModel> gen = service.createGenerator(model);
		assertEquals(1, gen.size());
		
		List<IPosition> positionList = gen.createPoints();
		assertEquals(1, positionList.size());
		IPosition position = positionList.get(0);
		assertEquals(0, position.size());
		assertEquals(new StaticPosition(), position);
	}
	
	@Test
	public void testMultipleStatic() throws Exception {
		final int size = 8;
		StaticModel model = new StaticModel();
		model.setSize(size);
		IPointGenerator<StaticModel> gen = service.createGenerator(model);
		assertEquals(size, gen.size());
		
		final StaticPosition expected = new StaticPosition();
		List<IPosition> positionList = gen.createPoints();
		assertEquals(size, positionList.size());
		for (IPosition position : positionList) {
			assertEquals(0, position.size());
			assertEquals(expected, position);
		}
	}
	
	@Test(expected = ModelValidationException.class)
	public void testInvalidZeroSize() throws Exception {
		StaticModel model = new StaticModel();
		model.setSize(0);
		IPointGenerator<StaticModel> gen = service.createGenerator(model);
		gen.iterator();
	}

	@Test(expected = ModelValidationException.class)
	public void testInvalidNegativeSize() throws Exception {
		StaticModel model = new StaticModel();
		model.setSize(-3);
		IPointGenerator<StaticModel> gen = service.createGenerator(model);
		gen.iterator();
	}

}
