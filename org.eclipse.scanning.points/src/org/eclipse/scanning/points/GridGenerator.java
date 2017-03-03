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
package org.eclipse.scanning.points;

import java.util.Iterator;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.GridModel;

class GridGenerator extends AbstractGenerator<GridModel> {
	
	GridGenerator() {
		setLabel("Grid");
		setDescription("Creates a grid scan (a scan of x and y).\nThe scan supports bidirectional or 'snake' mode.");
		setIconPath("icons/scanner--grid.png"); // This icon exists in the rendering bundle 
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getSlowAxisPoints() <= 0) throw new ModelValidationException("Model must have a positive number of slow axis points!", model, "slowAxisPoints");
		if (model.getFastAxisPoints() <= 0) throw new ModelValidationException("Model must have a positive number of fast axis points!", model, "fastAxisPoints");
		if (model.getFastAxisName()==null) throw new ModelValidationException("The model must have a fast axis!\nIt is the motor name used for this axis.", model, "fastAxisName");
		if (model.getSlowAxisName()==null) throw new ModelValidationException("The model must have a slow axis!\nIt is the motor name used for this axis.", model, "slowAxisName");
	}

	@Override
	public Iterator<IPosition> iteratorFromValidModel() {
		return new GridIterator(this);
	}

}
