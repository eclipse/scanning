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
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.RandomOffsetGridModel;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;

class GridIterator extends AbstractScanPointIterator {

	private final int columns;
	private final int rows;
	private final String xName;
	private final String yName;
	private final double minX;
	private final double minY;
	private final double xStep;
	private final double yStep;

	private Point currentPoint;

	public GridIterator(GridGenerator gen) {
		GridModel model = gen.getModel();

		this.columns = model.getFastAxisPoints();
		this.rows = model.getSlowAxisPoints();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();
		this.xStep = model.getBoundingBox().getFastAxisLength() / columns;
		this.yStep = model.getBoundingBox().getSlowAxisLength() / rows;
		this.minX = model.getBoundingBox().getFastAxisStart() + xStep / 2;
		this.minY = model.getBoundingBox().getSlowAxisStart() + yStep / 2;

		JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

		ScanPointIterator outerLine = lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows, model.isSnake());

		ScanPointIterator innerLine = lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());

        Iterator<?>[] generators = {outerLine, innerLine};

		pyIterator = createSpgCompoundGenerator(generators, gen.getRegions().toArray(),
				new String[] {xName, yName}, new PyObject[] {});
	}

	public GridIterator(RandomOffsetGridGenerator gen) {
		RandomOffsetGridModel model = (RandomOffsetGridModel) gen.getModel();

		this.columns = model.getFastAxisPoints();
		this.rows = model.getSlowAxisPoints();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();
		this.xStep = model.getBoundingBox().getFastAxisLength() / columns;
		this.yStep = model.getBoundingBox().getSlowAxisLength() / rows;
		this.minX = model.getBoundingBox().getFastAxisStart() + xStep / 2;
		this.minY = model.getBoundingBox().getSlowAxisStart() + yStep / 2;

        JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

		ScanPointIterator outerLine = lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows);

		ScanPointIterator innerLine = lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());

        JythonObjectFactory<PyObject> randomOffsetMutatorFactory = ScanPointGeneratorFactory.JRandomOffsetMutatorFactory();

        int seed = model.getSeed();
        PyList axes = new PyList(Arrays.asList(new String[] {yName, xName}));
        double offset = xStep * model.getOffset() / 100;

        PyDictionary maxOffset = new PyDictionary();
        maxOffset.put(yName, offset);
        maxOffset.put(xName, offset);

		PyObject randomOffset = (PyObject) randomOffsetMutatorFactory.createObject(seed, axes, maxOffset);

        Iterator<?>[] generators = {outerLine, innerLine};
        PyObject[] mutators = {randomOffset};

		pyIterator = createSpgCompoundGenerator(generators, gen.getRegions().toArray(),
				new String[] {xName, yName}, mutators);
	}

	public GridIterator(RasterGenerator gen) {
		RasterModel model = gen.getModel();
		this.xStep = model.getFastAxisStep();
		this.yStep = model.getSlowAxisStep();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();
		this.minX = model.getBoundingBox().getFastAxisStart();
		this.minY = model.getBoundingBox().getSlowAxisStart();
		this.columns = (int) Math.floor(model.getBoundingBox().getFastAxisLength() / xStep + 1);
		this.rows = (int) Math.floor(model.getBoundingBox().getSlowAxisLength() / yStep + 1);

		JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

		ScanPointIterator outerLine = lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows);
		ScanPointIterator innerLine = lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());

        Iterator<?>[] generators = {outerLine, innerLine};

		pyIterator = createSpgCompoundGenerator(generators, gen.getRegions().toArray(),
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
