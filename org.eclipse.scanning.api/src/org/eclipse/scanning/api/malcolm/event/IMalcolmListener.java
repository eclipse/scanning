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
package org.eclipse.scanning.api.malcolm.event;

import java.util.EventListener;

/**
 * 
 * This is a plain old event which can be used to get the
 * events during a scan. 
 * 
 * @author Matthew Gerring
 *
 */
public interface IMalcolmListener<T> extends EventListener {

	/**
	 * Called when Malcolm notifies the service that something happened.
	 * @param e
	 */
	public void eventPerformed(MalcolmEvent<T> e);
}
