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
package org.eclipse.scanning.api;

/**
 * A service used to validate models of different types without making a dependency
 * on the validator concerned.
 * 
 * All the point generators are validators and will validate their own models. They
 * may also have submodels and these can be edited separately to the validator therefore
 * a separate service is required for validation.
 * 
 * @author Matthew Gerring
 *
 */
public interface IValidatorService {

	
	/**
	 * Call to validate a given model or model component.
	 * @param model
	 * @throws Exception, ModelValidationException
	 */
    <T> void validate(T model) throws ValidationException, InstantiationException, IllegalAccessException;

    /**
     * Get the validator for a given model or null if the model is not supported.
     * @param model
     * @return
     */
    <T> IValidator<T> getValidator(T model) throws InstantiationException, IllegalAccessException; 
}
