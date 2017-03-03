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

import org.eclipse.scanning.api.event.queues.beans.QueueBean;

/**
 * Class to mock behaviour of a generic QueueBean. This can be processed only
 * within a job-queue.
 * 
 * @author Michael Wharmby
 *
 */
public class DummyBean extends QueueBean {
	
	public DummyBean() {
		super();
	}
	
	public DummyBean(String name, long time) {
		super();
		setName(name);
		runTime = time;
	}
	
}
