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

/**
 * A model for a scan along a straight line in two-dimensional space, starting at the beginning of the line and moving
 * in steps of the size given in this model.
 *
 * @author Colin Palmer
 *
 */
public class OneDStepModel extends AbstractBoundingLineModel implements IBoundingLineModel {
    

	private double step;

	public double getStep() {
		return step;
	}
	public void setStep(double step) {
		double oldValue = this.step;
		this.step = step;
		this.pcs.firePropertyChange("step", oldValue, step);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(step);
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
		OneDStepModel other = (OneDStepModel) obj;
		if (Double.doubleToLongBits(step) != Double.doubleToLongBits(other.step))
			return false;
		return true;
	}
}
