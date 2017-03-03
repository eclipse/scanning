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
package org.eclipse.scanning.test.validation;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.junit.Test;

public class CompoundModelTest extends AbstractValidationTest {

	
	@Test(expected=ValidationException.class)
	public void testNoBoundingBox() throws ValidationException, InstantiationException, IllegalAccessException {
		
		final CompoundModel<?> cmodel = new CompoundModel<>(Arrays.asList(new StepModel("fred", 10, 20, 1), new GridModel("stage_x", "stage_y")));
		validator.validate(cmodel);
	}

	@Test(expected=ValidationException.class)
	public void testAxesColliding() {
		
		final CompoundModel<IROI> cmodel = new CompoundModel<>(Arrays.asList(new StepModel("stage_x", 10, 20, 1), new GridModel("stage_x", "stage_y")));
		try {
			validator.validate(cmodel);
		} catch (InstantiationException | IllegalAccessException e) {
			fail("Unexpected exception in test!");
		}
	}

	@Test
	public void testBoundingBox() throws ValidationException, InstantiationException, IllegalAccessException {
		
		GridModel gmodel = new GridModel("stage_x", "stage_y");
		gmodel.setBoundingBox(new BoundingBox(10, -10, 100, -100));
		validator.validate(new CompoundModel<>(new StepModel("fred", 10, 20, 1), gmodel));
	}
	
	@Test(expected=ValidationException.class)
	public void nullAxisTest() throws ValidationException, InstantiationException, IllegalAccessException {
		
		GridModel gmodel = new GridModel(null, "stage_y");
		gmodel.setBoundingBox(new BoundingBox(10, -10, 100, -100));
		validator.validate(new CompoundModel<>(Arrays.asList(new StepModel("fred", 10, 20, 1), gmodel)));
	}
}
