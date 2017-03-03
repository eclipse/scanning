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
public interface IFilterService {

	/**
	 * The default filter service. OSGi is not used for this because
	 * it is a simple implementation using JDK classes.
	 * 
	 * When a {@link Filter} is made it registers itself to IFilterService.DEFAULT
	 */
	public final static IFilterService DEFAULT = new FilterService();

	
	/**
	 * Call to register a filter with the service.
	 * 
	 * @param filter
	 */
	<T> void register(IFilter<T> filter);
	
	/**
	 * Call to register a filter with the service.
	 * 
	 * @param filter
	 */
	<T> void unregister(IFilter<T> filter);

	/**
	 * Filter the given list of items with a named filter.
	 * 
	 * If there is no filter with this name, the original list is returned.
	 * Use getFilter(...) to check if a filter is there.
     *
	 * @param filterName
	 * @param items
	 * @return
	 */
	<T> List<T> filter(String filterName, Collection<T> items);
	
	/**
	 * Filter the given list of items with a named filter.
	 * 
	 * @param filterName
	 * @param items
	 * @return
	 */
	<T> IFilter<T> getFilter(String filterName);

	/**
	 * Clears all the added filters.
	 * WARNING USe with caution, this will blat any spring created filters.
	 */
	void clear();
}
