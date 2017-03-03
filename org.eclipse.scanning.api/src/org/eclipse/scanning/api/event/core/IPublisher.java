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
package org.eclipse.scanning.api.event.core;

import java.io.PrintStream;

import org.eclipse.scanning.api.event.EventException;


public interface IPublisher<T> extends ITopicConnection {
	

	/**
	 * Sends information about a specific progress using the topic to
	 * broadcast the information which is the bean (JSON encoded but if
	 * an ISubscriber is used the API user is not exposed to how the encoding
	 * works)
	 * 
	 * @param bean
	 */
	public void broadcast(T bean) throws EventException;

	/**
	 * Calling this method true starts a thread which notifies of
	 * 
	 * 
	 * @param alive
	 */
	public void setAlive(boolean alive) throws EventException;

	/**
	 * Returns true if the producer is alive and sending a heartbeat.
	 */
	public boolean isAlive();
	
	/**
	 * The queue name to synch with the topic updates (if any)
	 * @return
	 */
	public String getStatusSetName();

	/**
	 * Set the queue name (default none) to which to echo the bean.
	 * If doing this the bean needs a UUID so that it can be found and replaced in the queue.
	 * @param queueName
	 */
	public void setStatusSetName(String queueName);
	
	/**
	 * By default the queue is added to if the item is not found.
	 * Optionally if you are managing the queue directly, you may want
	 * only an update, not an add. In that case set this flag to false.
	 * 
	 * @param isRequired
	 */
	public void setStatusSetAddRequired(boolean isRequired);

	/**
	 * You may optionally set a logging stream on a publisher so that 
	 * publications can be recorded to file for debugging.
	 * 
	 * @param stream
	 */
	public void setLoggingStream(PrintStream stream);
	
	/**
	 * If this publisher is providing alive events for a consumer,
	 * use this method to set the consumer and provide the consumer's
	 * current information when the events are sent.
	 * @param consumer
	 */
	public void setConsumer(IConsumer<?> consumer);

}
