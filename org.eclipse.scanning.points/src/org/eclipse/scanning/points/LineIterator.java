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

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.function.Function;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.BoundingLine;
import org.eclipse.scanning.api.points.models.CollatedStepModel;
import org.eclipse.scanning.api.points.models.OneDEqualSpacingModel;
import org.eclipse.scanning.api.points.models.OneDStepModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyList;

class LineIterator extends AbstractScanPointIterator {

	private final IPointGenerator<?> gen;

	public LineIterator(StepGenerator gen) {
		this.gen = gen;
		final StepModel model = gen.getModel();

        JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

        String name   = model.getName();
        double start  = model.getStart();
        double stop   = model.getStop();
        int numPoints = model.size();

		ScanPointIterator iterator = lineGeneratorFactory.createObject(name, "mm", start, stop, numPoints);
		pyIterator = iterator;
	}

	public LineIterator(OneDEqualSpacingGenerator gen) {
		this.gen = gen;
		OneDEqualSpacingModel model= gen.getModel();
		BoundingLine line = model.getBoundingLine();

		JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

		int numPoints = model.getPoints();
		double step = line.getLength() / numPoints;
		double xStep = step * Math.cos(line.getAngle());
		double yStep = step * Math.sin(line.getAngle());

		PyList names =  new PyList(Arrays.asList(model.getFastAxisName(), model.getSlowAxisName()));
		PyList units = new PyList(Arrays.asList("mm", "mm"));
		double[] start = {line.getxStart() + xStep/2, line.getyStart() + yStep/2};
		double[] stop = {line.getxStart() + xStep * (numPoints - 0.5), line.getyStart() + yStep * (numPoints - 0.5)};

		ScanPointIterator iterator = lineGeneratorFactory.createObject(
				names, units, start, stop, numPoints);
		pyIterator = iterator;
	}

	public LineIterator(OneDStepGenerator gen) {
		this.gen = gen;
		OneDStepModel model= gen.getModel();
		BoundingLine line = model.getBoundingLine();

        JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator2DFactory();

		int numPoints = (int) Math.floor(line.getLength() / model.getStep()) + 1;
        double xStep = model.getStep() * Math.cos(line.getAngle());
        double yStep = model.getStep() * Math.sin(line.getAngle());

		PyList names =  new PyList(Arrays.asList(model.getFastAxisName(), model.getSlowAxisName()));
		PyList units = new PyList(Arrays.asList("mm", "mm"));
		double[] start = {line.getxStart(), line.getyStart()};
        double[] stop = {line.getxStart() + xStep * numPoints, line.getyStart() + yStep * numPoints};

		ScanPointIterator iterator = lineGeneratorFactory.createObject(
				names, units, start, stop, numPoints);
		pyIterator = iterator;
	}

	@Override
	public IPosition next() {
		final IPosition next;
		if (gen.getModel() instanceof CollatedStepModel) {
			// special case: create a map from each scannable name to the actual value
			@SuppressWarnings("unchecked")
			final Scalar<Double> point = (Scalar<Double>) pyIterator.next();
			double value = point.getValue();
			next = new MapPosition(((CollatedStepModel) gen.getModel()).getNames().stream().collect(toMap(
					Function.identity(), name -> value)));
			next.setStepIndex(point.getStepIndex());
		} else {
			// standard case: just use the next value
			next = pyIterator.next();
		}

		// set the exposure time and index
		if (next != null && gen.getModel() instanceof StepModel) {
			next.setExposureTime(((StepModel) gen.getModel()).getExposureTime()); // Usually 0
		}

		return next;
	}

}
