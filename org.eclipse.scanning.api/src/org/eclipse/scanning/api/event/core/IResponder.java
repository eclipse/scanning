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
 * A responder is an object that can respond to a topic posted by a poster.
 * One or more responders may be registered and they post for a given request
 * to the response topic. 
 * 
 * The implementation of IPoster blocks until it thinks (via timeout) all responses
 * are in then collates them (depending on response object, for instance detectors)
 * and returns.
 * 
 * @author Matthew Gerring
 *
 */
public interface IResponder<T extends IdBean> extends IRequestResponseConnection, IBeanClass<T>{

	/**
	 * The responder will be asked to respond to posts on the request topic
	 * of this object type.
	 * 
	 * @param responder
	 * @throws EventException
	 */
	void setResponseCreator(IResponseCreator<T> responder) throws EventException;
	
}
