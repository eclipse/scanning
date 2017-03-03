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

import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;

/**
 * This is an object that can be configured to 
 * send events in topics which can be listened to in other processes.
 * It also allows events to be directly listened to in this VM.
 * 
 * In the case of Malcolm, the publisher receives events from Malcolm 
 * and publishes them to ActiveMQ. These events can then be picked up 
 * and the SWMR file read to get the data.
 * 
 * @author Matthew Gerring
 *
 */
public interface IMalcolmEventPublisher {
	/**
	 * Add a listener which can be used instead of monitoring
	 * JMS topics. Useful if the device connection to Malcolm
	 * is running in the same VM.
	 * 
	 * @param l
	 */
	public void addMalcolmListener(IMalcolmListener<?> l);
	
	/**
	 * Remove a listener which can be used instead of monitoring
	 * JMS topics.
     *
	 * @param l
	 */
	public void removeMalcolmListener(IMalcolmListener<?> l);
	
	
	/**
	 * Call to dispose of the resources used send events, clear listener
	 * lists and any other connections.
	 */
	public void dispose() throws MalcolmDeviceException ;


}
