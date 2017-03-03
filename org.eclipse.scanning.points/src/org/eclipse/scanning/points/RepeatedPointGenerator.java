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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.models.RepeatedPointModel;

class RepeatedPointGenerator extends AbstractGenerator<RepeatedPointModel> implements IDeviceDependentIterable {
	
	RepeatedPointGenerator() {
		setLabel("Repeat");
		setDescription("Repeats a point a given number of times.");
		setIconPath("icons/scanner--repeat.png"); // This icon exists in the rendering bundle 
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		if (model.getCount() <= 0) throw new ModelValidationException("Count must be greater than 0", model, "count");
		if (model.getSleep() < 0) throw new ModelValidationException("Sleep must be 0 or more", model, "sleep");
	}

	@Override
	public Iterator<IPosition> iteratorFromValidModel() {
		return new RepeatedPointIterator(this);
	}

	@Override
	protected int sizeOfValidModel() throws GeneratorException {
		return model.getCount();
	}

	/**
	 * Provide the names of the scannables at each position without making
	 * an iterator.
	 * 
	 * @return
	 */
	public List<String> getScannableNames() {
		return Arrays.asList(model.getName());
	}

	
	public boolean isScanPointGeneratorFactory() {
		return false;
	}
	
	@Override
	public IPosition getFirstPoint() {
		return new Scalar<>(model.getName(), 0, model.getValue());
	}
}
