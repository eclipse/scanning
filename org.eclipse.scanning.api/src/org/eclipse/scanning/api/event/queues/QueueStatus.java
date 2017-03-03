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

/**
 * Current state of a queue managed within the {@link IQueueService}.
 * 
 * @author Michael Wharmby
 *
 */
public enum QueueStatus {
	INITIALISED, STARTED, STOPPING, STOPPED, KILLED, DISPOSED;
	
	/**
	 * Return whether the queue is running
	 * 
	 * @return true if running.
	 */
	public boolean isActive() {
		return this == STARTED;
	}
	
	/**
	 * Return whether the queue is in a startable state
	 * 
	 * @return true if can be started.
	 */
	public boolean isStartable() {
		return this == INITIALISED || this == STOPPED;
	}

}
