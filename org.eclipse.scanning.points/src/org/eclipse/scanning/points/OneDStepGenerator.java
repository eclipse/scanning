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
package org.eclipse.scanning.points;

import java.util.Arrays;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.BoundingLine;
import org.eclipse.scanning.api.points.models.OneDStepModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;

class OneDStepGenerator extends AbstractGenerator<OneDStepModel> {

	OneDStepGenerator() {
		setLabel("Point");
		setDescription("Creates a point to scan.");
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getStep() <= 0) throw new ModelValidationException("Model step size must be positive!", model, "step");
	}

	@Override
	protected ScanPointIterator iteratorFromValidModel() {
		final OneDStepModel model= getModel();
		final BoundingLine line = model.getBoundingLine();

        final JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

		final int numPoints = (int) Math.floor(line.getLength() / model.getStep()) + 1;
        final double xStep = model.getStep() * Math.cos(line.getAngle());
        final double yStep = model.getStep() * Math.sin(line.getAngle());

		final PyList names =  new PyList(Arrays.asList(model.getFastAxisName(), model.getSlowAxisName()));
		final PyList units = new PyList(Arrays.asList("mm", "mm"));
		final double[] start = {line.getxStart(), line.getyStart()};
        final double[] stop = {line.getxStart() + xStep * numPoints, line.getyStart() + yStep * numPoints};

		final ScanPointIterator pyIterator = lineGeneratorFactory.createObject(
				names, units, start, stop, numPoints);
		return new SpgIterator(pyIterator);
	}

	@Override
	public int[] getShape() throws GeneratorException {
		BoundingLine line = getModel().getBoundingLine();
		if (line != null) {
			return new int[] { (int) Math.floor(line.getLength() / getModel().getStep()) + 1 };
		}

		return super.getShape();
	}

}
