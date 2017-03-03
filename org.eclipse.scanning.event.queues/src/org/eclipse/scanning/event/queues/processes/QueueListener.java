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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.queues.IQueueBroadcaster;
import org.eclipse.scanning.api.event.queues.beans.IHasAtomQueue;
import org.eclipse.scanning.api.event.queues.beans.IHasChildQueue;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueueListener provides a bridge between an atom which creates a queue (e.g. 
 * TaskBean, SubTaskAtom, ScanAtom) and its dependent queue. When an event in 
 * the child queue causes the listener to fire, it  reads the {@link Status} 
 * and percent complete of the bean causing the event and updates the 
 * parent bean appropriately.
 * 
 * The QueueListener is used in the ScanAtomProcess and also in the 
 * AtomQueueProcessor.
 * 
 * @author Michael Wharmby
 *
 * @param <Q> Bean extending {@link StatusBean} from the child queue.
 * @param <T> Bean extending {@link Queueable}, the parent queue atom.
 */
//TODO Can we update the broadcast mechanism to accept queuemessage updates too?
public class QueueListener<P extends Queueable, Q extends StatusBean> implements IBeanListener<Q> {
	
	private static Logger logger = LoggerFactory.getLogger(QueueListener.class);
	
	//Infrastructure
	private final IQueueBroadcaster<? extends Queueable> broadcaster;
	private final CountDownLatch processLatch;
	
	//
	private P parent;
	private double initPercent;
	private boolean firstTime = true;
	private Map<String, ProcessStatus> children = new HashMap<>();
	private boolean childCommand;
	private final double queueCompletePercentage = 99.5;
	
