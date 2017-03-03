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
import org.eclipse.scanning.api.points.models.SpiralModel;

class SpiralGenerator extends AbstractGenerator<SpiralModel> {

	SpiralGenerator() {
		setLabel("Fermat Spiral");
		setDescription("Creates a spiral scaled around the center of a bounding box.");
		setIconPath("icons/scanner--spiral.png"); // This icon exists in the rendering bundle 
	}
	
	@Override
	public Iterator<IPosition> iteratorFromValidModel() {
		return new SpiralIterator(this);
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getScale() == 0.0) throw new ModelValidationException("Scale must be non-zero!", model, "scale");
	}
}
