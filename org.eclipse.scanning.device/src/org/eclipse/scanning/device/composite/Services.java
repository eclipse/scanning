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
package org.eclipse.scanning.device.composite;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * This class holds services for the scanning server servlets. Services should be configured to be optional and dynamic
 * and will then be injected correctly by Equinox DS.
 */
public class Services {

	private static IScannableDeviceService scannableDeviceService;

	private static ComponentContext context;

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
	}

	public void stop() {
	}

	public static IScannableDeviceService getScannableDeviceService() {
		if (scannableDeviceService==null) scannableDeviceService = getService(IScannableDeviceService.class);
		return scannableDeviceService;
	}

	public static void setScannableDeviceService(IScannableDeviceService scannableDeviceService) {
		Services.scannableDeviceService = scannableDeviceService;
	}
}
