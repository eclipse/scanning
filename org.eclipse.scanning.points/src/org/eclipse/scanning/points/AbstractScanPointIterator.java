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

import java.util.Iterator;

import org.eclipse.scanning.api.points.IPosition;
import org.python.core.PyDictionary;

public abstract class AbstractScanPointIterator implements SerializableIterator<IPosition>{

	protected Iterator<IPosition> pyIterator;

	public Iterator<IPosition> getPyIterator() {
		return pyIterator;
	}

	public void setPyIterator(Iterator<IPosition> pyIterator) {
		this.pyIterator = pyIterator;
	}

	public PyDictionary toDict() {
		return null;
	}
	
	public int size() {
		return 0;
	}
}
