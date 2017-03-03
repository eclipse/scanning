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
package org.eclipse.scanning.server.servlet;

import static org.eclipse.scanning.api.event.EventConstants.ACQUIRE_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.ACQUIRE_RESPONSE_TOPIC;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.scan.AcquireRequest;

/**
 * A servlet to acquire data from a particular detector.
 */
public class AcquireServlet extends AbstractResponderServlet<AcquireRequest> {
	
	public AcquireServlet() {
		super(ACQUIRE_REQUEST_TOPIC, ACQUIRE_RESPONSE_TOPIC);
	}

	@Override
	public IResponseProcess<AcquireRequest> createResponder(AcquireRequest bean,
			IPublisher<AcquireRequest> response) throws EventException {
		return new AcquireRequestHandler(bean, response);
	}

}
