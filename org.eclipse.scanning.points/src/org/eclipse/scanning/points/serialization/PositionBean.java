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
package org.eclipse.scanning.points.serialization;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;

class PositionBean {

	private Map<String, Object>  values;
	private Map<String, Integer> indices;
	private int stepIndex;
	private List<Collection<String>> dimensionNames; // Dimension->Names@dimension
	
	public PositionBean() {
		
	}
	public PositionBean(IPosition pos) {
		this.values    = pos.getValues();
		this.indices   = pos.getIndices();
		this.stepIndex = pos.getStepIndex();
		this.dimensionNames = getDimensionNames(pos);
	}
	
	private List<Collection<String>> getDimensionNames(IPosition pos) {
		if (pos instanceof AbstractPosition) return ((AbstractPosition)pos).getDimensionNames();
		return null; // Do not have to support dimension names
	}
	public IPosition toPosition() {
		MapPosition pos = new MapPosition(values, indices);
		pos.setStepIndex(stepIndex);
		pos.setDimensionNames(dimensionNames);
		return pos;
	}
	public Map<String, Object> getValues() {
		return values;
	}
	public void setValues(Map<String, Object> values) {
		this.values = values;
	}
	public Map<String, Integer> getIndices() {
		return indices;
	}
	public void setIndices(Map<String, Integer> indices) {
		this.indices = indices;
	}
	public int getStepIndex() {
		return stepIndex;
	}
	public void setStepIndex(int stepIndex) {
		this.stepIndex = stepIndex;
	}
	public List<Collection<String>> getDimensionNames() {
		return dimensionNames;
	}
	public void setDimensionNames(List<Collection<String>> dimensionNames) {
		this.dimensionNames = dimensionNames;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dimensionNames == null) ? 0 : dimensionNames.hashCode());
		result = prime * result + ((indices == null) ? 0 : indices.hashCode());
		result = prime * result + stepIndex;
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
		PositionBean other = (PositionBean) obj;
		if (dimensionNames == null) {
			if (other.dimensionNames != null)
				return false;
		} else if (!dimensionNames.equals(other.dimensionNames))
			return false;
		if (indices == null) {
			if (other.indices != null)
				return false;
		} else if (!indices.equals(other.indices))
			return false;
		if (stepIndex != other.stepIndex)
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

}
