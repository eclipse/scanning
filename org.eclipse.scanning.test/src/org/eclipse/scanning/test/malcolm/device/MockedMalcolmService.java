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
package org.eclipse.scanning.test.malcolm.device;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.device.models.IMalcolmModel;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;

public class MockedMalcolmService implements IMalcolmService {
	
	private Map<String, IMalcolmDevice<?>> devices;
	private final LatchDelegate latcher;
	private boolean usePausableDevices;

	public MockedMalcolmService(final boolean pausable) {
		super();
		this.latcher = new LatchDelegate();
		usePausableDevices = pausable;
	}

	public void dispose() throws MalcolmDeviceException {
	}
	
	@Override
	public <M extends IMalcolmModel> IMalcolmDevice<M> getDevice(String name) throws MalcolmDeviceException {
		return getDevice(name, null);
	}
	@Override
	public <M extends IMalcolmModel> IMalcolmDevice<M> getDevice(String name, IPublisher<ScanBean> publisher) throws MalcolmDeviceException {
		try {
			if (devices==null || devices.isEmpty()) {
				devices = new HashMap<String, IMalcolmDevice<?>>(1);
				IMalcolmDevice<M> device = usePausableDevices ? 
						(IMalcolmDevice<M>) new MockedWriteInLoopPausableMalcolmDevice("zebra", latcher) : 
						(IMalcolmDevice<M>) new MockedMalcolmDevice("zebra"); // TODO why are these casts required?
				
				devices.put("zebra", device);
			}
			IMalcolmDevice<M> device = (IMalcolmDevice<M>) devices.get(name);
			if (device!=null) return device;
			throw new MalcolmDeviceException("Invalid name "+name);
		    
		} catch (Exception ne) {
			throw new MalcolmDeviceException(null, "Unable to connect to device!", ne);
		}
	}
}
