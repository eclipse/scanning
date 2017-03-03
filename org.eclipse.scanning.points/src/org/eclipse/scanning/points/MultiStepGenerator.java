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
import java.util.Iterator;

import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.StepModel;

/**
 * Point generator for {@link MultiStepModel}s.
 * 
 * @author Matthew Dickie
 */
class MultiStepGenerator extends AbstractGenerator<MultiStepModel> {
	
	MultiStepGenerator() {
		setLabel("MultiStep");
		setDescription("Creates a step scan as a series of ranges possibly with different step sizes");
	}
	
	public boolean isScanPointGeneratorFactory() {
		return false;
	}

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
			if (!model.getName().equals(stepModel.getName())) {
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
	
	@Override
	protected Iterator<IPosition> iteratorFromValidModel() {
		return new MultiStepIterator(model);
	}

}
