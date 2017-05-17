/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mark Booth - initial API and implementation and/or initial documentation
 *******************************************************************************/
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

	public Map<String, Double> getValues() {
		return values;
	}

	public void setValues(Map<String, Double> values) {
		this.values = values;
	}

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
