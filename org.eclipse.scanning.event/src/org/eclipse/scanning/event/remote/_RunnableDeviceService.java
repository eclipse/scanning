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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.ResponseConfiguration;
import org.eclipse.scanning.api.event.core.ResponseConfiguration.ResponseType;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.DeviceRequest;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class _RunnableDeviceService extends AbstractRemoteService implements IRunnableDeviceService {
	
	private static final Logger logger = LoggerFactory.getLogger(_RunnableDeviceService.class);

	private IRequester<DeviceRequest> requester;
	private IScannableDeviceService   cservice;
	private Map<String, IRunnableDevice<?>> runnables;
	
	public void init() throws EventException {
		requester = eservice.createRequestor(uri, IEventService.DEVICE_REQUEST_TOPIC, IEventService.DEVICE_RESPONSE_TOPIC);
		requester.setResponseConfiguration(new ResponseConfiguration(ResponseType.ONE, 500, RemoteServiceFactory.getTimeUnit()));
		runnables = new HashMap<>();
	}
	
	@Override
	public void disconnect() throws EventException {
		requester.disconnect(); // Requester can still be used again after a disconnect
		for (String name : runnables.keySet()) {
			IRunnableDevice<?> runnable = runnables.remove(name);
			if (runnable instanceof IDisconnectable) ((IDisconnectable)runnable).disconnect();
		}
		runnables.clear();
		setDisconnected(true);
	}

	@Override
	public IPositioner createPositioner() throws ScanningException {
		try {
			return new _Positioner(uri, eservice);
		} catch (EventException e) {
			throw new ScanningException("Cannot create a positioner!", e);
		}
	}

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model) throws ScanningException {
		return createRunnableDevice(model, true);
	}

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model, boolean configure) throws ScanningException {
		try {
			return new _RunnableDevice(new DeviceRequest(model, configure), uri, eservice);
		} catch (EventException | InterruptedException e) {
			throw new ScanningException(e);
		}
	}

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model, IPublisher<ScanBean> publisher) throws ScanningException {
		throw new ScanningException("Not possible to set custom publishers on "+getClass().getSimpleName()+" because it is remote!");
	}
	
	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model, IPublisher<ScanBean> publisher, boolean configure) throws ScanningException {
		throw new ScanningException("Not possible to set custom publishers on "+getClass().getSimpleName()+" because it is remote!");
	}

	@Override
	public <T> IRunnableDevice<T> getRunnableDevice(String name) throws ScanningException {
		return getRunnableDevice(name, null);
	}

	@Override
	public <T> IRunnableDevice<T> getRunnableDevice(String name, IPublisher<ScanBean> publisher) throws ScanningException {
		try {
			if (runnables.containsKey(name)) return (IRunnableDevice<T>)runnables.get(name);
			IRunnableDevice<T> device = new _RunnableDevice(new DeviceRequest(name), uri, eservice);
			runnables.put(name, device);
			return device;
		} catch (EventException | InterruptedException e) {
			throw new ScanningException(e);
		}
	}

	@Override
	public Collection<String> getRunnableDeviceNames() throws ScanningException {
		DeviceInformation<?>[] devices = getDevices();
	    String[] names = new String[devices.length];
	    for (int i = 0; i < devices.length; i++) names[i] = devices[i].getName();
		return Arrays.asList(names);
	}

	@Override
	public IScannableDeviceService getDeviceConnectorService() {
		if (cservice == null) {
			try {
				cservice = RemoteServiceFactory.getRemoteService(uri, IScannableDeviceService.class, eservice);
			} catch (InstantiationException | IllegalAccessException | EventException e) {
				logger.error("Cannot get service!", e);
			}
		}
		return cservice;
	}

	private DeviceInformation<?>[] getDevices() throws ScanningException {
	    return getDevices(false);
	}

	private DeviceInformation<?>[] getDevices(boolean getNonAlive) throws ScanningException {
	    DeviceRequest req;
		try {
			DeviceRequest outboundRequest = new DeviceRequest();
			outboundRequest.setIncludeNonAlive(getNonAlive);
			req = requester.post(outboundRequest);
			req.checkException();
		} catch (EventException | InterruptedException e) {
			throw new ScanningException("Cannot get devices! Connection to broker may be lost or no server up!", e);
		}
	    return req.getDevices().toArray(new DeviceInformation<?>[req.size()]);
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformationIncludingNonAlive() throws ScanningException {
		// TODO Matt G - extend the timeout here. Timeout seems to be set in init(). Possibly need to set new
		// ResponseConfiguration with longer timeout here, and then set it back afterwards?
		return Arrays.asList(getDevices(true));
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformation() throws ScanningException {
		return Arrays.asList(getDevices());
	}
	@Override
	public Collection<DeviceInformation<?>> getDeviceInformation(DeviceRole role) throws ScanningException {
		return getDeviceInformation().stream().filter(info -> info.getDeviceRole()==role).collect(Collectors.toList());
	}

	@Override
	public DeviceInformation<?> getDeviceInformation(String name) throws ScanningException {
	    DeviceRequest req;
		try {
			req = requester.post(new DeviceRequest(name));
		} catch (EventException | InterruptedException e) {
			throw new ScanningException("Cannot get devices! Connection to broker may be lost or no server up!", e);
		}
	    return req.getDeviceInformation();
	}

	@Override
	public <T> void register(IRunnableDevice<T> device) {
		throw new IllegalArgumentException("New devices may not be registered on a remote service implementation!");
	}
}
