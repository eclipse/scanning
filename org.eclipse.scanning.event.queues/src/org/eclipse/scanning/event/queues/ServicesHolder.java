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
package org.eclipse.scanning.event.queues;

import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.scan.IFilePathService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * Class to hold services referenced in the implementation of the 
 * {@link IQueueService} and it's subsystems. Services are configured
 * by OSGi (or may be set by tests).
 * 
 * @author Michael Wharmby
 *
 */
public final class ServicesHolder {

	private static IRunnableDeviceService deviceService;
	private static IEventService eventService;
	private static IQueueService queueService;
	private static IQueueControllerService controllerService;
	private static IScannableDeviceService scannableDeviceService;
	private static IFilePathService filePathService;
	private static INexusFileFactory nexusFileFactory;

	public static IRunnableDeviceService getDeviceService() {
		return deviceService;
	}

	public static void setDeviceService(IRunnableDeviceService deviceService) {
		ServicesHolder.deviceService = deviceService;
	}

	public static void unsetDeviceService(IRunnableDeviceService deviceService) {
		if (ServicesHolder.deviceService == deviceService) {
			ServicesHolder.deviceService = null;
		}
	}

	public static IEventService getEventService() {
		if (eventService==null) eventService = getService(IEventService.class);
		return eventService;
	}

	public static void setEventService(IEventService eventService) {
		ServicesHolder.eventService = eventService;
	}

	public static void unsetEventService(IEventService eventService) {
		if (ServicesHolder.eventService == eventService) {
			ServicesHolder.eventService = null;
		}
	}

	public static IQueueService getQueueService() {
		if (queueService==null) queueService = getService(IQueueService.class);
		return queueService;
	}

	public static void setQueueService(IQueueService queueService) {
		ServicesHolder.queueService = queueService;
	}

	public static void unsetQueueService(IQueueService queueService) {
		if (ServicesHolder.queueService == queueService) {
			ServicesHolder.queueService = null;
		}
	}
	
	public static IQueueControllerService getQueueControllerService() {
		if (controllerService==null) controllerService = getService(IQueueControllerService.class);
		return controllerService;
	}
	
	public static void setQueueControllerService(IQueueControllerService controllerService) {
		ServicesHolder.controllerService = controllerService;
	}
	
	public static void unsetQueueControllerService(IQueueControllerService controllerService) {
		if (ServicesHolder.controllerService == controllerService) {
			ServicesHolder.controllerService = null;
		}
	}

	public static IScannableDeviceService getScannableDeviceService() {
		if (scannableDeviceService==null) scannableDeviceService = getService(IScannableDeviceService.class);
		return scannableDeviceService;
	}

	public static void setScannableDeviceService(IScannableDeviceService scannableDeviceService) {
		ServicesHolder.scannableDeviceService = scannableDeviceService;
	}
	
	private static ComponentContext context;
	private static ServicesHolder   current;
	
	private static <T> T getService(Class<T> clazz) {
		if (context == null) return null;
		try {
			ServiceReference<T> ref = context.getBundleContext().getServiceReference(clazz);
	        return context.getBundleContext().getService(ref);
		} catch (NullPointerException npe) {
			return null;
		}
	}


	public void start(ComponentContext c) {
		context = c;
		current = this;
	}
	
	public void stop() {
		current = null;
	}

	public static ServicesHolder getCurrent() {
		return current;
	}

	public static IFilePathService getFilePathService() {
		if (filePathService==null) filePathService = getService(IFilePathService.class);
		return filePathService;
	}

	public static void setFilePathService(IFilePathService filePathService) {
		ServicesHolder.filePathService = filePathService;
	}

	public static INexusFileFactory getNexusFileFactory() {
		if (nexusFileFactory==null) nexusFileFactory = getService(INexusFileFactory.class);
		return nexusFileFactory;
	}

	public static void setNexusFileFactory(INexusFileFactory nexusFileFactory) {
		ServicesHolder.nexusFileFactory = nexusFileFactory;
	}


}
