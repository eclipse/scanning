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
import java.util.stream.Collectors;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

/**
 * TaskBean is a type of {@link QueueBean} implementing an 
 * {@link IHasAtomQueue}. As a {@link QueueBean} it can only be passed 
 * into the job-queue of an {@link IQueueService} and as such provides the most
 * abstract description of an experiment. 
 * 
 * This class of bean is used to describe a complete experiment, including the
 * beamline setup, data collection and post-processing steps. It should also 
 * contain all the sample metadata necessary to write the NeXus file. 
 * 
 * TODO Sample metadata holder.
 * 
 * @author Michael Wharmby
 *
 */
public class TaskBean extends QueueBean implements IHasAtomQueue<SubTaskAtom> {

	/**
	 * Version ID for serialization. Should be updated when class changed. 
	 */
	private static final long serialVersionUID = 20161021L;
	
	private LinkedList<SubTaskAtom> atomQueue;
	private LinkedList<QueueValue<String>> queueAtomShortNames;
	private String queueMessage;
//	private Object nexusMetadata; TODO!!!!
	
	/**
	 * No argument constructor for JSON
	 */
	public TaskBean() {
		super();
		atomQueue = new LinkedList<>();
	}
	
	/**
	 * Create an instance with a shortname (reference) and a more verbose name 
	 * to be used in UI etc.
	 * @param tbShrtNm String short name used within the 
	 *        {@link IQueueBeanFactory}
	 * @param name String user-supplied name
	 */
	public TaskBean(String tbShrtNm, String name) {
		this(tbShrtNm, name, false);
	}
	
	/**
	 * Create an instance with a shortname (reference) and a more verbose name 
	 * to be used in UI etc. This may be a model which can be used by the 
	 * {@link IQueueBeanFactory} to create a real {@link TaskBean} or it may 
	 * itself be a real {@link TaskBean}.
	 * @param stShrtNm String short name used within the 
	 *        {@link IQueueBeanFactory}
	 * @param name String user-supplied name
	 * @param model boolean flag indicating whether this is a model
	 */
	public TaskBean(String tbShrtNm, String name, boolean model) {
		super();
		setShortName(tbShrtNm);
		setName(name);
		setModel(model);
		if (model) {
			queueAtomShortNames = new LinkedList<>();
		} else {
			atomQueue = new LinkedList<>();
		}
	}
	
	/**
	 * Create a model instance with a shortname (reference), a more verbose 
	 * name and a list of shortnames (references) of atoms which will be used 
	 * by the {@link IQueueBeanFactory} during the creation of a real 
	 * {@link TaskBean} instance.
	 * @param stShrtNm String short name used within the 
	 *        {@link IQueueBeanFactory}
	 * @param name String user-supplied name
	 * @param queueAtomShortNames List of String references which will be 
	 *        converted into an atomQueue
	 */
	public TaskBean(String tbShrtNm, String name, List<QueueValue<String>> queueAtomShortNames) {
		this(tbShrtNm, name, true);
		setQueueAtomShortNames(queueAtomShortNames);
	}
	
	@Override
	public List<SubTaskAtom> getAtomQueue() {
		return atomQueue;
	}

	@Override
	public void setAtomQueue(List<SubTaskAtom> atomQueue) {
		this.atomQueue = new LinkedList<>(atomQueue);
		setRunTime(calculateRunTime());
	}
	
	@Override
	public List<QueueValue<String>> getQueueAtomShortNames() {
		return queueAtomShortNames;
	}

	@Override
	public void setQueueAtomShortNames(List<QueueValue<String>> queueAtomShortNames) {
		this.queueAtomShortNames = new LinkedList<>(queueAtomShortNames);
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
	public boolean addAtom(SubTaskAtom atom) {
		//Check this SubTaskAtom is real and the atom is not a non-duplicate
		if (model) throw new IllegalArgumentException("Cannot add non-model atom to a model SubTaskAtom");
		if(isAtomPresent(atom)) throw new IllegalArgumentException("Atom with identical UID already in queue.");
		
		boolean result = addAtom(atom, atomQueue);
		setRunTime(calculateRunTime());
		return result;
	}
	
	public boolean addAtom(QueueValue<String> subTaskAtomShortName) {
		//Check this SubTaskAtom is a model 
		if (!model) throw new IllegalArgumentException("Cannot add model atom to a non-model TaskBean");
		
		return addAtom(subTaskAtomShortName, queueAtomShortNames);
	}
	
	/**
	 * Generic method to add either an atom or a model name to this 
	 * {@link TaskBean}.
	 * @param atom T being added to queue (String or {@link SubTaskAtom})
	 * @param atomList List describing the queue
	 * @return true if the atom was added successfully
	 */
	private <T> boolean addAtom(T atom, List<T> atomList) {
		//Check it's not a null we're adding to our list...
		if(atom == null) throw new NullPointerException("Attempting to add null atom to AtomQueue");
		return atomList.add(atom);

	}

	@Override
	public int atomQueueSize() {
		return atomQueue.size();
	}

	@Override
	public int getQueuePosition(String uid) {
		for (SubTaskAtom atom: atomQueue) {
			if (uid.equals(atom.getUniqueId())) return atomQueue.indexOf(atom);
		}
		throw new IllegalArgumentException("No queue element present with given UID");
	}

	@Override
	public SubTaskAtom nextAtom() {
		//Returns & removes first element of queue or throws NoSuchElementException if null
		return atomQueue.removeFirst();
	}

	@Override
	public SubTaskAtom viewNextAtom() {
		//Returns head of queue or throws NoSuchElementException if null.
		return atomQueue.getFirst();
	}

	@Override
	public boolean isAtomPresent(SubTaskAtom atom) {
		for (SubTaskAtom at: atomQueue) {
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
		result = prime * result + ((queueAtomShortNames == null) ? 0 : queueAtomShortNames.hashCode());
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
		TaskBean other = (TaskBean) obj;
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
		if (queueAtomShortNames == null) {
			if (other.queueAtomShortNames != null)
				return false;
		} else if (!queueAtomShortNames.equals(other.queueAtomShortNames))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String clazzName = this.getClass().getSimpleName();
		
		StringBuffer atomQueueStrBuff = new StringBuffer("{");
		if (model) {
			clazzName = clazzName + " (MODEL)";
			atomQueueStrBuff.append(queueAtomShortNames.stream().map(qv -> {
				try {
					return qv.evaluate();
				} catch (ModelEvaluationException e) {
					return "Failed to read queue atom short names";
				}
			}).collect(Collectors.joining(", ")));
		} else {
			atomQueueStrBuff.append(atomQueue.stream().map(qa -> qa.getShortName() + "('" + qa.getName() + "')")
					.collect(Collectors.joining(", ")));
		} 
		atomQueueStrBuff.append("}");
		String atomQueueStr = atomQueueStrBuff.toString();
		atomQueueStr = atomQueueStr.replaceAll(", }$", "}"); //Replace trailing ", "
		
		return clazzName + " [name=" + name + " (shortName="+shortName+"), atomQueue=" + atomQueueStr 
				+ ", status=" + status + ", message=" + message + ", queueMessage=" + queueMessage 
				+ ", percentComplete=" + percentComplete + ", previousStatus=" + previousStatus 
				+ ", runTime=" + runTime + ", userName=" + userName+ ", hostName=" + hostName 
				+ ", beamline="+ beamline + ", submissionTime=" + submissionTime + "]";
	}

}