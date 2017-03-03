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
package org.eclipse.scanning.api.event.core;

import java.util.List;

/**
 * 
 * @author Matthew Gerring
 *
 */
public interface IPropertyFilter {

	/**
	 * It is current only possible to filter properties by deleting them.
	 *
	 */
	public enum FilterAction {
		DELETE;
	}
	
	/**
	 * 
	 * @param name
	 * @param action
	 */
	void addProperty(String name, FilterAction... action);
	
	/**
	 * Removes the filter for the named property.
	 * @param name
	 */
	void removeProperty(String name);
	
	/**
	 * 
	 * @return the list of named properties that we are filtering.
	 */
	List<String> getProperties();
}
