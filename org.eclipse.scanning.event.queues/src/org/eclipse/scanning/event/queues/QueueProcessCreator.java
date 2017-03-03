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
package org.eclipse.scanning.event.queues;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * QueueProcessCreator creates the class which processes a given atom/bean. 
 * The processor returned depends on the type of the atom/bean.
 * 
 * @author Michael Wharmby
 *
 * @param <T> Base type of atom/bean operated on by the queue, e.g. 
 *            {@link QueueAtom} or {@QueueBean}.
 */
public class QueueProcessCreator<T extends Queueable> implements IProcessCreator<T> {

	private boolean blocking;
	
	public QueueProcessCreator(boolean blocking) { //TODO Do we need this?
		this.blocking = blocking;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IConsumerProcess<T> createProcess(T bean,
			IPublisher<T> publisher) throws EventException {

		return QueueProcessFactory.getProcessor(bean, publisher, blocking);
	}

}
