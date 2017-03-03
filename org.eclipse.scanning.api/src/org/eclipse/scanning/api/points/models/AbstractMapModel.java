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

import org.eclipse.scanning.api.annotation.UiHidden;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

public class AbstractMapModel extends AbstractPointsModel implements IMapPathModel {
	
	@FieldDescriptor(label="Fast Axis", device=DeviceType.SCANNABLE, hint="The name of the scannable in the fast direction, for instance 'stage_x'.") // TODO Right?
	private String      fastAxisName = "stage_x";

	@FieldDescriptor(label="Slow Axis", device=DeviceType.SCANNABLE, hint="The name of the scannable in the fast direction, for instance 'stage_y'.")  // TODO Right?
	private String      slowAxisName = "stage_y";
	
	public AbstractMapModel() {
		super();
	}
	
	public AbstractMapModel(String fastName, String slowName) {
		this.fastAxisName = fastName;
		this.slowAxisName = slowName;
	}

	@UiHidden
	public String getFastAxisName() {
		return fastAxisName;
	}
	
	public void setFastAxisName(String newValue) {
		String oldValue = this.fastAxisName;
		this.fastAxisName = newValue;
		this.pcs.firePropertyChange("fastAxisName", oldValue, newValue);
	}
	
	@UiHidden
	public String getSlowAxisName() {
		return slowAxisName;
	}
	
	public void setSlowAxisName(String newValue) {
		String oldValue = this.slowAxisName;
		this.slowAxisName = newValue;
		this.pcs.firePropertyChange("slowAxisName", oldValue, newValue);
	}
	
	@UiHidden
	@Override
	public List<String> getScannableNames() {
		return Arrays.asList(getFastAxisName(), getSlowAxisName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fastAxisName == null) ? 0 : fastAxisName.hashCode());
		result = prime * result + ((slowAxisName == null) ? 0 : slowAxisName.hashCode());
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
		AbstractMapModel other = (AbstractMapModel) obj;
		if (fastAxisName == null) {
			if (other.fastAxisName != null)
				return false;
		} else if (!fastAxisName.equals(other.fastAxisName))
			return false;
		if (slowAxisName == null) {
			if (other.slowAxisName != null)
				return false;
		} else if (!slowAxisName.equals(other.slowAxisName))
			return false;
		return true;
	}

}
