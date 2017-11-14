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
 * A model provider is any object that holds a model.
 *
 * @author Matthew Gerring
 *
 * @param <T>
 */
@FunctionalInterface
public interface IModelProvider<T> {

	/**
	 *
	 * @return the model that we are providing
	 */
	public T getModel() throws Exception;

	/**
	 * The model for some providers may be set but be warned that others
	 * throw an IllegalArgumentException.
	 *
	 * @param model
	 * @return the old model or null
	 */
	default void setModel(T model) throws Exception{
		throw new IllegalArgumentException("setModel is not implemented for "+getClass().getSimpleName());
	}

	/**
	 * The model for some providers may be updated but be warned that others
	 * throw an IllegalArgumentException.
	 *
	 * @param model
	 * @throws Exception
	 */
	default void updateModel(T model) throws Exception {
		throw new IllegalArgumentException("updateModel is not implemented for "+getClass().getSimpleName());
	}
}
