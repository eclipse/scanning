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

import java.util.Collection;

/**
 * 
 * This interface exists to provide a mechanism for scanning.api to
 * find services from OSGi without making a dependency on OSGi directy
 * or having an activator of this bundle. It means that the API bundle
 * stays totally vanilla which is a goal such that anyone may use
 * service interfaces it defines regardless of mechanism.
 * 
 * @author Matthew Gerring
 *
 */
public interface IServiceResolver {

	/**
	 * Method comparible to getting a service from OSGi without making a dependency
	 * on OGSi in this bundle.
	 * 
	 * @param serviceClass
	 * @return
	 */
	<T> T getService(Class<T> serviceClass);
	
	/**
	 * Try and get all the declaring instances of a given service.
	 * @param serviceClass
	 * @return
	 */
	<T> Collection<T> getServices(Class<T> serviceClass) throws Exception;
}
