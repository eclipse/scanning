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
import org.eclipse.scanning.api.points.models.LissajousModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyDictionary;
import org.python.core.PyList;

public class LissajousGenerator extends AbstractGenerator<LissajousModel> {

	public LissajousGenerator() {
		setLabel("Lissajous Curve");
		setDescription("Creates a lissajous curve inside a bounding box.");
		setIconPath("icons/scanner--lissajous.png"); // This icon exists in the rendering bundle
	}

	@Override
	public ScanPointIterator iteratorFromValidModel() {
		final String xName = model.getFastAxisName();
		final String yName = model.getSlowAxisName();
		final double width = model.getBoundingBox().getFastAxisLength();
		final double height = model.getBoundingBox().getSlowAxisLength();

        final JythonObjectFactory<ScanPointIterator> lissajousGeneratorFactory = ScanPointGeneratorFactory.JLissajousGeneratorFactory();

        final PyDictionary box = new PyDictionary();
        box.put("width", width);
        box.put("height", height);
        box.put("centre", new double[] {model.getBoundingBox().getFastAxisStart() + width / 2,
									model.getBoundingBox().getSlowAxisStart() + height / 2});

        final PyList names =  new PyList(Arrays.asList(xName, yName));
        final PyList units = new PyList(Arrays.asList("mm", "mm"));
        final int numLobes = (int) (model.getA() / model.getB());
        final int numPoints = model.getPoints();

        final ScanPointIterator lissajous = lissajousGeneratorFactory.createObject(
				names, units, box, numLobes, numPoints);
		final ScanPointIterator pyIterator = CompoundSpgIteratorFactory.createSpgCompoundGenerator(new Iterator[] {lissajous}, getRegions().toArray(),
				new String[] {xName, yName}, EMPTY_PY_ARRAY, -1, model.isContinuous());
		return new SpgIterator(pyIterator);
	}

	@Override
	protected void validateModel() {
		if (model.getPoints() < 1) throw new ModelValidationException("Must have one or more points in model!", model, "points");
		if (model.getFastAxisName()==null) throw new ModelValidationException("The model must have a fast axis!\nIt is the motor name used for this axis.", model, "fastAxisName");
		if (model.getSlowAxisName()==null) throw new ModelValidationException("The model must have a slow axis!\nIt is the motor name used for this axis.", model, "slowAxisName");
	}

}
