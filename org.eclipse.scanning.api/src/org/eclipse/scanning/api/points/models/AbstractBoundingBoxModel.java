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
import org.eclipse.scanning.api.annotation.ui.EditType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;


/**
 * A model for a scan within a rectangular box in two-dimensional space.
 * <p>
 * This abstract class defines the box size and the names of the two axes.
 *
 * @author Colin Palmer
 *
 */
public abstract class AbstractBoundingBoxModel extends AbstractMapModel implements IBoundingBoxModel {

	@FieldDescriptor(edit=EditType.COMPOUND, hint="The bounding box is automatically calculated from the scan regions shown in the main plot.") // We edit this with a popup.
	private BoundingBox boundingBox;

	protected AbstractBoundingBoxModel() {
		super();
	}
	
	protected AbstractBoundingBoxModel(String fastName, String slowName, BoundingBox box) {
		super(fastName, slowName);
		this.boundingBox  = box;
	}

	@Override
	@UiHidden
	public BoundingBox getBoundingBox() {
		return boundingBox;
	}
	
	@Override
	public void setBoundingBox(BoundingBox newValue) {
		BoundingBox oldValue = this.boundingBox;
		this.boundingBox = newValue;
		boundingBox.setFastAxisName(getFastAxisName());
		boundingBox.setSlowAxisName(getSlowAxisName());
		this.pcs.firePropertyChange("boundingBox", oldValue, newValue);
	}

	public void setFastAxisName(String newValue) {
		if (boundingBox!=null) boundingBox.setFastAxisName(getFastAxisName());
		super.setFastAxisName(newValue);
	}
	
	public void setSlowAxisName(String newValue) {
		if (boundingBox!=null) boundingBox.setSlowAxisName(getSlowAxisName());
		super.setSlowAxisName(newValue);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((boundingBox == null) ? 0 : boundingBox.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		AbstractBoundingBoxModel other = (AbstractBoundingBoxModel) obj;
		if (boundingBox == null) {
			if (other.boundingBox != null)
				return false;
		} else if (!boundingBox.equals(other.boundingBox))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+" [boundingBox=" + boundingBox + ", fastAxisName=" + getFastAxisName()
				+ ", slowAxisName=" + getSlowAxisName() + "]";
	}

}
