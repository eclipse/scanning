/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.api.event.scan;

import java.util.Arrays;
import java.util.List;

/**
 * This is a special Position class to allow both x and y gap values to be returned from a
 * NexusSlitsWrapper scannable. It is a temporary creation until we understand why can't
 * use a List or a Map for a DeviceValue in a DeviceRequest. 
 *
 */
public class NexusSlitsPosition {
	private double x_gap;
	private double y_gap;

	public double getX_gap() {
		return x_gap;
	}

	public void setX_gap(double x_gap) {
		this.x_gap = x_gap;
	}

	public double getY_gap() {
		return y_gap;
	}

	public void setY_gap(double y_gap) {
		this.y_gap = y_gap;
	}

	public List<Double> getPositionList() {
		return Arrays.asList(getX_gap(), getY_gap());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x_gap);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y_gap);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NexusSlitsPosition other = (NexusSlitsPosition) obj;
		if (Double.doubleToLongBits(x_gap) != Double.doubleToLongBits(other.x_gap))
			return false;
		if (Double.doubleToLongBits(y_gap) != Double.doubleToLongBits(other.y_gap))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NexusSlitsPosition [x_gap=" + x_gap + ", y_gap=" + y_gap + "]";
	}
}
