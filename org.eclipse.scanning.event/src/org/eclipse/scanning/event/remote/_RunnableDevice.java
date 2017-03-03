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
import java.util.List;
import java.util.Set;

import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.device.IActivatable;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.ScanMode;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.scan.DeviceAction;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.DeviceRequest;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.attributes.MalcolmAttribute;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class _RunnableDevice<M> extends _AbstractRemoteDevice<M> implements IRunnableDevice<M>, IActivatable {
	
	private static final long REQUEST_TIMEOUT = 1000; // timeout in milliseconds
	
	private static final Logger logger = LoggerFactory.getLogger(_RunnableDevice.class);

	_RunnableDevice(DeviceRequest req, URI uri, IEventService eservice) throws EventException, InterruptedException {
		super(req, REQUEST_TIMEOUT, uri, eservice);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void configure(M model) throws ScanningException {
		try {
			DeviceRequest req = requester.post(new DeviceRequest(info.getName(), DeviceAction.CONFIGURE, model));
			merge((DeviceInformation<M>)req.getDeviceInformation());
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
	}

	@Override
	public M getModel() {
		update();
		return info.getModel();
	}
	
	@Override
	public String getDeviceStatus() throws ScanningException {
		update();
		return info.getStatus();
	}

	@Override
	public boolean isDeviceBusy() throws ScanningException {
		update();
		return info.isBusy();
	}
	
	@Override
	public DeviceRole getRole() {
		update();
		return info.getDeviceRole();
	}

	@Override
	public void setRole(DeviceRole role) {
		throw new IllegalArgumentException("Clients may not change the role of devices!");
	}

	@Override
	public Set<ScanMode> getSupportedScanModes() {
		update();
		return info.getSupportedScanModes();
	}

	@Override
	public void validate(M model) throws ValidationException {
		try {
			DeviceRequest res = requester.post(new DeviceRequest(info.getName(), DeviceAction.VALIDATE, model));
			res.checkException();
		} catch (ValidationException ve) {
			throw ve;
		} catch (Exception ne) {
            throw new ValidationException(ne);
		}
	}
	
	@Override
	public Object validateWithReturn(M model) throws ValidationException {
		try {
			DeviceRequest res = requester.post(new DeviceRequest(info.getName(), DeviceAction.VALIDATEWITHRETURN, model));
			res.checkException();
			return res.getDeviceValue();
		} catch (ValidationException ve) {
			throw ve;
		} catch (Exception ne) {
            throw new ValidationException(ne);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DeviceState getDeviceState() throws ScanningException {
		try {
			DeviceRequest req = requester.post(new DeviceRequest(name));
			merge((DeviceInformation<M>)req.getDeviceInformation());
		} catch (Exception ne) {
			throw new ScanningException(ne);
		}
		return info.getState();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected DeviceRequest update() {
		try {
			DeviceRequest req = requester.post(new DeviceRequest(name));
			merge((DeviceInformation<M>)req.getDeviceInformation());
			return req;
		} catch (Exception ne) {
			logger.error("Cannot update device info for "+info.getName(), ne);
			return null;
		}
	}

	@Override
	public void run(IPosition position) throws ScanningException, InterruptedException {
		DeviceRequest req = new DeviceRequest(info.getName(), DeviceAction.RUN);
		req.setPosition(position);
		method(req);
	}

	@Override
	public void reset() throws ScanningException {
		method(new DeviceRequest(info.getName(), DeviceAction.RESET));
	}

	@Override
	public void abort() throws ScanningException {
		method(new DeviceRequest(info.getName(), DeviceAction.ABORT));
	}

	@Override
	public void disable() throws ScanningException {
		method(new DeviceRequest(info.getName(), DeviceAction.DISABLE));
	}

	public boolean isActivated() {
		update();
		return info.isActivated();
	}
	
	public boolean setActivated(boolean activated) throws ScanningException {
		if (info==null) update();
		boolean wasactivated = info.isActivated();
		method(new DeviceRequest(info.getName(), DeviceType.RUNNABLE, DeviceAction.ACTIVATE, activated));
		return wasactivated;
	}

	@Override
	public boolean isAlive() {
		return info.isAlive();
	}

	@Override
	public void setAlive(boolean alive) {
		info.setAlive(alive);
	}

}
