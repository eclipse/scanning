package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class ServicesHolder {
	
	private static IRunnableDeviceService deviceService;
	
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
	
	public static IRunnableDeviceService getDeviceService() {
		if (deviceService==null) deviceService = getService(IRunnableDeviceService.class);
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

}
