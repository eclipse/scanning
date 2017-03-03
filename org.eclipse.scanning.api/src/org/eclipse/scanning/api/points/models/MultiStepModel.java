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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A model consisting of multiple {@link StepModel}s to be iterated over sequentially.
 * 
 * @author Matthew Dickie
 */
public class MultiStepModel extends AbstractPointsModel {

	private List<StepModel> stepModels;
	
	public MultiStepModel() {
		stepModels = new ArrayList<>(4);
	}
	
	public MultiStepModel(String name, double start, double stop, double step) {
		super();
		setName(name);
		stepModels = new ArrayList<>(4);
		stepModels.add(new StepModel(name, start, stop, step));
	}
	
	public void addRange(double start, double stop, double step) {
		stepModels.add(new StepModel(getName(), start, stop, step));
	}
	
	public void addRange(StepModel stepModel) {
		if (!getName().equals(stepModel.getName())) {
			throw new IllegalArgumentException(MessageFormat.format(
					"Child step model must have the same name as the MultiStepModel. Expected ''{0}'', was ''{1}''", getName(), stepModel.getName()));
		}
		
		stepModels.add(stepModel);
	}
	
	public List<StepModel> getStepModels() {
		return stepModels;
	}
	
	public void stepStepModels(List<StepModel> stepModels) {
		this.stepModels = stepModels;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((stepModels == null) ? 0 : stepModels.hashCode());
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
		MultiStepModel other = (MultiStepModel) obj;
		if (stepModels == null) {
			if (other.stepModels != null)
				return false;
		} else if (!stepModels.equals(other.stepModels))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MultiStepModel [name=");
		sb.append(getName());
		sb.append(", stepModels=(");
		for (StepModel stepModel : stepModels) {
			sb.append("start=");
			sb.append(stepModel.getStart());
			sb.append(", stop=");
			sb.append(stepModel.getStop());
			sb.append(", step=");
			sb.append(stepModel.getStep());
			sb.append("; ");
		}
		
		sb.append(")]");
		
		return sb.toString();
	}
	
}
