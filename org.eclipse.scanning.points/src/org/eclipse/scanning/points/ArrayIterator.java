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

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.points.ScanPointGeneratorFactory.JythonObjectFactory;
import org.python.core.PyList;

class ArrayIterator extends AbstractScanPointIterator {

	private ArrayModel model;
	int index = 0;

	public ArrayIterator(ArrayGenerator gen) {
		this.model= gen.getModel();
		
        JythonObjectFactory arrayGeneratorFactory = ScanPointGeneratorFactory.JArrayGeneratorFactory();

        double[] points = model.getPositions();        
        PyList names =  new PyList(Arrays.asList(new String[] { model.getName()}));
        
        @SuppressWarnings("unchecked")
		Iterator<IPosition> iterator = (Iterator<IPosition>) arrayGeneratorFactory.createObject(
				names, "mm", points);
        pyIterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return pyIterator.hasNext();
	}

	@Override
	public IPosition next() {
		return pyIterator.next();
	}

	public void remove() {
		throw new UnsupportedOperationException("remove");
	}
}
