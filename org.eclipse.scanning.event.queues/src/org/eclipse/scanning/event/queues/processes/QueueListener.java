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
	
	/**
	 * Create QueueListener with child atoms to listen for specified by the 
	 * atomQueue of the given parent.
	 * 
	 * @param broadcaster {@link IQueueBroadcaster} - typically parent process.
	 * @param parent {@link IHasAtomQueue} bean which specifies atoms to 
	 *        listen for.
	 * @param processLatch {@link CountDownLatch} to be released when beans 
	 *        complete.
	 * @throws EventException if
	 */
	@SuppressWarnings("unchecked")
	public QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch processLatch) throws EventException {
		this.broadcaster = broadcaster;
		this.parent = parent;
		this.processLatch = processLatch;
		
		if (parent instanceof IHasAtomQueue<?>) {
			List<?> children = ((IHasAtomQueue<?>)parent).getAtomQueue();
			initChildList((List<Q>) children);//QueueAtom extends StatusBean, so this cast is OK.
		} else {
			logger.error("Given parent bean ('"+parent.getName()+"') doesn't have atomQueue to get child atoms from.");
			throw new EventException("Given parent bean has no atomQueue");
		}
		logger.debug("Initialised from atomQueue of '"+parent.getName()+"' ("+children.size()+" atoms)");
	}
	
	/**
	 * Create QueueListener with a single given child atom to listen for (explicitly declared).
	 * 
	 * @param broadcaster
	 * @param parent
	 * @param processLatch
	 * @param child
	 */
	public QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch processLatch, Q child) {
		this.broadcaster = broadcaster;
		this.parent = parent;
		this.processLatch = processLatch;
		
		children.put(child.getUniqueId(), new ProcessStatus(child));
		logger.debug("Initialised with "+parent.getClass().getSimpleName()+" '"+parent.getName()+"' and 1 atom ("+child.getClass().getSimpleName()+": '"+child.getName()+"') explicitly declared");
	}
	
	/**
	 * Used in tests
	 */
	public QueueListener(IQueueBroadcaster<? extends Queueable> broadcaster, P parent, CountDownLatch processLatch, List<Q> children) {
		this.broadcaster = broadcaster;
		this.parent = parent;
		this.processLatch = processLatch;
		
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
				if (childWork > 0) {
					this.children.get(childID).workFraction = childWork;
				}
			}
			totalWork = totalWork + this.children.get(childID).workFraction;
		}
		/*
		 * Normalise the amount of work done per child.
		 */
		for(String childID : this.children.keySet()) {
			childWork = this.children.get(childID).workFraction;
			this.children.get(childID).workFraction = childWork/totalWork;
		}	
	}

	@Override
	public void beanChangePerformed(BeanEvent<Q> evt) {
		boolean broadcastUpdate = false, beanCompleted = false, failed = false;
		Q bean = evt.getBean();
		String beanID = bean.getUniqueId();
		//If this bean is not from the parent ignore it.
		if (!children.containsKey(beanID)) return;
		
		if (!children.get(beanID).operating) {
			/*
			 * We're not interested in SUBMITTED or QUEUED - nothing should be happening;
			 * likewise, REQUEST_* might be a legitimate call, so we just allow them past, 
			 * without setting our bean operating.
			 */
			if (bean.getStatus().isActive()) {
				children.get(beanID).operating = true;
			} else if (bean.getStatus().isFinal()) {
				//Bean should only broadcast a final status if it's operating
				logger.warn(bean.getClass().getSimpleName()+" '"+bean.getName()+"' is not set to operating, but is broadcasting (Status="+bean.getStatus()+")");	
			}
		}
		
		//Whatever happens update the message on the parent
		parent.setMessage("'"+bean.getName()+"': "+bean.getMessage());
		
		//The percent complete changed, update the parent
		if (bean.getPercentComplete() != children.get(beanID).percentComplete) {
			//First time we need to change the parent percent, get its initial value
			if (firstTime) {
				initPercent = parent.getPercentComplete();
				firstTime = false;
			}
			double childPercent = bean.getPercentComplete();
			double childContribution = (queueCompletePercentage - initPercent) * children.get(beanID).workFraction;
			double parentPercent = parent.getPercentComplete() + childContribution / 100 * (childPercent - children.get(beanID).percentComplete);
			parent.setPercentComplete(parentPercent);
			children.get(beanID).percentComplete = childPercent;
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
		if (bean.getStatus() != children.get(beanID).status) {
			//Update the status of the process
			children.get(beanID).status = bean.getStatus();
			
			if (bean.getStatus().isRunning() || bean.getStatus().isResumed()) {
				//RESUMED/RUNNING
				if (parent.getStatus().isPaused()) {
					// -parent is paused => unpause it
					parent.setStatus(Status.REQUEST_RESUME);
					logger.info(bean.getClass().getSimpleName()+" '"+bean.getName()+"' in queue requested to resume");
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
				logger.info(bean.getClass().getSimpleName()+" '"+bean.getName()+"' in queue requested to pause");
				((IHasChildQueue)parent).setQueueMessage("Pause requested from '"+bean.getName()+"'");
				childCommand = true;
				broadcastUpdate = true;
			} else if (bean.getStatus().isTerminated()) {
				//TERMINATE
				parent.setStatus(Status.REQUEST_TERMINATE);
				logger.info(bean.getClass().getSimpleName()+" '"+bean.getName()+"' in queue requested to terminate");
				((IHasChildQueue)parent).setQueueMessage("Termination requested from '"+bean.getName()+"'");
				childCommand = true;
				broadcastUpdate = true;
			} else if (bean.getStatus().isFinal()) {
				//FINAL states
				children.get(beanID).operating = false;
				beanCompleted = true;
				if (bean.getStatus().equals(Status.COMPLETE)) {
					logger.info(bean.getClass().getSimpleName()+" '"+bean.getName()+"' in queue completed successfully");
					((IHasChildQueue)parent).setQueueMessage("'"+bean.getName()+"' completed successfully.");
					broadcastUpdate = true;
				} else {
					//Status.FAILED or unhandled state
					logger.info(bean.getClass().getSimpleName()+" '"+bean.getName()+"' in queue failed (message: "+bean.getMessage()+")");
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
			boolean allBeansFinished = true, anyBeansOperating = false;
			for (String childID : children.keySet()) {
				allBeansFinished = allBeansFinished && children.get(childID).isFinished();
				anyBeansOperating = anyBeansOperating || children.get(childID).operating;
			}
			logger.debug("Status of all child beans of "+parent.getClass().getSimpleName()+" '"+parent.getName()+"': allBeansFinished="+allBeansFinished+"  anyBeansOperating="+anyBeansOperating);
			if (allBeansFinished && !anyBeansOperating || failed) {
				if (!failed) {
					parent.setMessage("Atom queue completed");
					((IHasChildQueue)parent).setQueueMessage("All child queue beans completed successfully");
				}
				try {
					broadcaster.broadcast();
				} catch (EventException evEx) {
					logger.error("Broadcasting completed message failed with: "+evEx.getMessage());
				}
				logger.debug("Releasing parent bean process latch... ("+parent.getClass().getSimpleName()+": '"+parent.getName()+"')");
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
		
		private Status status; //Current status of a bean
		private double percentComplete; //Current percent complete of a bean
		private double workFraction = 1d; //Fraction of total work in parent performed by this bean
		private boolean operating = false; //Flag to show this bean is currently doing work (i.e. queue is running)
		
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
		 * Test whether the process has actually been started (NONE is being 
		 * used to indicate nothing done yet).
		 * @return true if process state is final & is NONE
		 */
		public boolean isFinished() {
			return status.isFinal() && status != Status.NONE;
		}

		@Override
		public String toString() {
			return "ProcessStatus [percentComplete=" + percentComplete + ", workFraction=" + workFraction + ", status="
					+ status + ", operating=" + operating + "]";
		}
	}

}
