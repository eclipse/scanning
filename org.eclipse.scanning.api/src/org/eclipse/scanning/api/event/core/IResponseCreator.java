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

public interface IResponseCreator<T extends IdBean> {

	/**
	 * Called to create a responder.
	 * @param bean
	 * @param statusNotifier
	 * @return
	 * @throws EventException
	 */
	IResponseProcess<T> createResponder(T bean, IPublisher<T> statusNotifier) throws EventException;

	/**
	 * Override to allows events to be despatched asynchronously.
	 * Useful for devices that can be cancelled for instance.
	 * 
	 * @return
	 */
	default boolean isSynchronous() {
		return true;
	}
}
