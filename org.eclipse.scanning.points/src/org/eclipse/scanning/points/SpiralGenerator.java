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

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;

class SpiralGenerator extends AbstractGenerator<SpiralModel> {

	SpiralGenerator() {
		setLabel("Fermat Spiral");
		setDescription("Creates a spiral scaled around the center of a bounding box.");
		setIconPath("icons/scanner--spiral.png"); // This icon exists in the rendering bundle
	}

	@Override
	public ScanPointIterator iteratorFromValidModel() {
		final SpiralModel model = getModel();
		final String xName = model.getFastAxisName();
		final String yName = model.getSlowAxisName();

		final double radiusX = model.getBoundingBox().getFastAxisLength() / 2;
		final double radiusY = model.getBoundingBox().getSlowAxisLength() / 2;
		final double xCentre = model.getBoundingBox().getFastAxisStart() + radiusX;
		final double yCentre = model.getBoundingBox().getSlowAxisStart() + radiusY;
		final double maxRadius = Math.sqrt(radiusX * radiusX + radiusY * radiusY);

        final JythonObjectFactory<ScanPointIterator> spiralGeneratorFactory = ScanPointGeneratorFactory.JSpiralGeneratorFactory();

        final PyList axisNamesPy =  new PyList(Arrays.asList(xName, yName));
        final PyList units = new PyList(Arrays.asList("mm", "mm"));
        final PyList centre = new PyList(Arrays.asList(xCentre, yCentre));
        final double radius = maxRadius;
        final double scale = model.getScale();
        final boolean alternate = false;

		final ScanPointIterator spiral = spiralGeneratorFactory.createObject(
				axisNamesPy, units, centre, radius, scale, alternate);
		final Iterator<?>[] iterators = new Iterator<?>[] { spiral };
		final String[] axisNames = new String[] { xName, yName };
		final ScanPointIterator pyIterator = CompoundSpgIteratorFactory.createSpgCompoundGenerator(
				iterators, getRegions().toArray(),
				axisNames, EMPTY_PY_ARRAY, -1, model.isContinuous());

		return new SpgIterator(pyIterator);
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getScale() == 0.0) throw new ModelValidationException("Scale must be non-zero!", model, "scale");
	}
}
