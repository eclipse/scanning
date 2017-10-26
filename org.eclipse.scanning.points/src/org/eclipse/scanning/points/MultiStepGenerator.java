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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.jython.JythonObjectFactory;

/**
 * Point generator for {@link MultiStepModel}s.
 *
 * @author Matthew Dickie
 */
class MultiStepGenerator extends AbstractGenerator<MultiStepModel> {

	MultiStepGenerator() {
		setLabel("Multi-step");
		setDescription("Creates a step scan as a series of ranges possibly with different step sizes");
	}

	@Override
	public boolean isScanPointGeneratorFactory() {
		return true;
	}

	@Override
	protected ScanPointIterator iteratorFromValidModel() {
		this.model = model;

		JythonObjectFactory<ScanPointIterator> arrayGeneratorFactory = ScanPointGeneratorFactory.JArrayGeneratorFactory();

		int totalSize = 0;
		boolean finalPosWasEnd = false;
		List<double[]> positionArrays = new ArrayList<>(model.getStepModels().size());
		double previousEnd = 0;
		for (StepModel stepModel : model.getStepModels()) {
			int size = stepModel.size();
			double pos = stepModel.getStart();

			// if the start of this model is the end of the previous one, and the end of the
			// previous was was its final point, skip the first point
			if (finalPosWasEnd &&
					Math.abs(stepModel.getStart() - previousEnd) < Math.abs(stepModel.getStep() / 100)) {
				pos = pos += stepModel.getStep();
				size--;
			}
			double[] positions = new double[size];

			for (int i = 0; i < size; i++) {
				positions[i] = pos;
				pos += stepModel.getStep();
			}
			positionArrays.add(positions);
			totalSize += size;

			// record if the final position of this model is its end position (within a tolerance of step/100)
			// this is not always the case, e.g. if start=0, stop=10 and step=3
			double finalPos = positions[positions.length - 1];
			finalPosWasEnd = Math.abs(stepModel.getStop() - finalPos) < Math.abs(stepModel.getStep() / 100);
			previousEnd = stepModel.getStop();
		}

		final double[] points = new double[totalSize];
		final double[] times = new double[totalSize];

		int pos        = 0;
		int sindex     = 0;
		for (double[] positions : positionArrays) {
			System.arraycopy(positions, 0, points, pos, positions.length);
			double time = model.getStepModels().get(sindex).getExposureTime();
			Arrays.fill(times, pos, pos+positions.length, time);
			pos += positions.length;
			sindex+=1;
		}

		final ScanPointIterator pyIterator = arrayGeneratorFactory.createObject(model.getName(), "mm", points);
		return new MultiStepIterator(pyIterator, times);
	}

	@Override
	protected void validateModel() {
		super.validateModel();

		StepGenerator stepGen = new StepGenerator(); // to validate step models
		double dir = 0; // +1 for forwards, -1 for backwards, 0 when not yet calculated
		double lastStop = 0;

		if (model.getStepModels().isEmpty()) {
			throw new ModelValidationException("At least one step model must be specified", model, "stepModels");
		}

		for (StepModel stepModel : model.getStepModels()) {
			// check the inner step model has the same sign
			if (model.getName()==null || stepModel.getName()==null || !model.getName().equals(stepModel.getName())) {
				throw new ModelValidationException(MessageFormat.format(
						"Child step model must have the same name as the MultiStepModel. Expected ''{0}'', was ''{1}''", model.getName(), stepModel.getName()),
						model, "name");
			}

			// check the inner step model is valid according to StepGenerator.validate()
			stepGen.validate(stepModel);

			double stepDir = Math.signum(stepModel.getStop() - stepModel.getStart());
			if (dir == 0) {
				dir = stepDir;
			} else {
				// check this step model starts ahead (in same direction) of previous one
				double gapDir = Math.signum(stepModel.getStart() - lastStop);
				if (gapDir != dir && gapDir != 0) {
					throw new ModelValidationException(MessageFormat.format(
							"A step model must start at a point with a {0} (or equal) value than the stop value of the previous step model.",
							dir > 0 ? "higher" : "lower")
							, model, "stepModels");
				}
				// check this step model is in same direction as previous ones
				if (stepDir != dir) {
					throw new ModelValidationException(
							"Each step model must have the the same direction", model, "stepModels");
				}
			}

			// check the start of the next step is in the same direction as the
			lastStop = stepModel.getStop();
		}
	}

}
