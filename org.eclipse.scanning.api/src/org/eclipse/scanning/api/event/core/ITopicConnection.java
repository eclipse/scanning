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

public interface ITopicConnection extends IURIConnection {
	
	/**
	 * The string topic to publish events on for this manager.
	 * The events will be beans which serialize to JSON.
	 * 
	 * @return
	 */
	public String getTopicName();
	
	/**
	 * Sets the scan topic, causing the connection to be made to that topic and throwing an
	 * exception if the connection cannot be made.
	 * @param topic
	 * @throws EventException
	 */
	public void setTopicName(String topic) throws EventException;

	
	/**
	 * Call to disconnect this publisher or subscriber. Typically used in a try-finally block.
	 * @throws EventException
	 */
	public void disconnect() throws EventException;
 
}
