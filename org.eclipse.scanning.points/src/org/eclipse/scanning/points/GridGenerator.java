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

import static org.eclipse.scanning.points.AbstractScanPointIterator.EMPTY_PY_ARRAY;

import java.util.Iterator;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

class GridGenerator extends AbstractGenerator<GridModel> {

	GridGenerator() {
		setLabel("Grid");
		setDescription("Creates a grid scan (a scan of x and y).\nThe scan supports bidirectional or 'snake' mode.");
		setIconPath("icons/scanner--grid.png"); // This icon exists in the rendering bundle
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getSlowAxisPoints() <= 0) throw new ModelValidationException("Model must have a positive number of slow axis points!", model, "slowAxisPoints");
		if (model.getFastAxisPoints() <= 0) throw new ModelValidationException("Model must have a positive number of fast axis points!", model, "fastAxisPoints");
		if (model.getFastAxisName()==null) throw new ModelValidationException("The model must have a fast axis!\nIt is the motor name used for this axis.", model, "fastAxisName");
		if (model.getSlowAxisName()==null) throw new ModelValidationException("The model must have a slow axis!\nIt is the motor name used for this axis.", model, "slowAxisName");
	}

	@Override
	public ScanPointIterator iteratorFromValidModel() {
		final GridModel model = getModel();

		final int columns = model.getFastAxisPoints();
		final int rows = model.getSlowAxisPoints();
		final String xName = model.getFastAxisName();
		final String yName = model.getSlowAxisName();
		final double xStep = model.getBoundingBox().getFastAxisLength() / columns;
		final double yStep = model.getBoundingBox().getSlowAxisLength() / rows;
		final double minX = model.getBoundingBox().getFastAxisStart() + xStep / 2;
		final double minY = model.getBoundingBox().getSlowAxisStart() + yStep / 2;

		final JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

		final ScanPointIterator outerLine = lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows, model.isSnake());

		final ScanPointIterator innerLine = lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());

        final Iterator<?>[] generators = { outerLine, innerLine };

        final String[] axisNames = new String[] { xName, yName };
		final ScanPointIterator pyIterator = CompoundSpgIteratorFactory.createSpgCompoundGenerator(generators,
				getRegions().toArray(),	axisNames, EMPTY_PY_ARRAY, -1, model.isContinuous());

		return new SpgIterator(pyIterator);
	}

	@Override
	public String toString() {
		return "GridGenerator [" + super.toString() + "]";
	}

}
