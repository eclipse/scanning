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
package org.eclipse.scanning.test.event.queues.dummy;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

public class DummyHasQueueProcess<T extends Queueable> extends DummyProcess<DummyHasQueue, T> {
	
	public static final String BEAN_CLASS_NAME = DummyHasQueue.class.getName();

	public DummyHasQueueProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
	}

	@Override
	public Class<DummyHasQueue> getBeanClass() {
		return DummyHasQueue.class;
	}

}
