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
package org.eclipse.scanning.api.event.queues.beans;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.scanning.api.event.queues.IQueueService;

/**
 * SubTaskBean is a type of {@link QueueAtom} implementing an 
 * {@link IHasAtomQueue}. As a {@link QueueAtom} it can only be passed 
 * into an active-queue of an {@link IQueueService}.
 * 
 * This class of bean is used to describe a part of an experiment, for example 
 * motor moves and an I0 measurement before a scan or a series of motor moves, 
 * a scan and post-processing. It should be able to provide a pointer to the 
 * parent queue's sample metadata.
 * 
 * SubTaskBeans may be nested inside of SubTaskBeans to provide a hierarchy.
 * 
 * TODO Update java-doc 
 * 
 * @author Michael Wharmby
 * 
 */
public class SubTaskAtom extends QueueAtom implements IHasAtomQueue<QueueAtom> {

	/**
	 * Version ID for serialization. Should be updated when class changed. 
	 */
	private static final long serialVersionUID = 20161021L;

	private LinkedList<QueueAtom> atomQueue;
	private String queueMessage;

	/**
	 * No argument constructor for JSON
	 */
	public SubTaskAtom() {
		super();
		atomQueue = new LinkedList<>();
	}

	/**
	 * Basic constructor to set String name of bean
	 * @param name String user-supplied name
	 */
	public SubTaskAtom(String name) {
		super();
		atomQueue = new LinkedList<>();
		setName(name);
	}

	@Override
	public List<QueueAtom> getAtomQueue() {
		return atomQueue;
	}

	@Override
	public void setAtomQueue(List<QueueAtom> atomQueue) {
		this.atomQueue = new LinkedList<>(atomQueue);
		setRunTime(calculateRunTime());
	}

	@Override
	public long calculateRunTime() {
		long runTime = 0;
		for (QueueAtom atom: atomQueue) {
			runTime = runTime + atom.getRunTime();
		}
		return runTime;
	}

	@Override
	public boolean addAtom(QueueAtom atom) {
		//Check that we're adding a real, non-duplicate atom to the queue
		if(atom == null) {
			throw new NullPointerException("Attempting to add null atom to AtomQueue");
		}
		if(isAtomPresent(atom)) {
			throw new IllegalArgumentException("Bean with identical UID already in queue.");
		}
		//Add atom, recalculate the runtime and return
		boolean result =  atomQueue.add(atom);
		setRunTime(calculateRunTime());
		return result;
	}

	@Override
	public int atomQueueSize() {
		return atomQueue.size();
	}

	@Override
	public int getIndex(String uid) {
		for (QueueAtom atom: atomQueue) {
			if (uid.equals(atom.getUniqueId())) return atomQueue.indexOf(atom);
		}
		throw new IllegalArgumentException("No queue element present with given UID");
	}

	@Override
	public QueueAtom nextAtom() {
		//Returns & removes first element of queue or throws NoSuchElementException if null
		return atomQueue.removeFirst();
	}

	@Override
	public QueueAtom viewNextAtom() {
		//Returns head of queue or throws NoSuchElementException if null.
		return atomQueue.getFirst();
	}

	@Override
	public boolean isAtomPresent(QueueAtom atom) {
		for (QueueAtom at: atomQueue) {
			String atomUID = atom.getUniqueId();
			if (atomUID.equals(at.getUniqueId())) return true;
		}
		return false;
	}

	@Override
	public String getQueueMessage() {
		return queueMessage;
	}

	@Override
	public void setQueueMessage(String msg) {
		queueMessage = msg;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((atomQueue == null) ? 0 : atomQueue.hashCode());
		result = prime * result + ((queueMessage == null) ? 0 : queueMessage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubTaskAtom other = (SubTaskAtom) obj;
		if (atomQueue == null) {
			if (other.atomQueue != null)
				return false;
		} else if (!atomQueue.equals(other.atomQueue))
			return false;
		if (queueMessage == null) {
			if (other.queueMessage != null)
				return false;
		} else if (!queueMessage.equals(other.queueMessage))
			return false;
		return true;
	}

}
