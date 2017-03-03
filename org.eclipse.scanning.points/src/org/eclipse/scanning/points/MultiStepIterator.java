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
import java.util.NoSuchElementException;

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.StepModel;

/**
 * An iterator over multiple step ranges. Acts essentially as a sequence of
 * step iterators chained together. In the special case where one step iterator begins
 * with the last value of the previous iterator it is not repeated.
 * 
 * @author Matthew Dickie
 */
public class MultiStepIterator implements Iterator<IPosition> {
	
	private int index = 0;
	private final MultiStepModel model;
	private final  Iterator<StepModel> stepModelIter;
	private Iterator<IPosition> currentIter;
	private boolean newStepModel = false;
	private double lastValue = 0;
	
	public MultiStepIterator(MultiStepModel model) {
		this.model = model;
		stepModelIter = model.getStepModels().iterator();
		currentIter = null;
	}

	@Override
	public boolean hasNext() {
		if (currentIter != null && currentIter.hasNext()) {
			return true;
		}
		
		if (stepModelIter.hasNext()) {
			// move on to the next step model
			if (currentIter != null) {
				newStepModel = true;
			}
			currentIter = new StepIterator(stepModelIter.next());
			return currentIter.hasNext();
		}

		// reached the end of all step models
		return false;
	}

	@Override
	public IPosition next() {
		if (hasNext()) {
			IPosition pos = currentIter.next();
			double value = pos.getValue(model.getName());
			if (newStepModel) {
				if (value == lastValue) {
					pos = currentIter.next();
					value = pos.getValue(model.getName());
				}
				newStepModel = false;
			}
			
			lastValue = value;
			return new Scalar<>(model.getName(), index++, value);
		}
		
		throw new NoSuchElementException();
	}

}
