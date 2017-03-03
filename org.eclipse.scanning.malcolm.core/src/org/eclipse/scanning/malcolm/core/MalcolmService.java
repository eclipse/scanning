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
package org.eclipse.scanning.malcolm.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.IMalcolmModel;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;

public class MalcolmService implements IMalcolmService {

	private IMalcolmConnectorService<MalcolmMessage> connector;	
	private IRunnableDeviceService runnableDeviceService;
	private Map<String, IMalcolmDevice<?>>        devices;
	
	/**
	 * Used by OSGi to make the service.
	 */
	public MalcolmService() throws MalcolmDeviceException {
		this(null, null);
	}

	/**
	 * 
	 * @param connectorService - null in the OSGi services scenario
	 * @param malcolmUri - 
	 * @throws MalcolmDeviceException
	 */
	public MalcolmService(IMalcolmConnectorService<MalcolmMessage> connector,
			IRunnableDeviceService runnableDeviceService) throws MalcolmDeviceException {
		this.connector = connector; // Usually null unless we are in testing mode.
		this.runnableDeviceService = runnableDeviceService;
		this.devices   = new ConcurrentHashMap<String, IMalcolmDevice<?>>(4);
	}
	
	/**
	 * Get a device by name. At the point where the device is retrieved the
	 * caller may know the type of device and use a generic to declare its model.
	 * 
	 * @param name
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public <M extends IMalcolmModel> IMalcolmDevice<M> getDevice(String name) throws MalcolmDeviceException {
        return getDevice(name, null);
	}
	
	/**
	 * Get a device by name. At the point where the device is retrieved the
	 * caller may know the type of device and use a generic to declare its model.
	 * 
	 * @param name
	 * @param publisher
	 * @return
	 * @throws MalcolmDeviceException
	 */
	@SuppressWarnings("unchecked")
	public <M extends IMalcolmModel> IMalcolmDevice<M> getDevice(String name, IPublisher<ScanBean> publisher) throws MalcolmDeviceException {

		// Check that the connector is not null
		if (connector==null) throw new MalcolmDeviceException("No connector has been set up for this Service");
		
		if (devices.containsKey(name)) return (IMalcolmDevice<M>)devices.get(name);
		
		IMalcolmDevice<M> device = new MalcolmDevice(name, connector, runnableDeviceService, publisher); // Might throw exception
		devices.put(name, device);
		return device;
	}

	public void setConnectorService(IMalcolmConnectorService<MalcolmMessage> connectorService) {
		this.connector = connectorService;
	}
	
	public void setRunnableDeviceService(IRunnableDeviceService runnableDeviceService) {
		this.runnableDeviceService = runnableDeviceService;
	}

	/**
	 * Disposes the service
	 * @throws MalcolmDeviceException
	 */
	@Override
	public void dispose() throws MalcolmDeviceException {
		devices.clear();
		if (connector != null) {
			connector.disconnect();
		}
	}
}