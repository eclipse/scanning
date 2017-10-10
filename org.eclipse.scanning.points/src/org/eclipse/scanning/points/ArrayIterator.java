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

import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

class ArrayIterator extends AbstractScanPointIterator {

	private ArrayModel model;

	public ArrayIterator(ArrayGenerator gen) {
		this.model= gen.getModel();

        JythonObjectFactory<ScanPointIterator> arrayGeneratorFactory = ScanPointGeneratorFactory.JArrayGeneratorFactory();

        double[] points = model.getPositions();

		ScanPointIterator iterator = arrayGeneratorFactory.createObject(
				model.getName(), "mm", points);
        pyIterator = iterator;
	}

}
