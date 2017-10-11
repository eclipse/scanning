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
import java.util.Iterator;

import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;

class SpiralIterator extends AbstractScanPointIterator {

	// Constant parameters
	private final String xName;
	private final String yName;
	private final double xCentre;
	private final double yCentre;
	private final double maxRadius;

	public SpiralIterator(SpiralGenerator gen) {

		SpiralModel model = gen.getModel();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();

		double radiusX = model.getBoundingBox().getFastAxisLength() / 2;
		double radiusY = model.getBoundingBox().getSlowAxisLength() / 2;
		xCentre = model.getBoundingBox().getFastAxisStart() + radiusX;
		yCentre = model.getBoundingBox().getSlowAxisStart() + radiusY;
		maxRadius = Math.sqrt(radiusX * radiusX + radiusY * radiusY);

        JythonObjectFactory<ScanPointIterator> spiralGeneratorFactory = ScanPointGeneratorFactory.JSpiralGeneratorFactory();

        PyList names =  new PyList(Arrays.asList(xName, yName));
        PyList units = new PyList(Arrays.asList("mm", "mm"));
        PyList centre = new PyList(Arrays.asList(xCentre, yCentre));
        double radius = maxRadius;
        double scale = model.getScale();
        boolean alternate = false;

		ScanPointIterator spiral = spiralGeneratorFactory.createObject(
				names, units, centre, radius, scale, alternate);
		pyIterator = createSpgCompoundGenerator(new Iterator<?>[] {spiral}, gen.getRegions().toArray(),
				new String[] {xName, yName}, EMPTY_PY_ARRAY, -1, model.isContinuous());
	}
}
