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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.scanning.api.scan.ScannableValueTextProvider;


/**
 * This is a special Position class to allow multiple values to be returned from a scannable. It is a temporary
 * creation until we understand why can't use a standard List or Map for a DeviceValue in a DeviceRequest. 
 */
public class DeviceValueMultiPosition implements ScannableValueTextProvider {
	private Map<String, Double> values = new LinkedHashMap<>();

	public Double get(String parameter) {
		return values.get(parameter);
	}

	public void put(String parameter, Double value) {
		values.put(parameter, value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	// implements ScannableValueTextProvider

	@Override
	public String getText() {
		return values.toString();
	}
}
