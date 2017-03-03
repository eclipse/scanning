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
package org.eclipse.scanning.test.event.queues.mocks;

import java.net.URI;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.IResponseCreator;

public class MockResponder<T extends IdBean> implements IResponder<T> {

	@Override
	public void setRequestTopic(String requestTopic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getRequestTopic() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setResponseTopic(String responseTopic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getResponseTopic() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnect() throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public URI getUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IEventConnectorService getConnectorService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDisconnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<T> getBeanClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBeanClass(Class<T> beanClass) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResponseCreator(IResponseCreator<T> responder) throws EventException {
		// TODO Auto-generated method stub
		
	}

}
