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
import org.eclipse.scanning.api.points.models.RasterModel;

class RasterGenerator extends AbstractGenerator<RasterModel> {
	
	RasterGenerator() {
		setLabel("Raster");
		setDescription("Creates a raster scan (a scan of x and y).\nThe scan supports bidirectional or 'snake' mode.");
		setIconPath("icons/scanner--raster.png"); // This icon exists in the rendering bundle 
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getFastAxisStep() == 0) throw new ModelValidationException("Model fast axis step size must be nonzero!", model, "fastAxisStep");
		if (model.getSlowAxisStep() == 0) throw new ModelValidationException("Model slow axis step size must be nonzero!", model, "slowAxisStep");

		// Technically the following two throws are not required
		// (The generator could simply produce an empty list.)
		// but we throw errors to avoid potential confusion.
		// Plus, this is consistent with the StepGenerator behaviour.
		if (model.getFastAxisStep()/model.getBoundingBox().getFastAxisLength() < 0)
			throw new ModelValidationException("Model fast axis step is directed so as to produce no points!", model, "fastAxisStep");
		if (model.getSlowAxisStep()/model.getBoundingBox().getSlowAxisLength() < 0)
			throw new ModelValidationException("Model slow axis step is directed so as to produce no points!", model, "slowAxisStep");
	}

	public Iterator<IPosition> iteratorFromValidModel() {
		return new GridIterator(this);
	}

}
