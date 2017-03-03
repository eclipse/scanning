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
package org.eclipse.scanning.api.malcolm;

import org.eclipse.scanning.api.device.models.IMalcolmModel;
import org.eclipse.scanning.api.device.models.MalcolmModel;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.ScanBean;

/**
 * An OSGi service for creating Malcolm connections
 * 
 * Usage:
 * <code>
 <p>
        IMalcolmService service = ... // OSGi service <br>
        IMalcolmConnection        connection = service.createConnection("tcp://127.0.0.1:7800");<br>
<br>
		IMalcolmDevice zebra =  connection.getDevice("zebra");<br>
	    Map<String, Object> config = new HashMap<String,Object>(2);<br>
		config.put("PC_BIT_CAP", 1);<br>
		config.put("PC_TSPRE", "ms");<br>
		<br>
		zebra.configure(config);<br>
		zebra.run(); // blocks until finished<br>
		<br>
		final State state = zebra.getState();<br>
        // ... We did something!<br>
</p>        
</code>

 * @author Matthew Gerring
 *
 */
public interface IMalcolmService {
	
	/**
	 * Get a device by name. At the point where the device is retrieved the
	 * caller may know the type of device and use a generic to declare its model.
	 * 
	 * @param name
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public <M extends IMalcolmModel> IMalcolmDevice<M> getDevice(String name) throws MalcolmDeviceException;
	
	/**
	 * Get a device by name. At the point where the device is retrieved the
	 * caller may know the type of device and use a generic to declare it.
	 * 
	 * @param name
	 * @param publisher
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public <M extends IMalcolmModel> IMalcolmDevice<M> getDevice(String name, IPublisher<ScanBean> publisher) throws MalcolmDeviceException;	

	/**
	 * Disposes the service
	 * @throws MalcolmDeviceException
	 */
	public void dispose() throws MalcolmDeviceException;
}
