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
package org.eclipse.scanning.api.event.dry;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.AbstractLockingPausableProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;

public class DryRunProcess<T extends StatusBean> extends AbstractLockingPausableProcess<T> {

	
	private boolean blocking;
	private boolean terminated;

	private int stop;
	private int start;
	private int step;
	private long sleep;

	private Thread thread;

	public DryRunProcess(T bean, IPublisher<T> statusPublisher, boolean blocking) {
		this(bean,statusPublisher,blocking,0,100,1,100);
	}
	public DryRunProcess(T bean, IPublisher<T> statusPublisher, boolean blocking, int start, int stop, int step, long sleep) {
		super(bean, statusPublisher);
		this.blocking  = blocking;
		this.start = start;
		this.stop  = stop;
		this.step  = step;
		this.sleep  = sleep;
	}

	@Override
	public void execute() throws EventException {
		if (isBlocking()) {
			run(); // Block until process has run.
		} else {
			final Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						DryRunProcess.this.run();
					} catch (EventException ne) {
						System.out.println("Cannot complete dry run");
						ne.printStackTrace();
					}
				}
			});
			thread.setDaemon(true);
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		}		
	}
	
	private void run()  throws EventException {
		
		this.thread = Thread.currentThread();
		getBean().setPreviousStatus(Status.QUEUED);
		getBean().setStatus(Status.RUNNING);
		getBean().setPercentComplete(0d);
		getPublisher().broadcast(getBean());

		terminated = false;
		for (int i = start; i <= stop; i+=step) {
			
			if (isTerminated()) {
				getBean().setPreviousStatus(Status.RUNNING);
				getBean().setStatus(Status.TERMINATED);
				getPublisher().broadcast(getBean());
				return;
			}
			
			//Moved broadcast to before sleep to prevent another spurious broadcast.
			//System.out.println("Dry run : "+getBean().getPercentComplete()+" : "+getBean().getName());
			getBean().setPercentComplete((Double.valueOf(i)/Double.valueOf(stop))*100d);
			getPublisher().broadcast(getBean());
			
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				System.out.println("Cannot complete dry run");
				e.printStackTrace();
			}
			
			//This must happen after the broadcast, otherwise we get spurious messages sent on termination.
			checkPaused(); // Blocks if is, sends events
		}

		getBean().setPreviousStatus(Status.RUNNING);
		getBean().setStatus(Status.COMPLETE);
		getBean().setPercentComplete(100);
		getBean().setMessage("Dry run complete (no software run)");
		getPublisher().broadcast(getBean());
	}

	@Override
	public void doTerminate() throws EventException {
		if (thread!=null) thread.interrupt();
		terminated = true;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}

}
