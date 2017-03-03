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

import java.net.URI;

import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;

/**
 * A connector service is used internally to abstract the 
 * provision of a connection to malcolm. It provides the transport
 * mechanism (for instance Zeromq) and the serialization mechanism
 * (for instance Jackson or gson).
 * 
 * @author Matthew Gerring
 *
 */
public interface IMalcolmConnectorService<T> {
	/**
	 * Method to start the connection. In the case of ZeroMQ this will open the
	 * socket and throw an exception if the connection cannot be made.
	 * 
	 * @param malcolmUri
	 * @throws MalcolmDeviceException
	 */
	void connect(URI malcolmUri) throws MalcolmDeviceException;

	/**
	 * Call when the connection should be closed. In the case of ZeroMQ this will call 
	 * socket.close() for instance.
	 * 
	 * @throws MalcolmDeviceException
	 */
	void disconnect() throws MalcolmDeviceException;
	

	/**
	 * Send the message and get one back, blocking, same as send(device, T, true)
	 * @param message
	 * @return
	 * @throws MalcolmDeviceException
	 */
	T send(IMalcolmDevice<?> device, T message) throws MalcolmDeviceException;


	/**
	 * Subscribe to a message, adding the listener to the list of listeners for this message
	 * @param message
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public void subscribe(IMalcolmDevice<?> device, T msg, IMalcolmListener<T> listener) throws MalcolmDeviceException;


	/**
	 * Unsubscribe to a message, if listeners is null all listeners will be unsubscribed, otherwise just those specified.
	 * @param message
	 * @return
	 * @throws MalcolmDeviceException
	 */
	public T unsubscribe(IMalcolmDevice<?> device, T msg, IMalcolmListener<T>... listeners) throws MalcolmDeviceException;

	
	/**
	 * Creates the connection to Malcolm
	 * 
	 * @param class1
	 * @return
	 */
	MessageGenerator<T> createConnection();

	/**
	 * Creates the connection to the device in the protocol as defined with this connector service, for instance ZeroMQ.
	 * SerializedDeviceConnection encapsulates the encoding of objects sent down the connection, for instance using Jackson
	 * 
	 * @return the connection to a device talking the appropriately serialized objects.
	 * 
	 * @throws MalcolmDeviceException
	 */
	MessageGenerator<T> createDeviceConnection(IMalcolmDevice<?> device) throws MalcolmDeviceException;

	/**
	 * Listens to connection state changes on the device, notifying the specified listener of any change.
	 * 
	 * @param device the device to listen to
	 * @param listener the listener to be notified of changes
	 * @throws MalcolmDeviceException
	 */
	public void subscribeToConnectionStateChange(IMalcolmDevice<?> device, IMalcolmListener<Boolean> listener)
			throws MalcolmDeviceException;

}
