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
package org.eclipse.scanning.api.event.queues;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.status.Status;

/**
 * Methods for broadcasting the status and percentage complete of a bean within
 * a queue. It is assumed that the bean is configured already and that some 
 * method of broadcasting is  
 * 
 * @author Michael Wharmby
 *
 */
public interface IQueueBroadcaster<T> {
	/**
	 * Sets all the required new values on the bean, ready for broadcast, but 
	 * does not actually broadcast.
	 * 
	 * @param newStatus {@link Status} the bean has just reached.
	 * @param newPercent The value percent complete should be set to.
	 * @param message String to message to publish on the bean.
	 */
	public void updateBean(Status newStatus, Double newPercent, String message);
	
	/**
	 * Convenience method to call broadcast with both {@link Status} and 
	 * message arguments.
	 * 
	 * @param newStatus {@link Status} the bean has just reached.
	 * @param message String to message to publish on the bean.
	 * @throws EventException In case broadcasting fails.
	 */
	public default void broadcast(Status newStatus, String message) throws EventException {
		broadcast(newStatus, null, message);
	}

	/**
	 * Convenience method to call broadcast with only {@link Status} argument.
	 * 
	 * @param newStatus {@link Status} the bean has just reached.
	 * @throws EventException In case broadcasting fails.
	 */
	public default void broadcast(Status newStatus) throws EventException {
		broadcast(newStatus, null, null);
	}

	/**
	 * Convenience method to call broadcast with only percent complete 
	 * argument.
	 * 
	 * @param newPercent The value percent complete should be set to.
	 * @throws EventException In case broadcasting fails.
	 */
	public default void broadcast(double newPercent) throws EventException {
		broadcast(null, newPercent, null);
	}
	
	/**
	 * Convenience method to call broadcast with only a message argument.
	 * 
	 * @param message String to message to publish on the bean.
	 * @throws EventException In case broadcasting fails.
	 */
	public default void broadcast(String message) throws EventException {
		broadcast(null, null, message);
	}

	/**
	 * Convenience method to call broadcast with percent complete and 
	 * {@link Status} arguments.
	 * 
	 * @param newStatus Status the bean has just reached.
	 * @param newPercent The value percent complete should be set to.
	 * @throws EventException In case broadcasting fails.
	 */
	public default void broadcast(Status newStatus, Double newPercent) throws EventException {
		broadcast(newStatus, newPercent, null);
	}

	/**
	 * Broadcast the new status, update previous status, percent complete and 
	 * message of the bean associated with this process.
	 * 
	 * @param newStatus {@link Status} the bean has just reached.
	 * @param newPercent The value percent complete should be set to.
	 * @param message String to message to publish on the bean.
	 * @throws EventException In case broadcasting fails.
	 */
	public void broadcast(Status newStatus, Double newPercent, String message) throws EventException;
	
	/**
	 * Broadcast the bean when some other method or interaction of a child 
	 * queue (e.g. from {@link QueueListener}) has updated the bean 
	 * status/percent complete/message.
	 * 
	 * @throws EventException In case broadcasting fails.
	 */
	public void broadcast() throws EventException;
	
}
