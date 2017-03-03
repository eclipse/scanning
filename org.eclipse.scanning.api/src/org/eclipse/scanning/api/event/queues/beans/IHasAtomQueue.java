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

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Interface describing a queue of {@link QueueAtoms} which will be spooled 
 * into an {@link IQueueService} queue and sequentially processed.
 * 
 * @author Michael Wharmby
 *
 * @param <T> Class extending QueueAtom; this class will be held in the Queue
 *            inside this Atom/Bean
 */
public interface IHasAtomQueue<T extends QueueAtom> extends IHasChildQueue {
	
	/**
	 * Get the queue of {@link QueueAtom}s held by this atom/bean.
	 * 
	 * @return List describing the queue.
	 */
	public List<T> getAtomQueue();

	/**
	 * Replace the entire queue of {@link QueueAtom}s. This also recalculates 
	 * the run-time.
	 * 
	 * @param List of beans describing a queue.
	 */
	public void setAtomQueue(List<T> atomQueue);
	
	/**
	 * From the {@link QueueAtom}s present in the queue, calculate the 
	 * time this queue should take to run through.
	 * 
	 * @return long sum time in ms for {@link QueueAtom}s to run.
	 */
	public long calculateRunTime();
	
	/**
	 * Append a {@link QueueAtom} to the end of the list comprising 
	 * this queue. Duplicates and null values will cause an exception.
	 * After appending, the run-time is updated. 
	 * 
	 * @param atom - {@link QueueAtom} to add to the list.
	 * @throws NullPointerException - if atom is null.
	 * @throws IllegalArgumentException - if an atom with the same UID is in 
	 *                                    the queue.
	 * @return true if addition was successful.
	 */
	public boolean addAtom(T atom);
	
	/**
	 * Report the number of {@link QueueAtoms} queue.
	 * 
	 * @return number of {@link QueueAtom}s in the queue.
	 */
	public int atomQueueSize();
	
	/**
	 * Return the index of the {@link QueueAtom} with the given unique ID.
	 * 
	 * @param uid - unique ID of {@link QueueAtom}.
	 * @throws IllegalArgumentException - if no atom with given UID in queue. 
	 * @return index of {@link QueueAtom} with given UID.
	 */
	public int getIndex(String uid);
	
	/**
	 * Return and remove the {@link QueueAtom} at the head of the queue. Does 
	 * not change the run-time.
	 * 
	 * @throws NoSuchElementException - if there are no items in the queue.
	 * @return {@link QueueAtom} at head of queue.
	 */
	public T nextAtom();
	
	/**
	 * Return {@link QueueAtom} at the head of the queue, without removing it 
	 * from the queue. Does not change runtime.
	 * 
	 * @throws NoSuchElementException - if there are no items in the queue.
	 * @return {@link QueueAtom} at head of queue.
	 */
	public T viewNextAtom();
	
	/**
	 * Tests whether the given {@link QueueAtom} is already present in 
	 * the queue.
	 * 
	 * @param atom - {@link QueueAtom} whose UID is to be tested.
	 * @return true if atom already in given list.
	 */
	public boolean isAtomPresent(T atom);
	

}
