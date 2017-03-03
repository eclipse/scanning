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
package org.eclipse.scanning.api.event.servlet;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IPublisher;

/**
 * A servlet for processing a queue.
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public interface IConsumerServlet<T> extends IConnectable {

	
	/**
	 * Creates a process for each request processed from the queue for this servlet.
	 * 
	 * @param bean
	 * @param response
	 * @return
	 */
	public IConsumerProcess<T> createProcess(T bean, IPublisher<T> response) throws EventException;
}
