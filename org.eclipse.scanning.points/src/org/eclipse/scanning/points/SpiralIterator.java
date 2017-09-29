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

import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.SpiralModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;
import org.python.core.PyObject;

class SpiralIterator extends AbstractScanPointIterator {

	// Constant parameters
	private final String xName;
	private final String yName;
	private final double xCentre;
	private final double yCentre;
	private final double maxRadius;

	private Point currentPoint;

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

        PyList names =  new PyList(Arrays.asList(new String[] {xName, yName}));
        PyList units = new PyList(Arrays.asList(new String[] {"mm", "mm"}));
        PyList centre = new PyList(Arrays.asList(new Double[] {xCentre, yCentre}));
        double radius = maxRadius;
        double scale = model.getScale();
        boolean alternate = false;

		ScanPointIterator spiral = spiralGeneratorFactory.createObject(
				names, units, centre, radius, scale, alternate);
		pyIterator = createSpgCompoundGenerator(new Iterator<?>[] {spiral}, gen.getRegions().toArray(),
				new String[] {xName, yName}, new PyObject[] {});
	}

	@Override
	public boolean hasNext() {
		if (pyIterator.hasNext()) {
			currentPoint = (Point) pyIterator.next();
			return true;
		}
		return false;
	}

	@Override
	public Point next() {
		// TODO: This will return null if called without calling hasNext() and when the
		// ROI will exclude all further points. Raise error if called without hasNext()
		// first, or if point is null?
		if (currentPoint == null) {
			hasNext();
		}
		Point point = currentPoint;
		currentPoint = null;

		return point;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove");
	}
}
