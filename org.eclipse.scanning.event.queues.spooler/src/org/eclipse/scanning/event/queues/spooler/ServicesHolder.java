package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class ServicesHolder {
	
	private static IRunnableDeviceService runnableDeviceService;
	private static IScannableDeviceService scannableDeviceService;
	
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
	
	public static IRunnableDeviceService getRunnableDeviceService() {
		if (runnableDeviceService==null) runnableDeviceService = getService(IRunnableDeviceService.class);
		return runnableDeviceService;
	}

	public static void setRunnableDeviceService(IRunnableDeviceService deviceService) {
		ServicesHolder.runnableDeviceService = deviceService;
	}

	public static void unsetRunnableDeviceService(IRunnableDeviceService deviceService) {
		if (ServicesHolder.runnableDeviceService == deviceService) {
			ServicesHolder.runnableDeviceService = null;
		}
	}
	
	public static IScannableDeviceService getScannableDeviceService() {
		if (scannableDeviceService==null) scannableDeviceService = getService(IScannableDeviceService.class);
		return scannableDeviceService;
	}

	public static void setScannableDeviceService(IScannableDeviceService scannableDeviceService) {
		ServicesHolder.scannableDeviceService = scannableDeviceService;
	}
	
	public static void unsetScannableDeviceService(IScannableDeviceService scannableDeviceService) {
		if (ServicesHolder.scannableDeviceService == scannableDeviceService) {
			ServicesHolder.scannableDeviceService = null;
		}
	}

}
