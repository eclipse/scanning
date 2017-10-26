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
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.BoundingLine;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;

public class OneDEqualSpacingGenerator extends AbstractGenerator<OneDEqualSpacingModel> {

	OneDEqualSpacingGenerator() {
		setLabel("Line Equal Spacing");
		setDescription("Creates a line scan along a line defined in two dimensions.");
		setIconPath("icons/scanner--line.png"); // This icon exists in the rendering bundle
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getPoints() < 1) throw new ModelValidationException("Must have one or more points in model!", model, "points");
	}

	@Override
	public ScanPointIterator iteratorFromValidModel() {
		final OneDEqualSpacingModel model =  getModel();
		final BoundingLine line = model.getBoundingLine();

		final JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

		final int numPoints = model.getPoints();
		final double step = line.getLength() / numPoints;
		final double xStep = step * Math.cos(line.getAngle());
		final double yStep = step * Math.sin(line.getAngle());

		final PyList names =  new PyList(Arrays.asList(model.getFastAxisName(), model.getSlowAxisName()));
		final PyList units = new PyList(Arrays.asList("mm", "mm"));
		final double[] start = {line.getxStart() + xStep/2, line.getyStart() + yStep/2};
		final double[] stop = {line.getxStart() + xStep * (numPoints - 0.5), line.getyStart() + yStep * (numPoints - 0.5)};

		final ScanPointIterator pyIterator = lineGeneratorFactory.createObject(
				names, units, start, stop, numPoints);
		return new SpgIterator(pyIterator);
	}

	@Override
	public int[] getShape() {
		return new int[] { getModel().getPoints() };
	}
}
