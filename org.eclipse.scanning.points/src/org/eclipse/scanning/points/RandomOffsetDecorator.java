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
import java.util.Random;

import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;

/**
 * A decorator which adds a random Gaussian offset to each axis of an IPosition. All axes in the IPosition must have
 * numeric position values and the same standard deviation is used for the random Gaussian offset for each axis.
 *
 * @author Colin Palmer
 *
 */
public class RandomOffsetDecorator implements Iterator<IPosition> {

	private Iterator<IPosition> decoratee;
	private double stdDev;
	private Random random;

	public RandomOffsetDecorator(Iterator<IPosition> toDecorate, double offsetStandardDeviation) {
		this.decoratee = toDecorate;
		this.stdDev = offsetStandardDeviation;
		this.random = new Random();
	}

	/**
	 * Set the random seed used by the decorator's random number generator. Only
	 * intended for use in tests to ensure reproducibility.
	 *
	 * @param randomSeed
	 */
	public void setRandomSeed(long randomSeed) {
		random.setSeed(randomSeed);
	}

	@Override
	public boolean hasNext() {
		return decoratee.hasNext();
	}

	@Override
	public IPosition next() {
		IPosition position = decoratee.next();
		MapPosition offsetPosition = new MapPosition();
		offsetPosition.putAllIndices(position);
		for (String axis : position.getNames()) {
			Object value = position.get(axis);
			if (value instanceof Number) {
				double coordinate = ((Number) value).doubleValue();
				offsetPosition.put(axis, offset(coordinate));
			} else {
				throw new IllegalStateException("Cannot apply a random offset to a non-numeric position");
			}
		}
		offsetPosition.setDimensionNames(((AbstractPosition)position).getDimensionNames());
		return offsetPosition;
	}

	private double offset(double coordinate) {
		double offset = random.nextGaussian() * stdDev;
		return coordinate + offset;
	}
}
