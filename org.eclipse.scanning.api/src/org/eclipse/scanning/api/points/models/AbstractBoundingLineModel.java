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

import org.eclipse.scanning.api.annotation.UiHidden;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

public class AbstractBoundingLineModel extends AbstractMapModel implements IBoundingLineModel {
	
	@FieldDescriptor(editable=false) // We edit this with a popup.
	private BoundingLine boundingLine;
	
	@Override
	@UiHidden
	public BoundingLine getBoundingLine() {
		return boundingLine;
	}
	
	@Override
	public void setBoundingLine(BoundingLine boundingLine) {
		BoundingLine oldValue = this.boundingLine;
		this.boundingLine = boundingLine;
		this.pcs.firePropertyChange("boundingLine", oldValue, boundingLine);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((boundingLine == null) ? 0 : boundingLine.hashCode());
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
		AbstractBoundingLineModel other = (AbstractBoundingLineModel) obj;
		if (boundingLine == null) {
			if (other.boundingLine != null)
				return false;
		} else if (!boundingLine.equals(other.boundingLine))
			return false;
		return true;
	}

}
