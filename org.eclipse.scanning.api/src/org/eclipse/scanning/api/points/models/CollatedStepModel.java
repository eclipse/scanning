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

import java.util.Arrays;
import java.util.List;

/**
 * A model for a scan along one axis with start and stop positions and a step size.
 */
public class CollatedStepModel extends StepModel {

	private List<String> names;
	
	public CollatedStepModel() {
	}
	public CollatedStepModel(double start, double stop, double step, String... names) {
		super();
		this.names = Arrays.asList(names);
		setStart(start);
		setStop(stop);
		setStep(step);
	}
	public List<String> getNames() {
		return names;
	}
	public String getName() {
		if (super.getName()!=null) return super.getName();
		return names.get(0);
	}

	public void setNames(List<String> name) {
		this.names = name;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((names == null) ? 0 : names.hashCode());
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
		CollatedStepModel other = (CollatedStepModel) obj;
		if (names == null) {
			if (other.names != null)
				return false;
		} else if (!names.equals(other.names))
			return false;
		return true;
	}

}
