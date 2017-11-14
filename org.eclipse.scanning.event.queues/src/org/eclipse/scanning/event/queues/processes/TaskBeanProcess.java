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
		//Do most of the work of processing in the atomQueueProcessor...
		atomQueueProcessor.run();
	}

	@Override
	public void postMatchCompleted() throws EventException {
		updateBean(Status.COMPLETE, 100d, "Scan completed.");
		atomQueueProcessor.tidyQueue();
	}

	@Override
	public void postMatchTerminated() throws EventException {
		atomQueueProcessor.terminate();
		queueBean.setMessage("Job-queue was requested to abort before completion");
		atomQueueProcessor.tidyQueue();
	}

	@Override
	public void postMatchFailed() throws EventException {
		updateBean(Status.FAILED, null, "Job-queue failed (caused by atom in queue)");
		logger.warn("Job-queue paused and will not continue without user intervention");
		//As we don't know the origin of the failure, pause *this* queue
		IQueueControllerService controller = ServicesHolder.getQueueControllerService();
		controller.pauseQueue(ServicesHolder.getQueueService().getJobQueueID());
		atomQueueProcessor.tidyQueue();
	}

	@Override
	protected void doPause() throws Exception {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO!
		logger.warn("Pause/resume not implemented on TaskBeanProcessor");
	}

	@Override
	protected void doResume() throws Exception {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO!
		logger.warn("Pause/resume not implemented on TaskBeanProcessor");
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
