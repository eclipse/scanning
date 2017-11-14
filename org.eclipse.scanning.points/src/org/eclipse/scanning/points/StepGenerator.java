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

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

class StepGenerator extends AbstractGenerator<StepModel> {

	StepGenerator() {
		setLabel("Step");
		setDescription("Creates a step scan.\nIf the last requested point is within 1%\nof the end it will still be included in the scan");
		setIconPath("icons/scanner--step.png"); // This icon exists in the rendering bundle
	}

	@Override
	protected void validateModel() {
		super.validateModel();
		double div = ((model.getStop()-model.getStart())/model.getStep());
		if (div < 0) throw new ModelValidationException("Model step is directed in the wrong direction!", model, "start", "stop", "step");
		if (!Double.isFinite(div)) throw new ModelValidationException("Model step size must be nonzero!", model, "start", "stop", "step");
	}

	@Override
	public int sizeOfValidModel() throws GeneratorException {
		if (containers!=null) throw new GeneratorException("Cannot deal with regions in a step scan!");
		return getModel().size();
	}

	protected ScanPointIterator createPyIterator() {
		final StepModel model = getModel();

        final JythonObjectFactory<ScanPointIterator> lineGeneratorFactory = ScanPointGeneratorFactory.JLineGenerator1DFactory();

        final String name   = model.getName();
        final double start  = model.getStart();
        final double stop   = model.getStop();
        final int numPoints = model.size();

		return lineGeneratorFactory.createObject(name, "mm", start, stop, numPoints);
	}

	@Override
	public ScanPointIterator iteratorFromValidModel() {
		final ScanPointIterator pyIterator = createPyIterator();
		return new StepIterator(getModel(), pyIterator);
	}

	@Override
	public int[] getShape() throws GeneratorException {
		return new int[] { sizeOfValidModel() };
	}

	@Override
	public String toString() {
		return "StepGenerator [" + super.toString() + "]";
	}

}
