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

import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.core.ResponseConfiguration.ResponseType;

/**
 * 
 * A poster broadcasts a request of an object with a unique id.
 * The server then fills in information about this object and
 * broadcasts it back. The UUID must be set to determine which
 * response belongs with whcih request. 
 * <p>
 * This mimics web server functionality but uses the existing
 * messaging system without requiring a web server configuration.
 * An alternative to this might be to start a jetty server on
 * the acquisition server and response to requests directly.
 * <p>
 * A use case for this is where a client would like to get a list
 * of detectors. NOTE: Unlike a web server this paradigm allows
 * multiple responders to reply on a topic. Therefore for instance
 * if many malcolm devices are available on different ports, a post
 * will be made asking for those devices and the response from many
 * listeners of the post topic collated.
 * <p>
 * @param T You <b>must</b> override the merge(...) method in your type. This
 * puts the reponder's information into your bean. In the case where there
 * are multiple reponders (like a list of detectors) you must be additive
 * in the merge method.
 * <p>
 * @author Matthew Gerring
 *
 */
public interface IRequester<T extends IdBean> extends IRequestResponseConnection {
	
	
	/**
	 * The default response configuration is 1 reponse with a timeout of 1 second
	 * {@link ResponseConfiguration.DEFAULT}
	 * @return
	 */
	public ResponseConfiguration getResponseConfiguration();
	
	/**
	 * The default response configuration is 1 reponse with a timeout of 1 second.
	 * If multiple responders are likely for a given request, use {@link ResponseType.ONE_OR_MORE}
	 */
	public void setResponseConfiguration(ResponseConfiguration rc);

	/**
	 * Set the timout on the ResponseConfiguration directly
	 * @param time
	 * @param unit
	 */
	public void setTimeout(long time, TimeUnit unit);

	/**
	 * Requests a response from the request and returns it. This 
	 * method blocks until the response has been retrieved with the 
	 * correct uuid. 
	 * 
	 * Calls post and waits for the timeout until one or more reponses
	 * have come in (depending on the response confioguration) then returns.
	 * 
	 * @param request
	 * @return
	 * @throws EventException
	 */
	T post(T request) throws EventException, InterruptedException;
	
	/**
	 * Same as post with an optional ResponseWaiter (may be null) which
	 * provides the ability to return true if the post should carry on waiting. 
	 * This is useful for instance in the case where a scannable is setting
	 * position. It will have notified position recently and if the waiter thinks
	 * it is still alive there is not reason to timeout. This is useful
	 * in setPosition(...) calls for scannbles that can take an indeterminate
	 * time but should still timeout if they go inactive.
	 * 
	 * @param request
	 * @param waiter
	 * @return
	 * @throws EventException
	 * @throws InterruptedException
	 */
	T post(T request, ResponseConfiguration.ResponseWaiter waiter) throws EventException, InterruptedException;
	
}
