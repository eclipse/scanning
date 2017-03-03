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
package org.eclipse.scanning.api.filter;

import java.util.Collection;
import java.util.List;

import org.eclipse.scanning.api.INameable;

/**
 * <h3> Filters </h3>
 * Filters work as follows:<p>
 * 
 * <pre>
 * L = list of things to be filtered
 * e = excluded function
 * i = included function
 * l = filtered list.
 * <p>
 *   l = L-e(L)+i(L)
 * </pre>
 * 
 * This means that all combinations are possible.
 * An exclude of .* may be applied if only particular devices
 * are required in the filter and everything done with includes.
 * A particular set of devices may be removed but if one is
 * required, it may be added back in with the final include.
 * 
 * @author Matthew Gerring
 *
 */
public interface IFilter<T> extends INameable {

	/**
	 * 
	 * @param items
	 * @return filtered set
	 */
	List<T> filter(Collection<T> items);
	
	/**
	 * The list of exclude regular expressions
	 * 
	 * These regular expressions are matched with the String.matches(String) method.
	 * 
	 * @return
	 */
	List<String> getExcludes();
	
	/**
	 * The list of exclude regular expressions
	 * These regular expressions are matched with the String.matches(String) method.
	 * @return
	 */
	void setExcludes(List<String> excludes);
	
	/**
	 * The list of include regular expressions
	 * These regular expressions are matched with the String.matches(String) method.
	 * @return
	 */
	List<String> getIncludes();
	
	/**
	 * The list of include regular expressions
	 * These regular expressions are matched with the String.matches(String) method.
	 * @return
	 */
	void setIncludes(List<String> includes);

}
