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
 * Clients do not need to consume this service, it is provided by a bundle in the
 * workspace. Tests can hardcode an implementation of this service.
 * 
 * A service which provides the marshalling (the implementation uses Jackson) and
 * the JMS service provider (the implementation uses ActiveMQ)
 * 
 * @author Matthew Gerring
 *
 */
public interface IEventConnectorService {

	/**
	 * May be used to use the serializer to marshal any object using its serialization routine.
	 * This can be used for objects other than T
	 * @param event
	 * @return
	 */
	String marshal(Object anyObject) throws Exception;

	/**
	 * May be used to get the serializer to unmarshal any object using its serialization routine.
	 * This can be used for objects other than T
	 * @param event
	 * @return
	 */
	<U> U unmarshal(String anyObject, Class<U> beanClass) throws Exception;

	/**
	 * Create a connection factory for sending events. This method 
	 * may return null or a class implementing javax.jms.ConnectionFactory
	 * or javax.jms.QueueConnectionFactory
	 * 
	 * @param uri
	 * @return
	 */
	Object createConnectionFactory(URI uri);

}
