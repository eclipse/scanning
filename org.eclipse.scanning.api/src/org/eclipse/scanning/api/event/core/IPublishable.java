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

/**
 * A process, third party software run or response which happens at a specific time.
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public interface IPublishable<T> {

	/**
	 *  
	 * @return the bean which this process is currently running.
	 */
	T getBean();
	
	/**
	 *  
	 * @return the bean which this process is currently running.
	 */
	IPublisher<T> getPublisher();

}
