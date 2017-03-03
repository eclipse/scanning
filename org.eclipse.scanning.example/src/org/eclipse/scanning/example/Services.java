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
package org.eclipse.scanning.example;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class Services {

	private static Services         current;
	private static ComponentContext context;

	private static IEventService eventService;
	private static IRunnableDeviceService runnableDeviceService;
	private static IScannableDeviceService scannableDeviceService;
	private static IPointGeneratorService pointGeneratorService;

	
	private static <T> T getService(Class<T> clazz) {
		if (context == null) return null;
		try {
			ServiceReference<T> ref = context.getBundleContext().getServiceReference(clazz);
	        return context.getBundleContext().getService(ref);
		} catch (NullPointerException npe) {
			return null;
		}
	}

	public static IRunnableDeviceService getRunnableDeviceService() {
		if (runnableDeviceService==null) runnableDeviceService = getService(IRunnableDeviceService.class);
		return runnableDeviceService;
	}

	public static void setRunnableDeviceService(IRunnableDeviceService runnableDeviceService) {
		Services.runnableDeviceService = runnableDeviceService;
	}

	public static IEventService getEventService() {
		if (eventService==null) eventService = getService(IEventService.class);
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		Services.eventService = eventService;
	}
	
	public static IPointGeneratorService getPointGeneratorService() {
		if (pointGeneratorService==null) pointGeneratorService = getService(IPointGeneratorService.class);
		return pointGeneratorService;
	}
	
	public static void setPointGeneratorService(IPointGeneratorService pointGeneratorService) {
		Services.pointGeneratorService = pointGeneratorService;
	}

	public static IScannableDeviceService getScannableDeviceService() {
		if (scannableDeviceService==null) scannableDeviceService = getService(IScannableDeviceService.class);
		return scannableDeviceService;
	}

	public static void setScannableDeviceService(IScannableDeviceService scannableDeviceService) {
		Services.scannableDeviceService = scannableDeviceService;
	}

	public void start(ComponentContext context) {
		this.context = context;
		current = this;
	}
	
	public void stop() {
		current = null;
	}

	public static Services getCurrent() {
		return current;
	}

}
