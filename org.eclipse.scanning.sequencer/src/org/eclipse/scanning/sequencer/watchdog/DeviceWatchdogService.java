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
package org.eclipse.scanning.sequencer.watchdog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IDeviceController;
import org.eclipse.scanning.api.device.IDeviceWatchdog;
import org.eclipse.scanning.api.device.IDeviceWatchdogService;
import org.eclipse.scanning.api.device.IPausableDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceWatchdogService implements IDeviceWatchdogService {
	
	private static Logger logger = LoggerFactory.getLogger(DeviceWatchdogService.class);
	
	private Map<String, IDeviceWatchdog> templates = Collections.synchronizedMap(new LinkedHashMap<>(3));
	
	static {
		if (System.getProperty("org.eclipse.scanning.watchdogs.active")==null) {
			System.setProperty("org.eclipse.scanning.watchdogs.active", "true");
		}
	}

	@Override
	public void register(IDeviceWatchdog dog) {
		if (templates.containsKey(dog.getName())) throw new IllegalArgumentException("The watchdog name '"+dog.getName()+"' is already registered! A watchdog with a given name may only be registered once.");
		templates.put(dog.getName(), dog);
	}

	@Override
	public void unregister(IDeviceWatchdog dog) {
		templates.remove(dog.getName());
	}

	@Override
	public IDeviceController create(IPausableDevice<?> device) {
		
		if (!Boolean.getBoolean("org.eclipse.scanning.watchdogs.active")) return null;
		if (templates==null) return null;
		try {
			DeviceController controller = new DeviceController(device);
			if (device instanceof AbstractRunnableDevice<?>) {
				controller.setBean(((AbstractRunnableDevice<?>)device).getBean());
			}
			List<IDeviceWatchdog> objects = new ArrayList<>(templates.size());
			for (final IDeviceWatchdog dog : templates.values()) {
				if (!dog.isEnabled()) continue;
				IDeviceWatchdog ndog = dog.getClass().newInstance();
				ndog.setModel(dog.getModel());
				ndog.setController(controller);
				objects.add(ndog);
			}
			controller.setObjects(objects);
			return controller;
		} catch (Exception ne) {
			ne.printStackTrace();
			logger.error("Cannot create watchdogs", ne);
			return null;
		}
	}

	@Override
	public IDeviceWatchdog getWatchdog(String name) {
		return templates.get(name);
	}

}
