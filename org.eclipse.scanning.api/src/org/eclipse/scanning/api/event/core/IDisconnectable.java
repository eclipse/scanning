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

import org.eclipse.scanning.api.event.EventException;

/**
 * 
 * Any object which has a disconnect may implement this interface.
 * 
 * @author Matthew Gerring
 *
 */
@FunctionalInterface
public interface IDisconnectable {

	/**
	 * Call to disconnect any resources which we no longer need.
	 * The resource may have timed out so it might not be connected, 
	 * in that case it silently returns.
	 * 
	 * @throws EventException if resource could not be disconnected. 
	 */
	public void disconnect() throws EventException ;
	
	/**
	 * 
	 */
	default boolean isDisconnected() {
		return false;
	}
}
