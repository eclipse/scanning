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

import java.util.Iterator;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.RandomOffsetGridModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

public class RandomOffsetGridTest {

	private IPointGeneratorService service;

	@Before
	public void before() throws Exception {
		service = new PointGeneratorService();
	}
	
	@Test
	public void testSimpleBox() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(-0.5);
		box.setSlowAxisStart(-1.0);
		box.setFastAxisLength(5);
		box.setSlowAxisLength(10);

		final double offsetScale = 0.25;

		RandomOffsetGridModel rm = new RandomOffsetGridModel("x", "y");
		rm.setSlowAxisPoints(5);
		rm.setFastAxisPoints(5);
		rm.setBoundingBox(box);
		rm.setSeed(10);
		rm.setOffset(offsetScale * 100);

		GridModel gm = new GridModel("x", "y");
		gm.setSlowAxisPoints(5);
		gm.setFastAxisPoints(5);
		gm.setBoundingBox(box);

		IPointGenerator<RandomOffsetGridModel> r = service.createGenerator(rm);
		IPointGenerator<GridModel> g = service.createGenerator(gm);
		final int expectedSize = 25;
		assertEquals(expectedSize, g.size());
		assertEquals(2, g.getRank());
		assertArrayEquals(new int[] { 5, 5 }, g.getShape());

		for (Iterator<IPosition> it1 = r.iterator(), it2 = g.iterator(); it1.hasNext() && it2.hasNext();) {
			IPosition t1 = it1.next();
			IPosition t2 = it2.next();
			assertEquals(t1.getIndices(), t2.getIndices());
			assertEquals(t1.getNames(), t2.getNames());
			assertEquals(t1.getValue("x"), t2.getValue("x"), offsetScale);
			assertEquals(t1.getValue("y"), t2.getValue("y"), offsetScale);
		}
	}

}
