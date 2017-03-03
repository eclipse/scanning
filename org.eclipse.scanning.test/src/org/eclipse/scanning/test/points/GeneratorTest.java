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
import java.util.List;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;

public abstract class GeneratorTest {

	protected IPointGeneratorService service;

	@Before
	public void before() throws Exception {
	
		service = new PointGeneratorService();
	}

	
	public void checkWrtCompound(Object model, IROI roi, int size) throws Exception {
		
		// Get the point list
		IPointGenerator<?> generator = roi!=null ? service.createGenerator(model, roi) : service.createGenerator(model);
	    List<IPosition> pointList = generator.createPoints();

		assertEquals(size, pointList.size());
		assertEquals(size, generator.size());

		CompoundModel<IROI> cmodel = new CompoundModel<>(model);
		if (roi!=null) cmodel.setRegions(Arrays.asList(new ScanRegion(roi, Arrays.asList("x", "y"))));
		
		IPointGenerator<?> cgenerator = service.createCompoundGenerator(cmodel); 
	    List<IPosition> cpointList = cgenerator.createPoints();
		assertEquals(size, cpointList.size());
		assertEquals(size, cgenerator.size());

	}

}
