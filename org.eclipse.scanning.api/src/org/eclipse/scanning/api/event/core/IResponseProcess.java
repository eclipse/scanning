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
import org.eclipse.scanning.api.event.IdBean;

/**
 * 
 * A response is a class instance which deals with a single
 * request UUID
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public interface IResponseProcess<T extends IdBean> extends IPublishable<T> {

	/**
	 * Adds information to the request. Returns an object with
	 * the same UUID as the request.
	 * 
	 * @param request
	 * @return
	 * @throws EventException
	 */
	T process(T request) throws EventException;
}
