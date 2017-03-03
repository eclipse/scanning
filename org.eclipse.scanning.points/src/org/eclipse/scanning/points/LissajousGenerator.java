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
import org.eclipse.scanning.api.points.models.LissajousModel;

public class LissajousGenerator extends AbstractGenerator<LissajousModel> {

	public LissajousGenerator() {
		setLabel("Lissajous Curve");
		setDescription("Creates a lissajous curve inside a bounding box.");
		setIconPath("icons/scanner--lissajous.png"); // This icon exists in the rendering bundle 
	}

	@Override
	public Iterator<IPosition> iteratorFromValidModel() {
		return new LissajousIterator(this);
	}

	@Override
	protected void validateModel() {
		if (model.getPoints() < 1) throw new ModelValidationException("Must have one or more points in model!", model, "points");
		if (model.getFastAxisName()==null) throw new ModelValidationException("The model must have a fast axis!\nIt is the motor name used for this axis.", model, "fastAxisName");
		if (model.getSlowAxisName()==null) throw new ModelValidationException("The model must have a slow axis!\nIt is the motor name used for this axis.", model, "slowAxisName");
	}

}
