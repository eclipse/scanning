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
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

class RasterGenerator extends AbstractGenerator<RasterModel> {

	RasterGenerator() {
		setLabel("Raster");
		setDescription("Creates a raster scan (a scan of x and y).\nThe scan supports bidirectional or 'snake' mode.");
		setIconPath("icons/scanner--raster.png"); // This icon exists in the rendering bundle
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getFastAxisStep() == 0) throw new ModelValidationException("Model fast axis step size must be nonzero!", model, "fastAxisStep");
		if (model.getSlowAxisStep() == 0) throw new ModelValidationException("Model slow axis step size must be nonzero!", model, "slowAxisStep");

		// Technically the following two throws are not required
		// (The generator could simply produce an empty list.)
		// but we throw errors to avoid potential confusion.
		// Plus, this is consistent with the StepGenerator behaviour.
		if (model.getFastAxisStep()/model.getBoundingBox().getFastAxisLength() < 0)
			throw new ModelValidationException("Model fast axis step is directed so as to produce no points!", model, "fastAxisStep");
		if (model.getSlowAxisStep()/model.getBoundingBox().getSlowAxisLength() < 0)
			throw new ModelValidationException("Model slow axis step is directed so as to produce no points!", model, "slowAxisStep");
	}

	@Override
	public ScanPointIterator iteratorFromValidModel() {
		final RasterModel model = getModel();
		final double xStep = model.getFastAxisStep();
		final double yStep = model.getSlowAxisStep();
		final String xName = model.getFastAxisName();
		final String yName = model.getSlowAxisName();
		final double minX = model.getBoundingBox().getFastAxisStart();
		final double minY = model.getBoundingBox().getSlowAxisStart();
		final int columns = (int) Math.floor(model.getBoundingBox().getFastAxisLength() / xStep + 1);
		final int rows = (int) Math.floor(model.getBoundingBox().getSlowAxisLength() / yStep + 1);

		final JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

		final ScanPointIterator outerLine = lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows);
		final ScanPointIterator innerLine = lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());

        final Iterator<?>[] generators = {outerLine, innerLine};
        final String[] axisNames = new String[] { xName, yName };

		final ScanPointIterator pyIterator = CompoundSpgIteratorFactory.createSpgCompoundGenerator(
				generators, getRegions().toArray(), axisNames,
				EMPTY_PY_ARRAY, -1, model.isContinuous());
		return new SpgIterator(pyIterator);
	}

}