	private QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch procLatch, boolean fakeArg) {
		this.broadcaster = broadcaster;
		this.parent = parent;
		processLatch = procLatch;
	}
	
	@SuppressWarnings("unchecked")
	public QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch procLatch) throws EventException {
		this(broadcaster, parent, procLatch, true);
		if (parent instanceof IHasAtomQueue<?>) {
			List<?> children = ((IHasAtomQueue<?>)parent).getAtomQueue();
			initChildList((List<Q>) children);//QueueAtom extends StatusBean, so this cast is OK.
		} else {
			throw new EventException("Listener has no child beans to monitor. Cannot continue.");
		}
	}
	
	public QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch procLatch, Q child) {
		this(broadcaster, parent, procLatch, true);
		children.put(child.getUniqueId(), new ProcessStatus(child));
	}
	
	public QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch procLatch, List<Q> children) {
		this(broadcaster, parent, procLatch, true);
		initChildList(children);
	}
	
	private void initChildList(List<Q> children) {
		double childWork, totalWork = 0d;
		for (Q child : children) {
			String childID = child.getUniqueId();
			this.children.put(childID, new ProcessStatus(child));
			
			/*
			 * Record the runtime of each child, if it is an instance of 
			 * Queueable. If not, assume each child does an equal amount 
			 * of work (this is set in the inner class {@see ProcessStatus}).
			 */
			if (child instanceof Queueable) {
				childWork = new Double(((Queueable)child).getRunTime());
				totalWork = totalWork + childWork;
				this.children.get(childID).setWorkFraction(childWork);
			}
		}
		/*
		 * Normalise the amount of work done per child.
		 */
		for(String childID : this.children.keySet()) {
			childWork = this.children.get(childID).getWorkFraction();
			this.children.get(childID).setWorkFraction(childWork/totalWork);
		}	
	}

	@Override
	public void beanChangePerformed(BeanEvent<Q> evt) {
		boolean broadcastUpdate = false, beanCompleted = false, failed = false;
		Q bean = evt.getBean();
		String beanID = bean.getUniqueId();
		//If this bean is not from the parent ignore it.
		if (!children.containsKey(beanID)) return;
		
		if (!children.get(beanID).isOperating()) { 
			if (bean.getStatus().isRunning()) {
				children.get(beanID).setOperating(true);
			} else {
				//The bean should be running if it's operating. Something's wrong.
				logger.warn("'"+bean.getName()+"' is not set to operating, but is running (Status = "+bean.getStatus()+").");	
			}
		}
		
		//Whatever happens update the message on the parent
		parent.setMessage("'"+bean.getName()+"': "+bean.getMessage());
		
		//The percent complete changed, update the parent
		if (bean.getPercentComplete() != children.get(beanID).getPercentComplete()) {
			//First time we need to change the parent percent, get its initial value
			if (firstTime) {
				initPercent = parent.getPercentComplete();
				firstTime = false;
			}
			double childPercent = bean.getPercentComplete();
			double childContribution = (queueCompletePercentage - initPercent) * children.get(beanID).getWorkFraction();
			double parentPercent = parent.getPercentComplete() + childContribution / 100 * (childPercent - children.get(beanID).getPercentComplete());
			parent.setPercentComplete(parentPercent);
			children.get(beanID).setPercentComplete(childPercent);
			broadcastUpdate = true;
		}
		
		/*
		 * If the status of the child changed, test if we need to update the
		 * parent. Transitions handled: (for each, set queue message)
		 * -> PAUSED (from elsewhere): REQUEST_PAUSE parent
		 * -> TERMINATED (from elsewhere): REQUEST_TERMINATE parent
		 * -> COMPLETE
		 * -> RESUMED/RUNNING from PAUSED: REQUEST_RESUME
		 * -> FAILED: FAILED (N.B. for TaskBean, consumer will pause on failure)
		 */
		if (bean.getStatus() != children.get(beanID).getStatus()) {
			//Update the status of the process
			children.get(beanID).setStatus(bean.getStatus());
			
			if (bean.getStatus().isRunning() || bean.getStatus().isResumed()) {
				//RESUMED/RUNNING
				if (parent.getStatus().isPaused()) {
					// -parent is paused => unpause it
					parent.setStatus(Status.REQUEST_RESUME);
					((IHasChildQueue)parent).setQueueMessage("Resume requested from '"+bean.getName()+"'");
					childCommand = true;
					broadcastUpdate = true;
				} else {
					// -DEFAULT for normal running
					((IHasChildQueue)parent).setQueueMessage("Running...");
					childCommand = false;
					broadcastUpdate = true;
				}
			} else if (bean.getStatus().isPaused()) {
				//PAUSE
				parent.setStatus(Status.REQUEST_PAUSE);
				((IHasChildQueue)parent).setQueueMessage("Pause requested from '"+bean.getName()+"'");
				childCommand = true;
				broadcastUpdate = true;
			} else if (bean.getStatus().isTerminated()) {
				//TERMINATE
				parent.setStatus(Status.REQUEST_TERMINATE);
				((IHasChildQueue)parent).setQueueMessage("Termination requested from '"+bean.getName()+"'");
				childCommand = true;
				broadcastUpdate = true;
			} else if (bean.getStatus().isFinal()) {
				//FINAL states
				children.get(beanID).setOperating(false);
				beanCompleted = true;
				if (bean.getStatus().equals(Status.COMPLETE)) {
					((IHasChildQueue)parent).setQueueMessage("'"+bean.getName()+"' completed successfully.");
					broadcastUpdate = true;
				} else {
					//Status.FAILED or unhandled state
					((IHasChildQueue)parent).setQueueMessage("Failure caused by '"+bean.getName()+"'");
					broadcastUpdate = true;
					childCommand = true;
					failed = true;
				}
			}
		}
		
		//If we have an update to broadcast, do it!
		if (broadcastUpdate) {
			try {
				broadcaster.broadcast();
			} catch (EventException evEx) {
				logger.error("Broadcasting '"+bean.getName()+"' failed with: "+evEx.getMessage());
			}
		}
		
		/*
		 * If no beans are still operating and all beans have concluded, 
		 * release the latch.
		 */
		if (beanCompleted) {
			boolean concluded = true, operating = false;
			for (String childID : children.keySet()) {
				concluded = concluded && children.get(childID).isConcluded();
				operating = operating || children.get(childID).isOperating();
			}
			if (concluded && !operating || failed) {
				if (!failed) {
					parent.setMessage("Running finished.");
					((IHasChildQueue)parent).setQueueMessage("All child processes complete.");
				}
				try {
					broadcaster.broadcast();
				} catch (EventException evEx) {
					logger.error("Broadcasting completed message failed with: "+evEx.getMessage());
				}
				processLatch.countDown();
			}
		}
	}
	
	/**
	 * Mark the last command status change of parent as resulting from a 
	 * command from  a child process (to prevent instruction loops).
	 * 
	 * @return true if last command to parent came from a child.
	 */
	public boolean isChildCommand() {
		return childCommand;
	}
	
	/**
	 * Records the state of a process within a monitored consumer queue, as 
	 * viewed by the {@link QueueListener}.
	 * 
	 * @author Michael Wharmby
	 *
	 */
	private class ProcessStatus {
		
		private Status status;
		private double percentComplete;
		private double workFraction = 1d;
		private boolean operating = false;
		
		/**
		 * Create ProcessStatus from the bean describing the process in the 
		 * queue.
		 * 
		 * @param bean extending StatusBean which will be used to update 
		 *        this object.
		 */
		public ProcessStatus(StatusBean bean) {
			status = bean.getStatus();
			percentComplete = bean.getPercentComplete();
		}

		/**
		 * Returns the percentage completeness of this process.
		 * 
		 * @return double representing the current percent complete.
		 */
		public double getPercentComplete() {
			return percentComplete;
		}
		
		/**
		 * Update the currently stored percentage complete.
		 * 
		 * @param percentComplete a double representing the new completeness.
		 */
		public void setPercentComplete(double percentComplete) {
			this.percentComplete = percentComplete;
		}
		
		/**
		 * Returns the fraction of runtime/processes of the parent queue that 
		 * this process represents. 
		 * 
		 * @return double representing fraction of work.
		 */
		public double getWorkFraction() {
			return workFraction;
		}
		
		/**
		 * Set the fraction of runtime/processes of the parent queue this 
		 * process represents. This should be called once and before processing
		 *  of the bean starts.
		 * 
		 * @param workFraction double representing fraction of work.
		 */
		public void setWorkFraction(double workFraction) {
			this.workFraction = workFraction;
		}
		
		/**
		 * Report the {@link Status} of the process as reported by the bean.
		 * 
		 * @return {@link Status} of the process.
		 */
		public Status getStatus() {
			return status;
		}
		
		/**
		 * Update the {@link Status} of the process as the process continues.
		 * 
		 * @param status new {@link Status} of the process.
		 */
		public void setStatus(Status status) {
			this.status = status;
		}
		
		/**
		 * Has execution of this process commenced yet?
		 * 
		 * @return true if the process is being executed.
		 */
		public boolean isOperating() {
			return operating;
		}
		
		/**
		 * Update whether this process is being executed.
		 * 
		 * @param operating boolean indicating whether execution is happening. 
		 */
		public void setOperating(boolean operating) {
			this.operating = operating;
		}
		
		/**
		 * Test whether the process has actually been started (NONE is being 
		 * used to indicate nothing done yet).
		 * @return true if process state is final & is NONE
		 */
		public boolean isConcluded() {
			return status.isFinal() && status != Status.NONE;
		}

		@Override
		public String toString() {
			return "ProcessStatus [percentComplete=" + percentComplete + ", workFraction=" + workFraction + ", status="
					+ status + ", active=" + operating + "]";
		}
	}

}
