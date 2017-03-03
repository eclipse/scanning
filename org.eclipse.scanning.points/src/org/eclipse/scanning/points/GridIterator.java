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

import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.points.models.RandomOffsetGridModel;
import org.eclipse.scanning.api.points.models.RasterModel;
import org.eclipse.scanning.points.ScanPointGeneratorFactory.JythonObjectFactory;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;

class GridIterator extends AbstractScanPointIterator {

	private final AbstractGenerator<? extends IBoundingBoxModel> gen;
	private final int columns;
	private final int rows;
	private final String xName;
	private final String yName;
	private final double minX;
	private final double minY;
	private final double xStep;
	private final double yStep;
	
	private Point currentPoint;
	private PyList pyIterators = new PyList();

	public GridIterator(GridGenerator gen) {
		GridModel model = gen.getModel();
		this.gen = gen;
		
		this.columns = model.getFastAxisPoints();
		this.rows = model.getSlowAxisPoints();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();
		this.xStep = model.getBoundingBox().getFastAxisLength() / columns;
		this.yStep = model.getBoundingBox().getSlowAxisLength() / rows;
		this.minX = model.getBoundingBox().getFastAxisStart() + xStep / 2;
		this.minY = model.getBoundingBox().getSlowAxisStart() + yStep / 2;
		
		JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> outerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows);
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> innerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());
		
		JythonObjectFactory compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();
        
        Object[] generators = {outerLine, innerLine};
        Object[] excluders = {};
        Object[] mutators = {};
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>)  compoundGeneratorFactory.createObject(
				generators, excluders, mutators);
        pyIterator = iterator;
        
        pyIterators.add(outerLine);
        pyIterators.add(innerLine);
	}

	public GridIterator(RandomOffsetGridGenerator gen) {
		this.gen = gen;
		RandomOffsetGridModel model = (RandomOffsetGridModel) gen.getModel();
		
		this.columns = model.getFastAxisPoints();
		this.rows = model.getSlowAxisPoints();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();
		this.xStep = model.getBoundingBox().getFastAxisLength() / columns;
		this.yStep = model.getBoundingBox().getSlowAxisLength() / rows;
		this.minX = model.getBoundingBox().getFastAxisStart() + xStep / 2;
		this.minY = model.getBoundingBox().getSlowAxisStart() + yStep / 2;
		
        JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> outerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows);
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> innerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());
		
        JythonObjectFactory randomOffsetMutatorFactory = ScanPointGeneratorFactory.JRandomOffsetMutatorFactory();
        
        int seed = model.getSeed();
        PyList axes = new PyList(Arrays.asList(new String[] {yName, xName}));
        double offset = getXStep() * model.getOffset() / 100;
        
        PyDictionary maxOffset = new PyDictionary();
        maxOffset.put(yName, offset);
        maxOffset.put(xName, offset);
        
		PyObject randomOffset = (PyObject) randomOffsetMutatorFactory.createObject(seed, axes, maxOffset);
        
        JythonObjectFactory compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();
        
        Object[] generators = {outerLine, innerLine};
        Object[] excluders = {};
        Object[] mutators = {randomOffset};
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>)  compoundGeneratorFactory.createObject(
				generators, excluders, mutators);
        pyIterator = iterator;
        
        pyIterators.add(outerLine);
        pyIterators.add(innerLine);
	}

	public GridIterator(RasterGenerator gen) {
		this.gen = gen;
		RasterModel model = gen.getModel();
		this.xStep = model.getFastAxisStep();
		this.yStep = model.getSlowAxisStep();
		this.xName = model.getFastAxisName();
		this.yName = model.getSlowAxisName();
		this.minX = model.getBoundingBox().getFastAxisStart();
		this.minY = model.getBoundingBox().getSlowAxisStart();
		this.columns = (int) Math.floor(model.getBoundingBox().getFastAxisLength() / xStep + 1);
		this.rows = (int) Math.floor(model.getBoundingBox().getSlowAxisLength() / yStep + 1);
		
		JythonObjectFactory lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> outerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				yName, "mm", minY, minY + (rows - 1) * yStep, rows);
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> innerLine = (Iterator<IPosition>)  lineGeneratorFactory.createObject(
				xName, "mm", minX, minX + (columns - 1) * xStep, columns, model.isSnake());
		
		JythonObjectFactory compoundGeneratorFactory = ScanPointGeneratorFactory.JCompoundGeneratorFactory();
        
        Object[] generators = {outerLine, innerLine};
        Object[] excluders = {};
        Object[] mutators = {};
        
		@SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>)  compoundGeneratorFactory.createObject(
				generators, excluders, mutators);
        pyIterator = iterator;
        
        pyIterators.add(outerLine);
        pyIterators.add(innerLine);
	}

	@Override
	public boolean hasNext() {
		Point point;
		
		while (pyIterator.hasNext()) {
			point = (Point) pyIterator.next();
			
			if (gen.containsPoint(point)) {
				currentPoint = point;
				return true;
			}
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

	public void remove() {
		throw new UnsupportedOperationException("remove");
	}

	public double getXStep() {
		return xStep;
	}
}
