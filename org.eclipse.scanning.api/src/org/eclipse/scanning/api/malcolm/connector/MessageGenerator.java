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
package org.eclipse.scanning.api.malcolm.connector;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;

/**
 * Deals with details of:
 * 1. Serializing JSON   (for instance Jackson)
 * 2. Sending string (for instance over zeromq)
 * 
 * 
 * @author Matthew Gerring
 *
 */
public interface MessageGenerator<T> {

	/**
	 * Automatically generates a call for a given method by using the method name from the stack.
	 * 
	 * @param stackTrace
	 * @param running
	 * @throws MalcolmDeviceException
	 */
	T call(StackTraceElement[] stackTrace, DeviceState... states) throws MalcolmDeviceException;
	
	/**
	 * Create a get message
	 * @param endpointString
	 * @return
	 */
	T createGetMessage(String endpointString)  throws MalcolmDeviceException;

	/**
	 * Create a call message
	 * @param methodName
	 * @param params
	 * @return
	 * @throws MalcolmDeviceException
	 */
	T createCallMessage(String methodName, Object params) throws MalcolmDeviceException;

	
    /**
     * 
     * @param subscription - for example "stateMachine"
     * @return
     */
	T createSubscribeMessage(String subscription);


	/**
	 * 
	 * @return
	 */
	T createUnsubscribeMessage();

}
