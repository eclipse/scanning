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
import org.eclipse.scanning.api.points.models.LissajousModel;
import org.eclipse.scanning.jython.JythonObjectFactory;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;

class LissajousIterator extends AbstractScanPointIterator {

	private LissajousModel model;

	public LissajousIterator(LissajousGenerator gen) {
		this.model     = gen.getModel();

		String xName = model.getFastAxisName();
		String yName = model.getSlowAxisName();
		double width = model.getBoundingBox().getFastAxisLength();
		double height = model.getBoundingBox().getSlowAxisLength();

        JythonObjectFactory<ScanPointIterator> lissajousGeneratorFactory = ScanPointGeneratorFactory.JLissajousGeneratorFactory();

        PyDictionary box = new PyDictionary();
        box.put("width", width);
        box.put("height", height);
        box.put("centre", new double[] {model.getBoundingBox().getFastAxisStart() + width / 2,
									model.getBoundingBox().getSlowAxisStart() + height / 2});

        PyList names =  new PyList(Arrays.asList(xName, yName));
        PyList units = new PyList(Arrays.asList("mm", "mm"));
        int numLobes = (int) (model.getA() / model.getB());
        int numPoints = model.getPoints();

        ScanPointIterator lissajous = lissajousGeneratorFactory.createObject(
				names, units, box, numLobes, numPoints);
		pyIterator = createSpgCompoundGenerator(new Iterator[] {lissajous}, gen.getRegions().toArray(),
				new String[] {xName, yName}, new PyObject[] {});
	}
}
