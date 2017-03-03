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
package org.eclipse.scanning.api.points.models;

import org.eclipse.scanning.api.annotation.UiTooltip;
import org.eclipse.scanning.api.annotation.Units;

public class RandomOffsetGridModel extends GridModel {

	
	/**
	 * The maximum allowed offset, as a percentage of fast axis step size
	 */
	private double offset;
	/**
	 * Seed to initialise random number generator with
	 */
	private int seed;

	public RandomOffsetGridModel() {
		setName("Random Offset Grid");
	}

	public RandomOffsetGridModel(String f, String s) {
		super(f, s);
	}

	@Units("%")
	@UiTooltip("Standard deviation to use for a 2D Gaussian random offset, as a percentage of the X step size")
	public double getOffset() {
		return offset;
	}
	public void setOffset(double newValue) {
		double oldValue = this.offset;
		this.offset = newValue;
		this.pcs.firePropertyChange("offset", oldValue, newValue);
	}
	@UiTooltip("Seed to initialise random number generator with")
	public int getSeed() {
		return seed;
	}
	public void setSeed(int newValue) {
		double oldValue = this.seed;
		this.seed = newValue;
		this.pcs.firePropertyChange("seed", oldValue, newValue);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(offset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RandomOffsetGridModel other = (RandomOffsetGridModel) obj;
		if (Double.doubleToLongBits(offset) != Double.doubleToLongBits(other.offset))
			return false;
		return true;
	}
}
