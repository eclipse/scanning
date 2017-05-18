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
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaskBeanProcess uses an {@link AtomQueueProcessor} to read the 
 * {@link SubTaskAtom}s in a {@link TaskBean} and form them into an 
 * active-queue.
 * 
 * It differs from the {@link SubTaskAtomProcess} only in its failure 
 * behaviour, when it is configured to send a {@link PauseBean} to its 
 * consumer (i.e. the job-queue consumer) to prevent any more {@link TaskBean}s
 * being consumed.
 * 
 * @author Michael Wharmby
 *
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using this TaskBeanProcess. This will be 
 *            {@link QueueBean}.
 */
public class TaskBeanProcess<T extends Queueable> extends QueueProcess<TaskBean, T> {
	
	private static Logger logger = LoggerFactory.getLogger(TaskBeanProcess.class);
	/*
	 * Used by {@link QueueProcessFactory} to identify the bean type this 
	 * {@link QueueProcess} handles.
	 */
	public static final String BEAN_CLASS_NAME = TaskBean.class.getName();
	
	private AtomQueueProcessor<TaskBean, SubTaskAtom, T> atomQueueProcessor;
	
	public TaskBeanProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
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
		//...do the post-match analysis in this class!
		try {
			postMatchAnalysisLock.lockInterruptibly();
			if (isTerminated()) {
				atomQueueProcessor.terminate();
				logger.debug(bean.getName()+" was aborted request to abort");
				queueBean.setMessage(bean.getName()+" was aborted before completion (requested)");
			}else if (queueBean.getPercentComplete() >= 99.49) {//99.49 to catch rounding errors
				//Completed successfully
				updateBean(Status.COMPLETE, 100d, "Scan completed.");
			} else {
				//Failed: latch released before completion
				updateBean(Status.FAILED, null, bean.getName()+" failed");
				logger.info(bean.getName()+" failed. Job-queue paused and will not continue without user intervention");
				//As we don't know the origin of the failure, pause *this* queue
				IQueueControllerService controller = ServicesHolder.getQueueControllerService();
				controller.pauseQueue(ServicesHolder.getQueueService().getJobQueueID());
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
	public void doTerminate() throws EventException {
		try {
			//Reentrant lock ensures execution method (and hence post-match 
			//analysis) completes before terminate does
			postMatchAnalysisLock.lockInterruptibly();
			
			terminated = true;
			logger.debug("Terminate requested; release processLatch (start post-match analysis)");
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
		logger.warn("Pause not implemented on TaskBeanProcessor");
	}
	
	@Override
	protected void doResume() throws Exception {
		//TODO!
		logger.warn("Resume not implemented on TaskBeanProcessor");
	}
	
	@Override
	public Class<TaskBean> getBeanClass() {
		return TaskBean.class;
	}

	/*
	 * For tests 
	 */
	public AtomQueueProcessor<TaskBean, SubTaskAtom, T> getAtomQueueProcessor() {
		return atomQueueProcessor;
	}

}
