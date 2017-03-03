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
 * 
 * This is supposed to fit any object which takes a model and
 * can notify the user if the a given model is valid or not.
 * For instance IRunnableDevice, IPointGenerator.
 * 
 * IMPORTANT: A model should not be an IValidator. Models should be 
 * maintained as vanilla as possible.
 * 
 * @author Matthew Gerring
 *
 */
public interface IValidator<T> {

	/**
	 * If the given model is considered "invalid", this method throws a 
	 * ModelValidationException explaining why it is considered invalid.
	 * Otherwise, just returns. A model should be considered invalid if its
	 * parameters would cause the generator implementation to hang or crash.
	 * 
	 * @throw exception if model invalid
	 * @return 
	 */
	default void validate(T model) throws ValidationException, InstantiationException, IllegalAccessException {
		return; // They should implement a validation which throws an exception
	}

	/**
	 * The validation server will set itself on any validator incase that validator
	 * want to validate sub-parts of a complex model.
	 * 
	 * @param vservice
	 */
	default void setService(IValidatorService vservice) {
		
	}
	
	/**
	 * Same as Validation method above but returns any results sent back by validation.
	 * 
	 * @param model
	 * @return
	 * @throws ValidationException
	 */
	default Object validateWithReturn(T model) throws ValidationException {
		try {
			validate(model);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ValidationException(e);
		}
		return null; // They should implement a validation which throws an exception
	}
}
