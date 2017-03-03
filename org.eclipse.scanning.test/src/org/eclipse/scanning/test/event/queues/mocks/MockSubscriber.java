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
import java.util.EventListener;
import java.util.List;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.core.ISubscriber;

public class MockSubscriber<T extends EventListener> implements ISubscriber<T> {
	
	private boolean disconnected = false;
	
	public MockSubscriber(URI uri, String topicName) {
		// TODO Auto-generated constructor stub
	}
	
	public void resetSubscriber() {
		disconnected= false;
	}

	@Override
	public String getTopicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTopicName(String topic) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() throws EventException {
		disconnected = true;
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
	public void addListener(T listener) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeListener(T listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addListener(String id, T listener) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeListener(String id, T listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDisconnected() {
		return disconnected;
	}

	@Override
	public void addProperty(String name, FilterAction... action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeProperty(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeListeners(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSynchronous(boolean sync) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSynchronous() {
		// TODO Auto-generated method stub
		return false;
	}

}
