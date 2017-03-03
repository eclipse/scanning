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
package org.eclipse.scanning.api.event;

import java.net.URI;

/**
 * 
 * This service allows messaging to be started and stopped on localhost.
 * 
 * It means that users do not have to start a manual activemq instance when
 * debugging the server. It might also mean that activemq can be run in the 
 * Acqusition Server, however this is not usually desirable because the point
 * of the messaging service it that it exists in a separate process which 
 * keeps state of queues even when consumers are restarted.
 * 
 * @author Matthew Gerring
 *
 */
public interface IMessagingService {

	
	/**
	 * Start messaging, usually activemq programmatically.
	 * 
	 * @throws EventException
	 * @return the host and remote port on which the server was started. 
	 *         If a unique port is looked for during the connection then this will be returned rather than the input URI's port.
	 * @param suggestedURI where to attempt to make the connection
	 */
	URI start(String suggestedURI) throws EventException;
	
	
	
	/**
	 * Stop messaging, usually activemq programmatically.
	 * @throws EventException
	 */
	void stop() throws EventException;

}
