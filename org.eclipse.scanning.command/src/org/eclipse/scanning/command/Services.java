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
package org.eclipse.scanning.command;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.points.IPointGeneratorService;


/**
 * This class holds services for the scanning server servlets. Services should be configured to be optional and dynamic
 * and will then be injected correctly by Equinox DS.
 *
 * @author Matthew Gerring
 * @author Colin Palmer
 *
 */
public class Services {

	private static IEventService           eventService;
	private static IPointGeneratorService  generatorService;
	private static IRunnableDeviceService  runnableDeviceService;

	public static IEventService getEventService() {
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		Services.eventService = eventService;
	}

	public static IPointGeneratorService getGeneratorService() {
		return generatorService;
	}

	public static void setGeneratorService(IPointGeneratorService generatorService) {
		Services.generatorService = generatorService;
	}

	public static IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}

	public static void setRunnableDeviceService(IRunnableDeviceService deviceService) {
		Services.runnableDeviceService = deviceService;
	}
}
