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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.eclipse.dawnsci.analysis.api.roi.IRectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;

public class PointServiceTest {
	
	private IPointGeneratorService pservice;
	
	@Before
	public void before() {
		pservice = new PointGeneratorService();
	}

	/**
	 * This tests an important feature of the service
	 * that it takes the bounds of all the regions and
	 * then cuts out the regions from those bounds.
	 * 
	 * Bounds are not conserved in the IBoundingBoxModel
	 * @throws GeneratorException 
	 * 
	 */
	@Test
	public void testMultipleGenerationsDifferentBoxes() throws Exception {
		
		IRectangularROI roi1 = new RectangularROI(0,0,5,5,0);
		
		GridModel model = new GridModel("x", "y");
		model.setSlowAxisPoints(5);
		model.setFastAxisPoints(5);

		pservice.createGenerator(model, roi1); // Sets the bounding box
		
		BoundingBox box1 = model.getBoundingBox();
		assertNotNull(box1);
		checkSame(box1, roi1);
	
		IRectangularROI roi2 = new RectangularROI(10,10,5,5,0);
		pservice.createGenerator(model, roi2); // Sets the bounding box

		BoundingBox box2 = model.getBoundingBox();
		assertNotNull(box2);
		checkSame(box2, new RectangularROI(0,0,15,15,0));
		
	}

	private void checkSame(BoundingBox box, IRectangularROI roi) throws Exception {
		assertEquals(box.getFastAxisStart(), roi.getPointX(), 0.00001);
		assertEquals(box.getSlowAxisStart(), roi.getPointY(), 0.00001);
		assertEquals(box.getFastAxisLength(), roi.getLength(0), 0.00001);
		assertEquals(box.getSlowAxisLength(), roi.getLength(1), 0.00001);
	}
}
