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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


	private static Logger logger = LoggerFactory.getLogger(SubTaskAtomProcess.class);
	/*
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
		queueBean.setMessage("Active-queue was requested to abort before completion");
//TODO		logger.debug("'"+bean.getName()+"' was requested to abort");
		atomQueueProcessor.tidyQueue();
	}

	@Override
	public void postMatchFailed() throws EventException {
		queueBean.setMessage("Active-queue failed (caused by atom in queue)");
		atomQueueProcessor.tidyQueue();
	}

	@Override
	protected void doPause() throws Exception {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO!
		logger.warn("Pause/resume not implemented on SubTaskAtom");
	}

	@Override
	protected void doResume() throws Exception {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		//TODO!
		logger.warn("Pause/resume not implemented on SubTaskAtom");
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
