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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.DeviceRequest;
import org.eclipse.scanning.api.scan.ScanningException;

abstract class _AbstractRemoteDevice<M> extends AbstractRemoteService {

	
	protected String                          name;
	protected DeviceInformation<M>            info;
	protected final IRequester<DeviceRequest> requester;

	private _AbstractRemoteDevice(URI uri, IEventService eservice) throws EventException {
		setEventService(eservice);
		setUri(uri);
		requester = eservice.createRequestor(uri, EventConstants.DEVICE_REQUEST_TOPIC, EventConstants.DEVICE_RESPONSE_TOPIC);
	}

	@SuppressWarnings("unchecked")
	_AbstractRemoteDevice(DeviceRequest req, URI uri, IEventService eservice) throws EventException, InterruptedException {
		this(uri, eservice);
	    requester.setTimeout(RemoteServiceFactory.getTime(), RemoteServiceFactory.getTimeUnit()); // Useful for debugging testing 
	    connect(req);
	}

	_AbstractRemoteDevice(DeviceRequest req, long timeoutMs, URI uri, IEventService eservice) throws EventException, InterruptedException {
		this(uri, eservice);
	    requester.setTimeout(timeoutMs, TimeUnit.MILLISECONDS); // Useful for debugging testing 
	    connect(req);
	}
	
	private void connect(DeviceRequest req) throws EventException, InterruptedException, ValidationException {
		req = requester.post(req);
		req.checkException();
		info = (DeviceInformation<M>)req.getDeviceInformation();
		this.name = info.getName();
	}
	
	public void disconnect() throws EventException {
		requester.disconnect(); // Requester can still be used again after a disconnect
		setDisconnected(true);
	}

	public String getName() {
		if (info==null) update();
		return info.getName();
	}

	public void setName(String name) {
		// TODO
		throw new RuntimeException("Devices may not have this changed remotely currently!");
	}

	public void setLevel(int level) {
		// TODO
		throw new RuntimeException("Devices may not have this changed remotely currently!");
	}

	public int getLevel() {
		if (info==null) update();
		return info.getLevel();
	}

	protected abstract DeviceRequest update();

	
	protected void merge(DeviceInformation<M> info) {
		if (info == null) return; // Nothing to merge
		if (this.info == null) {
			this.info = info;
			return;
		}
		this.info.merge(info);       
	}
	
	protected void method(DeviceRequest deviceRequest) throws ScanningException {
		try {
			DeviceRequest req = requester.post(deviceRequest);
			merge((DeviceInformation<M>)req.getDeviceInformation());
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
	}

}
