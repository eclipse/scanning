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
 * The interface provided to an IConsumer which defines the work done after
 * each item is taken from the queue.
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public interface IProcessCreator<T> {

	IConsumerProcess<T> createProcess(T bean, IPublisher<T> statusNotifier) throws EventException;
}
