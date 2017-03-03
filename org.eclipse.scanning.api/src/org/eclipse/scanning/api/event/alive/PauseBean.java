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
package org.eclipse.scanning.api.event.alive;

/**
 * Used to pause the consumer. This does not pause the running process, if there is one.
 * It stops the consumer consuming more jobs and running them. 
 * 
 * A pause is required internally before attempting to reorder queues to avoid collisions.
 * An API will be put over reordering so that consumers are paused and blocked before the
 * submit queue is edited. 
 * 
 * @author Matthew Gerring
 *
 */
public class PauseBean extends ConsumerCommandBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1152606459228271989L;
	
	private boolean pause = true;
	
	public PauseBean() {
		super();
	}
	
	public PauseBean(String queueName) {
		this();
		setQueueName(queueName);
	}

	public boolean isPause() {
		return pause;
	}

	public void setPause(boolean pause) {
		this.pause = pause;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (pause ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PauseBean other = (PauseBean) obj;
		if (pause != other.pause)
			return false;
		return true;
	}
}
