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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;
import org.eclipse.scanning.api.annotation.ui.TypeDescriptor;

/**
 * A model consisting of multiple {@link StepModel}s to be iterated over sequentially.
 * 
 * @author Matthew Dickie
 */
@TypeDescriptor(editor="org.eclipse.scanning.device.ui.composites.MultiStepComposite")
public class MultiStepModel extends AbstractPointsModel {

	private List<StepModel> stepModels;
	
	@FieldDescriptor(visible=true, label="The scannable name over which the multiple steps will run.")
	private String name;
	
	public MultiStepModel() {
		stepModels = new ArrayList<>(4);
		setName("energy");
	}
	
	public MultiStepModel(String name, double start, double stop, double step) {
		super();
		setName(name);
		stepModels = new ArrayList<>(4);
		stepModels.add(new StepModel(name, start, stop, step));
	}
	
	/**
	 * Must implement clear() method on beans being used with BeanUI.
	 */
	public void clear() {
		List<StepModel> oldModels = new ArrayList<StepModel>(stepModels);
		stepModels.clear();
		firePropertyChange("stepModels", oldModels, stepModels);
	}

	public void addRange(double start, double stop, double step) {
		addRange(start, stop, step, 0d);
	}
	public void addRange(double start, double stop, double step, double exposure) {
		List<StepModel> oldModels = new ArrayList<StepModel>(stepModels);
		stepModels.add(new StepModel(getName(), start, stop, step, exposure));
		firePropertyChange("stepModels", oldModels, stepModels);
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

	public void setStepModels(List<StepModel> stepModels) {
		List<StepModel> oldModels = stepModels;
		this.stepModels = stepModels;
		firePropertyChange("stepModels", oldModels, stepModels);
	}

	/**
	 * This method is accessed by reflection, it helps out BeanUI
	 * @param smodel
	 */
	public void addStepModel(StepModel smodel) {
		stepModels.add(smodel);
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
			if (stepModel.getExposureTime()>0) {
				sb.append(" exp=");
				sb.append(stepModel.getExposureTime());
			}
		}
		
		sb.append(")]");
		
		return sb.toString();
	}
	
	
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName,
				listener);
	}

	protected void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}

	public void clearListeners() {
		PropertyChangeListener[] ls = propertyChangeSupport.getPropertyChangeListeners();
		for (int i = 0; i < ls.length; i++) propertyChangeSupport.removePropertyChangeListener(ls[i]);
	}

}
