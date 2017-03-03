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
 * A model for a scan along a straight line in two-dimensional space, dividing the line into the number of points given
 * in this model.
 *
 * @author Colin Palmer
 *
 */
public class OneDEqualSpacingModel extends AbstractBoundingLineModel implements IBoundingLineModel {

	private int points;
	
	public int getPoints() {
		return points;
	}
	public void setPoints(int points) {
		int oldValue = this.points;
		this.points = points;
		this.pcs.firePropertyChange("points", oldValue, points);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + points;
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
		OneDEqualSpacingModel other = (OneDEqualSpacingModel) obj;
		if (points != other.points)
			return false;
		return true;
	}
}
