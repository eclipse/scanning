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

import org.eclipse.scanning.api.event.EventException;


/**
 * A consumer process is the process run when a consumer starts a job.
 * ProgressibleProcess in the DAWN command server plugin is the inspiration for this plugin.
 * 
 * @author Matthew Gerring
 *
 */
public interface IConsumerProcess<T> extends IPublishable<T>{

	
	/**
	 * Execute the process, if an exception is thrown the process is set to 
	 * failed and the message is the message of the exception.
	 * 
	 * This is the blocking method to run the process. 
	 * The start method will be called by the consumer running the process
	 * and by default terminate is called in the same thread. If blocking is
	 * set to 
	 * 
	 * @throws Exception
	 */
	void execute() throws EventException, InterruptedException;
	
	/**
	 * If the process is non-blocking this method will start a thread
	 * which calls execute (and the method will return).
	 * 
	 * By default a process blocks until it is done. isBlocking() and start()
	 * may be overridden to redefine this.
	 * 
	 * @throws EventException
	 */
	default void start() throws EventException, InterruptedException {
		
		if (isBlocking()) {
			execute(); // Block until process has run.
		} else {
			final Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						execute();
					} catch (EventException | InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}, "Run "+getBean());
			thread.setDaemon(true);
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		}
	}
	
	default boolean isBlocking() {
		return true;
	}
	
	/**
	 * Please provide a termination for the process by implementing this method.
	 * If the process has a stop file, write it now; if it needs to be killed,
	 * get its pid and kill it; if it is running on a cluster, use the qdel or dramaa api.
	 * 
	 * @throws Exception
	 */
	void terminate() throws EventException;

	/**
	 * Call to pause the running process.
	 */
	default void pause() throws EventException {
		
	}
	
	/**
	 * Call to resume the process.
	 */
	default void resume() throws EventException {

	}

	/**
	 * Should be implemented to provide paused status.
	 * If using AbstractPausibleProcess, it will be automatically.
	 * @return
	 */
	default boolean isPaused() {
		return false;
	}
}
