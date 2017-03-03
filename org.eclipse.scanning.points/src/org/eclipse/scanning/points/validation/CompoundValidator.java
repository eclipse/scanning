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
package org.eclipse.scanning.points.validation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.IValidator;
import org.eclipse.scanning.api.IValidatorService;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;

class CompoundValidator implements IValidator<CompoundModel<?>> {

	public void validate(CompoundModel<?> model) throws ValidationException, InstantiationException, IllegalAccessException {
		
		if (model.getModels()==null || model.getModels().isEmpty()) {
			throw new ModelValidationException("There are no models defined.", model, "models");
		}
		
		// Each model is ok
		final IValidatorService vservice = new ValidatorService();
		for (Object mod : model.getModels()) vservice.validate(mod);
		
		// Models are separate axes
		validateAxes(model.getModels());

	}
	

	private void validateAxes(List<Object> models) throws ValidationException {
		
        List<String> usedAxes = new ArrayList<String>();
        for (Object model : models) {
			if (model instanceof IScanPathModel) {
				List<String> axes = ((IScanPathModel)model).getScannableNames();
				if (axes!=null && axes.size()>0 && usedAxes.size()>0 && axes.stream().anyMatch(usedAxes::contains)) {
					throw new ValidationException("One or more of the axes '"+axes+"' are used in a previous model!");
				}
				usedAxes.addAll(axes);
			}
		}
	}

}
