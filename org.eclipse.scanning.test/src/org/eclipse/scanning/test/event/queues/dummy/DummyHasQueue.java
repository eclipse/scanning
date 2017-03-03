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
package org.eclipse.scanning.test.event.queues.dummy;

import org.eclipse.scanning.api.event.queues.beans.IHasChildQueue;
import org.eclipse.scanning.api.event.queues.beans.Queueable;

/**
 * Generic class to mock behaviour of a POJO in a Queue. Has an additional 
 * queue message option for testing a second message field
 * 
 * @author Michael Wharmby
 *
 */
public class DummyHasQueue extends Queueable implements IHasChildQueue {
	
	private String queueMessage;
	
	public DummyHasQueue() {
		super();
	}
	
	public DummyHasQueue(String name, long time) {
		super();
		setName(name);
		runTime = time;
	}

	public String getQueueMessage() {
		return queueMessage;
	}

	public void setQueueMessage(String queueMessage) {
		this.queueMessage = queueMessage;
	}
	
	public void merge(DummyHasQueue with) {
		super.merge(with);
		this.queueMessage = with.queueMessage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((queueMessage == null) ? 0 : queueMessage.hashCode());
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
		DummyHasQueue other = (DummyHasQueue) obj;
		if (queueMessage == null) {
			if (other.queueMessage != null)
				return false;
		} else if (!queueMessage.equals(other.queueMessage))
			return false;
		return true;
	}
	
	

}
