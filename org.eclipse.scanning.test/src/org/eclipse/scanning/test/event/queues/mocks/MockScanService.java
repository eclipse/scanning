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

import java.util.Collection;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositioner;

public class MockScanService implements IRunnableDeviceService {
	
	private IPositioner poser = new MockPositioner();

	@Override
	public IPositioner createPositioner() throws ScanningException {
		return poser;
	}

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model)
			throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model,
			boolean configure) throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model,
			IPublisher<ScanBean> publisher) throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public <T> IRunnableDevice<T> createRunnableDevice(T model,
			IPublisher<ScanBean> publisher, boolean configure) throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> IRunnableDevice<T> getRunnableDevice(String name)
			throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> IRunnableDevice<T> getRunnableDevice(String name,
			IPublisher<ScanBean> publisher) throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getRunnableDeviceNames() throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	public IPositioner getPositioner() {
		return poser;
	}

	@Override
	public IScannableDeviceService getDeviceConnectorService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformation() throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeviceInformation<?> getDeviceInformation(String name) throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> void register(IRunnableDevice<T> device) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformation(DeviceRole role) throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DeviceInformation<?>> getDeviceInformationIncludingNonAlive() throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

}
