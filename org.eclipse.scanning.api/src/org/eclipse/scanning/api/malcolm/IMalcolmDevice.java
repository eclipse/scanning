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

import java.util.Set;

import org.eclipse.scanning.api.IValidator;
import org.eclipse.scanning.api.device.IAttributableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.scan.ScanningException;


/**
 * This service talks to the middleware layer state machine for controlling
 * the scan. If working inside Diamond there is a wiki here:
 * http://confluence.diamond.ac.uk/display/MAP/WP4+Middlelayer+Design+Specification
 * 
 * A version of this design is also in Malcolm.pdf in this package.
 * 
 * The name malcolm is chosen for the middleware layer between the Java OSGi server and
 * the hardware - inspired by the sitcom 'Malcolm in the Middle' 
 * https://en.wikipedia.org/wiki/Malcolm_in_the_Middle
 * 
 * This interface attempts to mirror how the hardware in the python looks, hence the
 * fact that it does not look very 'Java'. So instead of setPaused(boolean) we have
 * pause() and resume().
 * 
 * Usage:
 * <code>
 <p>
        IMalcolmService service = ... // OSGi service<br>
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

 * <img src="./doc/device_state.png" />

 * @author Matthew Gerring
 *
 */
public interface IMalcolmDevice<T> extends IRunnableEventDevice<T>, IMalcolmEventPublisher, ILatchableDevice, IAttributableDevice, IValidator<T> {
	
	/**
	 * Attempts to determine if the device is locked doing something like a configure or a run.
	 * 
	 * @return true if not in locked state, otherwise false.
	 */
	public boolean isLocked() throws MalcolmDeviceException ;
	
	/**
	 * Returns the axes that this malcolm device can move.
	 * @return
	 * @throws ScanningException
	 */
	public Set<String> getAxesToMove() throws ScanningException;

	/**
	 * Set the point generator for the malcolm device.
	 * @param pointGenerator point generator
	 */
	public void setPointGenerator(IPointGenerator<?> pointGenerator);
}
