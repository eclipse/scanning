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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.points.AbstractPosition;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.StaticPosition;

class PositionBean<T extends IPosition> {

	private Map<String, Object>  values;
	private Map<String, Integer> indices;
	private int stepIndex;
	private List<Collection<String>> dimensionNames; // Dimension->Names@dimension
	private Class<T> klass;

	public PositionBean() {

	}
	public PositionBean(IPosition pos) {
		this.values    = pos.getValues();
		this.indices   = pos.getIndices();
		this.stepIndex = pos.getStepIndex();
		this.dimensionNames = getDimensionNames(pos);
		@SuppressWarnings("unchecked")
		Class<T> posKlass = (Class<T>) pos.getClass();
		this.klass = posKlass;
	}

	private List<Collection<String>> getDimensionNames(IPosition pos) {
		if (pos instanceof AbstractPosition) {
			return ((AbstractPosition)pos).getDimensionNames();
		}

		return null; // Do not have to support dimension names
	}

	public IPosition toPosition() {
		if (klass == Point.class) {
			return toPoint();
		}

		if (klass == Scalar.class) {
			final String name = values.keySet().iterator().next();
			final Object value = values.get(name);
			final int index = indices.get(name);

			return new Scalar<>(name, index, value);
		}

		if (klass == StaticPosition.class) {
			return new StaticPosition();
		}

		// use map position by default
		final MapPosition pos = new MapPosition(values, indices);
		pos.setStepIndex(stepIndex);
		pos.setDimensionNames(dimensionNames);
		return pos;
	}

	private IPosition toPoint() {
		final String xName;
		final String yName;
		final boolean is2D;
		if (dimensionNames.get(0).size() == 1) {
			yName = dimensionNames.get(0).iterator().next(); // yName is listed first
			xName = dimensionNames.get(1).iterator().next();
			is2D = true;
		} else {
			Iterator<String> namesIter = dimensionNames.get(0).iterator();
			yName = namesIter.next();
			xName = namesIter.next();
			is2D = false;
		}

		final int xIndex = indices.get(xName);
		final double xPosition = (Double) values.get(xName);
		final int yIndex = indices.get(yName);
		final double yPosition = (Double) values.get(yName);

		return new Point(xName, xIndex, xPosition, yName, yIndex, yPosition, stepIndex, is2D);
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

	public Class<T> getPositionClass() {
		return klass;
	}

	public void setPositionClass(Class<T> klass) {
		this.klass = klass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dimensionNames == null) ? 0 : dimensionNames.hashCode());
		result = prime * result + ((indices == null) ? 0 : indices.hashCode());
		result = prime * result + stepIndex;
		result = prime * result + ((klass == null) ? 0 : klass.hashCode());
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
		PositionBean<?> other = (PositionBean<?>) obj;
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
		if (klass == null) {
			if (other.klass != null)
				return false;
		} else if (!klass.equals(other.klass))
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

}
