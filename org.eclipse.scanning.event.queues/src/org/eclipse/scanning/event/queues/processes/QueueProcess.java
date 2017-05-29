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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.AbstractLockingPausableProcess;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueueBroadcaster;
import org.eclipse.scanning.api.event.queues.IQueueProcess;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent class for all {@link QueueService} queue processes. QueueProcess 
 * provides flow control to ensure post-match (i.e. after bean task) analysis 
 * is allowed to complete before other operations (e.g. termination) finish.
 * 
 * @author Michael Wharmby
 *
 * @param <Q> {@link Queueable} bean type to be operated on. 
 * @param <T> The {@link Queueable} specified by the {@link IConsumer} 
 *            instance using the IQueueProcess. This might be a 
 *            {@link QueueBean} or a {@link QueueAtom}. 
 */
public abstract class QueueProcess<Q extends Queueable, T extends Queueable> 
		extends AbstractLockingPausableProcess<T> implements IQueueProcess<Q, T>, IQueueBroadcaster<T> {
	
	private static Logger logger = LoggerFactory.getLogger(QueueProcess.class);
	
	protected final Q queueBean;
	protected boolean blocking = true, executed = false, terminated = false, finished = false;
	
	protected final CountDownLatch processLatch = new CountDownLatch(1);
	
	//Post-match analysis lock, ensures correct execution order of execute 
	//method & control (e.g. terminate) methods 
	protected final ReentrantLock postMatchAnalysisLock;
	protected final Condition analysisDone;
	
	@SuppressWarnings("unchecked")
	protected QueueProcess(T bean, IPublisher<T> publisher, Boolean blocking) throws EventException {
		super(bean, publisher);
		
		this.blocking = blocking;
		if (bean.getClass().equals(getBeanClass())) {
			this.queueBean = (Q)bean;
		} else {
			logger.error("Cannot set bean: Bean type "+bean.getClass().getSimpleName()+" not supported by "+getClass().getSimpleName()+".");
			throw new EventException("Unsupported bean type");
		}
		
		postMatchAnalysisLock = new ReentrantLock();
		analysisDone = postMatchAnalysisLock.newCondition();
	}
	
	@Override
	public void execute() throws EventException, InterruptedException {
		logger.info("Processing "+bean.getClass().getSimpleName()+": '"+bean.getName()+"'");
		executed = true;
		run();
		logger.debug("Waiting for processing of "+bean.getClass().getSimpleName()+" '"+bean.getName()+"' to complete...");
		processLatch.await();
		logger.debug("Post-match analysis of "+bean.getClass().getSimpleName()+" '"+bean.getName()+"' begins... (Status: "+bean.getStatus()+"; Percent: "+bean.getPercentComplete()+")");
		try {
			postMatchAnalysisLock.lockInterruptibly();
			postMatchAnalysis();
		} finally {
			//And we're done, so let other threads continue
			executionEnded();
			postMatchAnalysisLock.unlock();
		}
		logger.info("Processing of "+bean.getClass().getSimpleName()+" '"+bean.getName()+"' finished (Status: "+bean.getStatus()+"; Terminated? "+isTerminated()+"; Percent: "+bean.getPercentComplete()+")");
		
		/*
		 * Beans that have completed execution need to be broadcast. 
		 * N.B. Broadcasting needs to be done last; otherwise the next 
		 * queue may start when we're not ready.
		 */
		if (bean.getStatus().isFinal() || isTerminated()) {
			if (!isTerminated()) {
				//Terminated beans get broadcast by the AbstractPausibleLockingProcess, so we don't
				broadcast();
			}
		} else {
			System.err.println(isTerminated());
			logger.error(bean.getName()+" has a non-final state after processing complete (status="+bean.getStatus()+")");
			throw new EventException(bean.getName()+" has a non-final state after processing complete!");
		}
	}
	
	/**
	 * Performs the process described by the {@link Queueable} bean type to be 
	 * processed, using the configured parameters from the input bean.
	 * 
	 * @throws EventException in case of broadcast failure or in case of 
	 *         failure of {@link IEventService} infrastructure. Failures 
	 *         during proceessing should also be re-thrown as 
	 *         {@link EventExceptions}. 
	 * @throws InterruptedException if child run thread is interrupted
	 */
	protected abstract void run() throws EventException, InterruptedException;
	
	/**
	 * On completion of processing, determine the outcome - i.e. did 
	 * processing complete or fail in some way? Report back a message.
	 * 
	 * Final statuses should be set on the bean here and nowhere else. 
	 * 
	 * @throws EventException in case of broadcast failure.
	 * @throws InterruptedException if the analysis lock is interrupted
	 */
	protected abstract void postMatchAnalysis() throws EventException, InterruptedException;
	
	/*
	 * Method gets the postMatchAnalysis lock 
	 */
	@Override
	protected void doTerminate() throws EventException {
		if (finished) return; //Stops spurious messages/behaviour when processing already finished
		try {
			//Reentrant lock ensures execution method (and hence post-match 
			//analysis) completes before terminate does
			postMatchAnalysisLock.lockInterruptibly();
			
			logger.info("'"+bean.getName()+"' requested to terminate...");
			terminated = true;//<-- do this first, so specific actions can test we're terminated
			specificTerminateAction();
			logger.debug("Releasing processLatch to start post-match analysis");
			processLatch.countDown();
			
			//Wait for post-match analysis to finish
			continueIfExecutionEnded();
		} catch (InterruptedException iEx) {
			throw new EventException(iEx);
		} finally {
			postMatchAnalysisLock.unlock();
		}
	}
	
	/**
	 * Called from doTerminate, this method is called to perform the 
	 * termination actions. This is necessary so that doTerminate can lock out 
	 * other threads and will itself only complete when execution is complete.
	 * 
	 * @throws EventException in case termination fails.
	 */
	protected void specificTerminateAction() throws EventException {
		//Mostly we don't need to do anything
	}
	
	@Override
	public void updateBean(Status newStatus, Double newPercent, String newMessage) {
		if (newStatus != null) {
			bean.setPreviousStatus(bean.getStatus());
			bean.setStatus(newStatus);
		}
		if (newPercent != null) bean.setPercentComplete(newPercent);
		if (newMessage != null) bean.setMessage(newMessage);
		
		if ((newStatus == null) && (newPercent == null) && (newMessage == null)) {
			logger.warn("Bean updating prior to broadcast did not make any changes.");
		}
	}

	@Override
	public void broadcast(Status newStatus, Double newPercent, String newMessage) throws EventException {
		updateBean(newStatus, newPercent, newMessage);
		broadcast();
	}

	@Override
	public void broadcast() throws EventException {
		if (publisher != null) {
			publisher.broadcast(bean);
		}
	}

	@Override
	public boolean isExecuted() {
		return executed;
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}
	
	@Override
	public Q getQueueBean() {
		return queueBean;
	}
	
	/**
	 * Called at the end of post-match analysis to report the process finished
	 */
	protected void executionEnded() {
		finished = true;
		analysisDone.signal();
	}
	
	/**
	 * Called when we would need to wait if post-match analysis hasn't yet run.
	 * 
	 * @throws InterruptedException if wait is interrupted.
	 */
	protected void continueIfExecutionEnded() throws InterruptedException {
		if (finished) return;
		else {
			analysisDone.await();
		}
	}
	
	/**
	 * Get the latch controlling whether post-match analysis can be performed.
	 * 
	 * @return processLatch controlling released when run complete.
	 */
	public CountDownLatch getProcessLatch() {
		return processLatch;
	}

}
