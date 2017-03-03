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
package org.eclipse.scanning.event.queues.remote;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;

/**
 * Class to determine the current {@link Status} of a bean in an 
 * {@link IConsumer}. The search runs through both the submission queue and the
 * status set of the consumer and throws an error if the bean is not found.
 * 
 * @author Michael Wharmby
 *
 * @param <T> A bean extending {@link Queueable}
 */
public class BeanStatusFinder<T extends Queueable> {
	
	private static final long DEFAULT_TIMEOUT_MS = 5000;
	
	private final IConsumer<T> consumer;
	private final String beanID;
	private ExecutorService threadPool;
	
	private final CountDownLatch searchEndedLatch;
	private boolean beanFound = false;
	private Status foundStatus = null;

	/**
	 * Create a new status finder which will search for a bean with unique ID 
	 * beanID in the submission queue and status set of the given 
	 * {@link IConsumer}. Creates an {@link ExecutorService} to run the 
	 * searches.
	 * 
	 * @param beanID String unique ID of the bean to search for.
	 * @param consumer {@link IConsumer} to search for bean.
	 */
	public BeanStatusFinder(String beanID, IConsumer<T> consumer) {
		this.consumer = consumer;
		this.beanID = beanID;
		
		/*
		 * We will run two threads & use a latch to find out when both have 
		 * finished.
		 */
		threadPool = Executors.newFixedThreadPool(2);
		searchEndedLatch = new CountDownLatch(2);
	}
	
	/**
	 * Same behaviour as the find(long timeout) method, but uses the default 
	 * timeout hardcoded into the class to decide when the cancel the search.
	 * @return
	 * @throws EventException
	 */
	public Status find() throws EventException {
		return find(DEFAULT_TIMEOUT_MS);
	}
	
	/**
	 * Creates two {@link QueueSearcher} objects to locate the bean with the 
	 * given uniqueID in the submission queue or status set of the configured 
	 * {@link IConsumer}. A timeout (in ms) can be specified after which the 
	 * search is cancelled. If the bean is not found before the timeout or if 
	 * the bean is not found at all an exception is thrown to alert the caller. 
	 * If everything runs smoothly, the {@link Status} of the bean is returned. 
	 * 
	 * @param timeout long number of ms before search is cancelled.
	 * @return current {@link Status} of the requested bean.
	 * @throws EventException - if the search timed out or the bean was not 
	 *                          found.
	 */
	public Status find(long timeout) throws EventException {
		/*
		 * Interrogate both submission & status queues simultaneously.
		 */
		//This needs a latch to indicate completion...
		
		Future<?> statusSetSearch = threadPool.submit(new QueueSearcher(beanID, consumer.getStatusSet()));
		Future<?> submitQueueSearch = threadPool.submit(new QueueSearcher(beanID, consumer.getSubmissionQueue()));
		
		boolean latched;
		try {
			latched = searchEndedLatch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException iEx) {
			throw new EventException("Could not wait finding bean", iEx);
		} finally {
			//At this stage we can safely shutdown the executor.
			threadPool.shutdown();
		}
		if (!latched) {
			//Something went wrong, but was it an exception or just not found?
			try {
				statusSetSearch.get(1, TimeUnit.MILLISECONDS);
				submitQueueSearch.get(1, TimeUnit.MILLISECONDS);
				throw new EventException("Search timed out before finding bean. Contact your GDA representative.");
			} catch (Exception ex) {
				throw new EventException("Bean search encountered error(s)",ex);
			}
		}
		else if (foundStatus == null) throw new EventException("Bean not found");
		return foundStatus;
	}
	
	/**
	 * Runnable which searches through a list of beans looking for one with 
	 * the given unique ID. Called by the findStatus() in the outer class.
	 * 
	 * @author Michael Wharmby
	 *
	 */
	private class QueueSearcher implements Runnable {
		
		private final List<? extends Queueable> beanQueue;
		private final String beanID;
		
		private QueueSearcher(String beanID, List<? extends Queueable> beanQueue) {
			this.beanQueue = beanQueue;
			this.beanID = beanID;
		}

		@Override
		public void run() {
			for (Queueable queueItem : beanQueue) {
				if (queueItem.getUniqueId().equals(beanID)) {
					foundStatus = queueItem.getStatus();
					beanFound = true;
					break;
				}
				//In case another thread found our bean
				if (beanFound) break;
				
			}
			searchEndedLatch.countDown();
		}
		
	}
}
