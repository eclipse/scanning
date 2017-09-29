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
package org.eclipse.scanning.event.remote;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Matthew Gerring
 *
 */
abstract class AbstractRemoteService implements IDisconnectable, Closeable {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractRemoteService.class);

	protected IEventService eservice;
	protected URI uri;
	private boolean isDisconnected;

	protected AbstractRemoteService() {
		this.isDisconnected = false;
	}

	protected IEventService getEventService() {
		return eservice;
	}

	public void setEventService(IEventService eservice) {
		this.eservice = eservice;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * Does nothing, requires override.
	 */
	void init() throws EventException {

	}

	@Override
	public void close() throws IOException {
		try {
			disconnect();
		} catch (EventException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isDisconnected() {
		return isDisconnected;
	}

	public void setDisconnected(boolean isDisconnected) {
		this.isDisconnected = isDisconnected;
	}
}
