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

public interface IBeanClass<T> {

	/**
	 * Class of bean usually extending StatusBean
	 * 
	 * @return class or null
	 */
	public Class<T> getBeanClass();

	/**
	 * Class of bean usually extending StatusBean
	 * 
	 * It is not compulsory to set the bean class unless trying to deserialize messages sent by older versions of the connector service.
	 */
	public void setBeanClass(Class<T> beanClass);

}
