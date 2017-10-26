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

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.StepModel;

/**
 * An iterator along points on one axis with a start position, a stop position and an step size.
 *
 * TODO: DAQ-888 setting the exposure time should be done in the jython level, otherwise
 * it won't work for malcolm scans.
 */
class StepIterator extends SpgIterator {

	private final StepModel model;

	public StepIterator(StepModel model, ScanPointIterator pyIterator) {
		super(pyIterator);
		this.model = model;
	}

	protected StepModel getModel() {
		return model;
	}

	@Override
	public IPosition next() {
		final IPosition next = super.next();

		// set the exposure time and index
		if (next != null) {
			next.setExposureTime(model.getExposureTime()); // Usually 0
		}

		return next;
	}

}
