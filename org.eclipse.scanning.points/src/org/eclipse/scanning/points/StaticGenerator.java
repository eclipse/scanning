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

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

/**
 * A software generator that generates a static (i.e. empty) point one or more times.
 *
 * @author Matthew Dickie
 */
class StaticGenerator extends AbstractGenerator<StaticModel> {

	private static final int[] EMPTY_SHAPE = new int[0];

	StaticGenerator() {
		setLabel("Empty");
		setDescription("Empty generator used when wrapping malcolm scans with no CPU steps.");
		setVisible(false);
	}

	@Override
	protected void validateModel() {
		if (model.getSize() < 1) throw new ModelValidationException("Size must be greater than zero!", model, "size");
	}

	@Override
	protected ScanPointIterator iteratorFromValidModel() {
		final JythonObjectFactory<ScanPointIterator> staticGeneratorFactory = ScanPointGeneratorFactory.JStaticGeneratorFactory();

		final int numPoints = model.getSize();

		final ScanPointIterator pyIterator = staticGeneratorFactory.createObject(numPoints);
		return new SpgIterator(pyIterator);
	}

	// Users to not edit the StaticGenerator
	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public boolean isScanPointGeneratorFactory() {
		return false;
	}

	@Override
	public int[] getShape() throws GeneratorException {
		return model.getSize() == 1 ? EMPTY_SHAPE : new int[] { model.getSize() };
	}
}
