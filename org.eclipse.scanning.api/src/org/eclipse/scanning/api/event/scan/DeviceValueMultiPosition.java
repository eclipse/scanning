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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is a special Position class to allow both x and y gap values to be returned from a
 * NexusSlitsWrapper scannable. It is a temporary creation until we understand why can't
 * use a List or a Map for a DeviceValue in a DeviceRequest. 
 *
 */
public class DeviceValueMultiPosition {
	@JsonProperty("values")
	private List<Double> values = new ArrayList<Double>() {{
		add(0.0);
		add(0.0);
	}};

	public double getX_gap() {
		return values.get(0);
	}

	public void setX_gap(double x_gap) {
		values.set(0, x_gap);
	}

	public double getY_gap() {
		return values.get(1);
	}

	public void setY_gap(double y_gap) {
		values.set(1, y_gap);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(getX_gap());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(getY_gap());
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
		DeviceValueMultiPosition other = (DeviceValueMultiPosition) obj;
		if (Double.doubleToLongBits(getX_gap()) != Double.doubleToLongBits(other.getX_gap()))
			return false;
		if (Double.doubleToLongBits(getY_gap()) != Double.doubleToLongBits(other.getY_gap()))
			return false;
		return true;
	}
}
