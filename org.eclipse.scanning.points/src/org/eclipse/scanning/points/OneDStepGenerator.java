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
import org.eclipse.scanning.api.points.models.OneDStepModel;

class OneDStepGenerator extends AbstractGenerator<OneDStepModel> {

	OneDStepGenerator() {
		setLabel("Point");
		setDescription("Creates a point to scan.");
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getStep() <= 0) throw new ModelValidationException("Model step size must be positive!", model, "step");
	}

	@Override
	protected Iterator<IPosition> iteratorFromValidModel() {
		return new LineIterator(this);
	}
}
