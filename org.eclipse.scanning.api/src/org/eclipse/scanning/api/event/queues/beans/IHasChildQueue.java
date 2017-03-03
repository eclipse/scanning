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
package org.eclipse.scanning.api.event.queues.beans;

/**
 * Interface allowing messages which are specific to the behaviour & operation
 * of the queue to be passed through the queue hierarchy.
 * 
 * @author Michael Wharmby
 *
 */
public interface IHasChildQueue {
	
	/**
	 * Get the string reporting changes in the child queue owned by this 
	 * atom/bean.
	 * 
	 * @return String report of child queue state.
	 */
	public String getQueueMessage();
	
	/**
	 * Set the string reporting changes in the child queue owned by this 
	 * atom/bean.
	 * 
	 * @param String report of child queue state.
	 */
	public void setQueueMessage(String msg);

}
