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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * This class is not an OSGi service at the moment
 * because its logic uses only JDK classes therefore
 * we may add the implementation to API using a deeply
 * unfashionable singleton pattern. (This works with
 * Spring rather well however.)
 * 
 * @author Matthew Gerring
 *
 */
class FilterService implements IFilterService {


	private Map<String, IFilter<?>> filters;
	
 	/**
	 * Intentionally package private
	 */
	FilterService() {
		this.filters = new HashMap<>(3);
	}

	@Override
	public <T> void register(IFilter<T> filter) {
		filters.put(filter.getName(), filter);
	}

	@Override
	public <T> void unregister(IFilter<T> filter) {
		filters.remove(filter.getName());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> filter(String filterName, Collection<T> items) {
		if (!filters.containsKey(filterName)) return new ArrayList<>(items);
	    return ((IFilter<T>)filters.get(filterName)).filter(items);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> IFilter<T> getFilter(String filterName) {
		return (IFilter<T>)filters.get(filterName);
	}

	@Override
	public void clear() {
		filters.clear();
	}
}
