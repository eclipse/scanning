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

import org.eclipse.scanning.api.IValidator;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.points.models.BoundingBox;

class BoundingBoxValidator implements IValidator<BoundingBox> {
	
	public void validate(BoundingBox model) throws ValidationException {
        if (model.getFastAxisLength()==0)  throw new ModelValidationException("The length must not be 0!", model, "fastAxisLength");
        if (model.getSlowAxisLength()==0)  throw new ModelValidationException("The length must not be 0!", model, "slowAxisLength");
	}

}
