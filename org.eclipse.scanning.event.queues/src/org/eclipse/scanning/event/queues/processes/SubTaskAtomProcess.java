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
package org.eclipse.scanning.event.queues.processes;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueProcessFactory;

/**
 * SubTaskAtomProcess uses an {@link AtomQueueProcessor} to read the 
 * {@link QueueAtom}s in a {@link SubTaskAtom} and form them into an 
 * active-queue.
 * 
 * It differs from the {@link TaskBeanProcess} only in its failure 
 * behaviour, passing control back to it's parent.
 * 
 * @author Michael Wharmby
 *
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using this SubTaskAtomProcess. This will be 
 *            {@link QueueAtom}.
 */
public class SubTaskAtomProcess<T extends Queueable> extends QueueProcess<SubTaskAtom, T> {
	
	/**
	 * Used by {@link QueueProcessFactory} to identify the bean type this 
	 * {@link QueueProcess} handles.
	 */
	public static final String BEAN_CLASS_NAME = SubTaskAtom.class.getName();
	
	private AtomQueueProcessor<SubTaskAtom, QueueAtom, T> atomQueueProcessor;
	
	public SubTaskAtomProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher, blocking);
		atomQueueProcessor = new AtomQueueProcessor<>(this);
	}

	@Override
	protected void run() throws EventException, InterruptedException {
		executed = true;
		//Do most of the work of processing in the atomQueueProcessor...
		atomQueueProcessor.run();
	}
	
	@Override 
	protected void postMatchAnalysis() throws EventException, InterruptedException {
		//...do the post-match analysis in here!
		try {
			postMatchAnalysisLock.lockInterruptibly();
			if (isTerminated()) {
				atomQueueProcessor.terminate();
				queueBean.setMessage("Active-queue aborted before completion (requested)");
			}else if (queueBean.getPercentComplete() >= 99.49) {//99.49 to catch rounding errors
				//Completed successfully
				updateBean(Status.COMPLETE, 100d, "Scan completed.");
			} else {
				//Failed: latch released before completion
				updateBean(Status.FAILED, null, "Active-queue failed (caused by process Atom)");
			}
		} finally {
			//This should be run after we've reported the queue final state
			//This must be after unlock call, otherwise terminate gets stuck.
			atomQueueProcessor.tidyQueue();
			
			//And we're done, so let other processes continue
			executionEnded();
			
			postMatchAnalysisLock.unlock();

			/*
			 * N.B. Broadcasting needs to be done last; otherwise the next 
			 * queue may start when we're not ready. Broadcasting should not 
			 * happen if we've been terminated.
			 */
			if (!isTerminated()) {
				broadcast();
			}
		}
	}
	
	@Override
	protected void doTerminate() throws EventException {
		try {
			//Reentrant lock ensures execution method (and hence post-match 
			//analysis) completes before terminate does
			postMatchAnalysisLock.lockInterruptibly();

			terminated = true;
			processLatch.countDown();
			//Wait for post-match analysis to finish
			continueIfExecutionEnded();
		} catch (InterruptedException iEx) {
			throw new EventException(iEx);
		} finally {
			postMatchAnalysisLock.unlock();
		}
	}
	
	@Override
	protected void doPause() throws Exception {
		//TODO!
	}
	
	@Override
	protected void doResume() throws Exception {
		//TODO!
	}

	@Override
	public Class<SubTaskAtom> getBeanClass() {
		return SubTaskAtom.class;
	}
	
	/*
	 * For tests 
	 */
	public AtomQueueProcessor<SubTaskAtom, QueueAtom, T> getAtomQueueProcessor() {
		return atomQueueProcessor;
	}

}
