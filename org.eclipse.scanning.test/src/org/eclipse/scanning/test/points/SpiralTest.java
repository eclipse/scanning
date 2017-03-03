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

import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.junit.Ignore;
import org.junit.Test;

public class SpiralTest extends GeneratorTest {

	@Test
	public void testSpiralNoROI() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(-10);
		box.setSlowAxisStart(5);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(4);

		SpiralModel model = new SpiralModel("x", "y");
		model.setBoundingBox(box);

		// Get the point list
		IPointGenerator<SpiralModel> generator = service.createGenerator(model);
	    List<IPosition> pointList = generator.createPoints();

		assertEquals(20, pointList.size());

		// Test a few points
		// TODO check x and y index values - currently these are not tested by AbstractPosition.equals()
		assertEquals(new Point("x", 0, -8.263367850554253, "y", 0, 6.678814432234913, false), pointList.get(0));
		assertEquals(new Point("x", 3, -8.139330427516057, "y", 3, 7.991968780318976, false), pointList.get(3));
		assertEquals(new Point("x", 15, -6.315009394139057, "y", 15, 7.399523826759042, false), pointList.get(15));
	}
	
	@Test
	public void testSpiralNoROIWrtCompound() throws Exception {

		BoundingBox box = new BoundingBox();
		box.setFastAxisStart(-10);
		box.setSlowAxisStart(5);
		box.setFastAxisLength(3);
		box.setSlowAxisLength(4);

		SpiralModel model = new SpiralModel("x", "y");
		model.setBoundingBox(box);

		checkWrtCompound(model, null, 20);
	}

	
	// FIXME
	@Ignore("This should pass because compound of a model should equal the points from that model directly")
	@Test
	public void testSpiralWrtCompound() throws Exception {

		RectangularROI roi = new RectangularROI(28.5684, 24.0729, 50.4328, 54.2378, 0.0);		
		SpiralModel model = new SpiralModel("x", "y");
		model.setScale(2.0);

        checkWrtCompound(model, roi, 682);
	}

}
