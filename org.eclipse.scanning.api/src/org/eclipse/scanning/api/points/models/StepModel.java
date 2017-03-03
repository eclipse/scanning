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

import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

/**
 * A model for a scan along one axis with start and stop positions and a step size.
 */
public class StepModel extends AbstractPointsModel {

	@FieldDescriptor(label="Device", device=DeviceType.SCANNABLE, fieldPosition=0)
	private String name;
	
	@FieldDescriptor(label="Start", scannable="name", hint="This is the start position for the scan", fieldPosition=1) // The scannable lookup gets the units
	private double start;
	
	@FieldDescriptor(label="Stop", scannable="name", hint="This is the stop position for the scan", fieldPosition=2) // The scannable lookup gets the units
	private double stop;
	
	@FieldDescriptor(label="Step", scannable="name", hint="This is the step during the scan", fieldPosition=3) // The scannable lookup gets the units
	private double step;
	
	public StepModel() {
	
	}
	
	public StepModel(String name, double start, double stop, double step) {
		super();
		this.name = name;
		this.start = start;
		this.stop = stop;
		this.step = step;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getStart() {
		return start;
	}
	public void setStart(double start) {
		this.start = start;
	}
	public double getStop() {
		return stop;
	}
	public void setStop(double stop) {
		this.stop = stop;
	}
	public double getStep() {
		return step;
	}
	public void setStep(double step) {
		this.step = step;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		long temp;
		temp = Double.doubleToLongBits(start);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(step);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(stop);
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
		StepModel other = (StepModel) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (Double.doubleToLongBits(start) != Double
				.doubleToLongBits(other.start))
			return false;
		if (Double.doubleToLongBits(step) != Double
				.doubleToLongBits(other.step))
			return false;
		if (Double.doubleToLongBits(stop) != Double
				.doubleToLongBits(other.stop))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StepModel [name=" + name + ", start=" + start + ", stop=" + stop + ", step=" + step + "]";
	}
}
