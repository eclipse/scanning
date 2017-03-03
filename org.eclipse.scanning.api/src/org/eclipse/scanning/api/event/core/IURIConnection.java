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

import java.net.URI;

import org.eclipse.scanning.api.event.IEventConnectorService;

public interface IURIConnection extends IDisconnectable{

	/**
	 * The URI of this connection.
	 * @return
	 */
	public URI getUri();
	
	/**
	 * The underlyng service which the uri is connected using
	 */
	public IEventConnectorService getConnectorService();
}
