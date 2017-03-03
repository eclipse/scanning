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

import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * Process used in the queue service to run the instructions provided by a 
 * {@link Queueable} object. This interface provides a coarse route to identify
 * how complete the process is and also the bean class that will be operated on
 * (Q). To  complete the implementation an additional static string is required:
 * <pre>
 * public static final String BEAN_CLASS_NAME = ...
 * </pre>
 * 
 * @author Michael Wharmby
 *
 * @param <Q> Bean type that will be operated on
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using the IQueueProcess. This might be a 
 *            {@link QueueBean} or a {@link QueueAtom}.
 */
public interface IQueueProcess <Q extends Queueable, T extends Queueable> extends IConsumerProcess<T> {

	/**
	 * Returns the bean which will be operated on by this process.
	 * 
	 * @return P bean to be processed.
	 */
	public Q getQueueBean();
	
	/**
	 * Return whether execution has begun.
	 * 
	 * @return true if execution begun.
	 */
	public boolean isExecuted();

	/**
	 * Return whether the process has been terminated.
	 * 
	 * @return true if has been terminated.
	 */
	public boolean isTerminated();
	
	/**
	 * Return the class of the bean which this IQueueProcessor can process.
	 * 
	 * @return Class of bean which can be processed.
	 */
	public Class<Q> getBeanClass();

}
