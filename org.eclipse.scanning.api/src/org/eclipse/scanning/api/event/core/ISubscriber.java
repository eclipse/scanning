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

import org.eclipse.scanning.api.event.EventException;

/**
 * <p>
 * This interface is designed to make it easy to listen to all events and easy
 * to listen to a specific scan.
 * </p>
 * <b>Listen to all events:</b>
 * 
 * <pre>
 * {@code
 * IEventService service = ...
 * IScanSubscriber sub = service.createSubscriber(...)
 * IScanListener listener = new IScanListener() {
 * 		{@literal @}Override
 * 		public void scanEventPerformed(ScanEvent evt) {
 * 		// Everything that happens ...
 * 		}
 * 	};
 * sub.addListener(listener);
 * }
 * </pre>
 * 
 * <b>Listen to specific events for a given scan:</b>
 * 
 * <pre>
 * {@code
 * IScanListener listener2 = new IScanListener() {
 * 		{@literal @}Override
 * 		public void scanEventPerformed(ScanEvent evt) {
 * 		// Everything that happens ...
 *  		}
 * sub.addListener(<b>id</b>, listener2);
 * }
 * </pre>
 * 
 * NOTE: If a listener is registered it will then be associated with the scan it
 * is registered with only. It should be unregistered and readded. So for
 * instance:
 * 
 * <pre>
 * {@code sub.addListener(id, listener); // registers for id but removes it as general listener.}
 * </pre>
 * 
 * Removes the Object listener from the general listeners and defines it as a
 * listener of the id. This listener would have to be readded using
 * {@code addListener(listener)} to use it again as a general listener.
 * 
 * @author Matthew Gerring
 */
public interface ISubscriber<T> extends ITopicConnection, IPropertyFilter {

	/**
	 * Adds a listener which is notified when events are broadcast.
	 * The listener works event if the manager is running on a client
	 * and the broadcast is happening from a server because through JMS or
	 * similar messaging system which this service and manager are hiding.
	 * 
	 * @param listener
	 * @throws event exception if the remote event cannot be connected to.
	 */
	public void addListener(T listener) throws EventException;
	
	/**
	 * Removes a listener such that events are no longer sent to it.
	 * 
	 * @param listener
	 */
	public void removeListener(T listener);

	/**
	 * Register events for a given id to be reported.
	 * @param id
	 * @param listener
	 * @throws event exception if the remote event cannot be connected to.
	 */
	public void addListener(String id, T listener) throws EventException;
	
	/**
	 * Unregister events for a given id to be reported.
	 * @param id
	 * @param listener
	 */
	public void removeListener(String id, T listener);
	
	/**
	 * Unregister all listeners with this id.
	 * @param id
	 * @param listener
	 */
	public void removeListeners(String id);
	
	/**
	 * Clears all listeners without disconnecting from events or
	 * stopping the JMS threads.
	 */
	public void clear();
	
	/**
	 * Call to set if the events should be ordered and BLOCKING.
	 * The default is true. When synchronous is true events are 
	 * despatched in order and the event listener method is waited
	 * for until it returns before processing more events.
	 * If it is false the messaging thread is used to directly
	 * depatch the event meaning that more events may occur and
	 * be called the last listener has returned.
	 * @param sync
	 */
	public void setSynchronous(boolean sync);
	
	/**
	 * 
	 * @return true by default.
	 */
	public boolean isSynchronous();
}
