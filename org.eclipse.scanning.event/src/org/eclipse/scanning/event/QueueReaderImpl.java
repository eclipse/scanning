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
package org.eclipse.scanning.event;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IQueueReader;

public class QueueReaderImpl<T> extends AbstractConnection implements IQueueReader<T> {

	private IEventService eservice;
	private Class<T> beanClass;

	QueueReaderImpl(URI uri, String qName, IEventService service) {
		super(uri, null, service.getEventConnectorService());
		setSubmitQueueName(qName);
		this.eservice = service;
	}

	@Override
	public List<T> getQueue() throws EventException {
					
		QueueReader<T> reader = new QueueReader<T>(getConnectorService(), null);
		try {
			return reader.getBeans(uri, getSubmitQueueName(), beanClass);
		} catch (Exception e) {
			throw new EventException("Cannot get the beans for queue " + getSubmitQueueName(), e);
		}
	}

	@Override
	public Class<T> getBeanClass() {
		return beanClass;
	}

	@Override
	public void setBeanClass(Class<T> beanClass) {
		this.beanClass = beanClass;
	}
	

}
