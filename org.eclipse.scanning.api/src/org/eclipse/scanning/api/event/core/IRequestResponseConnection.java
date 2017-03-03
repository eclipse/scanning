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
 * 
 * @author Matthew Gerring
 *
 */
public interface IRequestResponseConnection extends IURIConnection {

	/**
	 * The topic used to request the response
	 */
	void setRequestTopic(String requestTopic);
	
	/**
	 * The topic used to request the response
	 * @return
	 */
	String getRequestTopic();
	
	/**
	 * The topic used to send the response.
	 */
	void setResponseTopic(String responseTopic);
	
	/**
	 * The topic used to send the response.
	 * @return
	 */
	String getResponseTopic();
	
	/**
	 * Call to disconnect
	 * @throws EventException
	 */
	public void disconnect() throws EventException;

}
