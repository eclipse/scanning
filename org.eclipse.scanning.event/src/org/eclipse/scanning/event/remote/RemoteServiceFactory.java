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
package org.eclipse.scanning.event.remote;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ResponseConfiguration;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;

/**
 * 
 * TODO FIXME - Do something better than this for remote services! Need help from
 * Data Acquisition Team on this subject.
 * 
 * It is not clear how to do remote OSGi services that work on any code base, python, javascript etc.
 * 
 * Rather than adopt a specific technology like the equinox OSGi services, it was decided
 * to make certain services have remote implementations. This is a temporary measure which 
 * if it works and is maintainable might be a supportable long term solution. However if
 * is is hard to maintain, the getRemoteService(...) call in IEventService could be replaced
 * with a call to get the full remote OSGi service. Point being this approach can be edited to
 * gracefully support something other than hand coding each remote service in the future.
 * 
 * Problems:
 * 1. Some services will actually materialize from OSGi on the client but will not be remote
 * this could be confusing.
 * 2. Inelegant design that needs work done for each new service added, we have seen how
 * poor this is this GDA CORBA. We should not repeat the same mistakes.
 * 
 * Advantages:
 * 1. Client technologies other than Java like python and javascript are possible. They would 
 * need an equivalent of RemoteServiceFactory probably to make the remote servers.
 * 2. This is a full asynchronous model using messaging. The client does not have a static endpoint.
 * For instance as far as the client is concerned the devices in the device service are all connectable
 * to and controllable, however they may be implemented on separate hardware. For instance a detector
 * could register itself from python/stomp and the client would still pick it up and make it available.
 * This complete abstraction can be advantageous in theory for facilities like DLS.
 * 
 * @author Matthew Gerring
 *
 */
public class RemoteServiceFactory {

	private static final Map<Class<?>, Class<?>> remotes;
	static {
		Map<Class<?>, Class<?>> tmp = new HashMap<>(7);
		tmp.put(IScannableDeviceService.class, _ScannableDeviceService.class);
		tmp.put(IRunnableDeviceService.class,  _RunnableDeviceService.class);
		tmp.put(IQueueControllerService.class,  _QueueControllerService.class);
		remotes = Collections.unmodifiableMap(tmp);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getRemoteService(URI uri, Class<T> clazz, IEventService eservice) throws EventException, InstantiationException, IllegalAccessException {
		if (!remotes.containsKey(clazz)) return null; // TODO Maybe throw an exception?
		T instance = (T)remotes.get(clazz).newInstance();
		if (instance instanceof AbstractRemoteService) {
			AbstractRemoteService aservice = (AbstractRemoteService)instance;
			aservice.setEventService(eservice);
			aservice.setUri(uri);
			aservice.init();
		}
		return instance;
	}
	
	private static long     time = ResponseConfiguration.DEFAULT.getTimeout();
	private static TimeUnit timeUnit = ResponseConfiguration.DEFAULT.getTimeUnit();
	
	/**
	 * Used to set the timeout where tests would like to debug a response.
	 * @param t
	 * @param u
	 */
	public static void setTimeout(long t, TimeUnit u) {
		time = t;
		timeUnit = u;
	}

	static long getTime() {
		return time;
	}

	static TimeUnit getTimeUnit() {
		return timeUnit;
	}
}
