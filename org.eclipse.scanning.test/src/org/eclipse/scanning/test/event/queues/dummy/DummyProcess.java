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

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.processes.QueueProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DummyProcess<Q extends Queueable, T extends Queueable> extends QueueProcess<Q, T> {
	
	private static final Logger logger = LoggerFactory.getLogger(DummyBeanProcess.class);

	protected DummyProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
	}

	@Override
	public void execute() throws EventException {
		executed = true; 
		broadcast(Status.RUNNING, 0d);

		for (int i = 0; i < 10; i++) {
			if (isTerminated()) {
				broadcast(Status.TERMINATED);
				return;
			}
			logger.debug("DummyProcessor ("+queueBean.getClass().getSimpleName()+" - "+queueBean.getName()+"): "+queueBean.getPercentComplete());
			System.out.println("DummyProcessor ("+queueBean.getClass().getSimpleName()+" - "+queueBean.getName()+"): "+queueBean.getPercentComplete());
			broadcast(new Double(i*10));

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				logger.error("Dummy process sleeping failed", e);
				throw new EventException(e);
			}
		}
		broadcast(Status.COMPLETE, 100d, "Dummy process complete (no software run)");
	}
	
	@Override
	protected void run() throws EventException, InterruptedException {
		//Do nothing - this is not needed for Dummy processing
	}

	@Override
	protected void postMatchAnalysis() throws EventException, InterruptedException {
		//Do nothing - this is not needed for Dummy processing
		
	}
	
	@Override
	public void doTerminate() throws EventException {
		terminated = true;
	}

}
