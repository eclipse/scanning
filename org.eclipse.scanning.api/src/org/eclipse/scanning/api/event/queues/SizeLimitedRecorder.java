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
package org.eclipse.scanning.api.event.queues;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An object composed of a list which may only contain a fixed number of items
 * of a given type. The capacity of the list may be increased or decreased as 
 * required. The recorder stores objects in the order in which they arrive, 
 * and thus has the idea of oldest and latest objects. It can act as a 
 * chronological store.
 * 
 * @author Michael Wharmby
 *
 * @param <T> type of object recorded in the internal list.
 */
@Deprecated
public class SizeLimitedRecorder<T extends Object> {
	
	private final LinkedList<T> record;
	private int capacity;
	
	/**
	 * Create a new recorder, with a maximum number of capacity elements in it's List 
	 * 
	 * @param capacity
	 */
	public SizeLimitedRecorder(int capacity) {
		this.capacity = capacity;
		
		record = new LinkedList<T>();
	}

	/**
	 * Get the maximum number of elements stored in this recorder.
	 * 
	 * @return int capacity of recorder
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Change the maximum number of elements storable. If this is less than the
	 * number of elements, the record will be truncated.
	 * 
	 * @param capacity
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
		adaptSize();
	}

	/**
	 * Return the complete recording.
	 * 
	 * @return List containing the recorded elements.
	 */
	public List<T> getRecording() {
		return record;
	}
	
	/**
	 * Append a new element to the record. If this increases the size of the 
	 * record beyond capacity, the oldest element will be removed.
	 * 
	 * @param element to be added to the record
	 */
	public void add(T element) {
		record.add(element);
		adaptSize();
	}
	
	/**
	 * The most recently added element in the record.
	 * 
	 * @return The last element added to the record.
	 */
	public T latest() {
		try {
			return record.getLast();
		} catch (NoSuchElementException nse) {
			//In cases where we've not heard a beat.
			return null;
		}
	}
	
	/**
	 * The element which has been in the record longest.
	 * 
	 * @return The oldest element in the list.
	 */
	public T oldest() {
		try {
			return record.getFirst();
		} catch (NoSuchElementException nse) {
			//In cases where we've not heard a beat.
			return null;
		}
	}
	
	/**
	 * Return the element at the ith position in the list 
	 * 
	 * @param i index of element to be returned
	 * @return The element at the requested position in the list
	 */
	public T get(int i) {
		return record.get(i);
	}
	
	/**
	 * Returns true if this collection contains no elements. 
	 * 
	 * @return true if this collection contains no elements
	 */
	public boolean isEmpty() {
		return record.isEmpty();
	}
	
	/**
	 * Returns the number of elements in this list.
	 * @return the number of elements in this list
	 */
	public int size() {
		return record.size();
	}
	
	/**
	 * Removes all elements from this recording.
	 */
	public void clear() {
		record.clear();
	}
	
	/**
	 * When the capacity of the recorder is updated, it may be necessary to 
	 * remove older records.
	 */
	private void adaptSize() {
		while (true) {
			if (this.capacity >= record.size()) break;
			record.pop();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + capacity;
		result = prime * result + ((record == null) ? 0 : record.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SizeLimitedRecorder<?> other = (SizeLimitedRecorder<?>) obj;
		if (capacity != other.capacity)
			return false;
		if (record == null) {
			if (other.record != null)
				return false;
		} else if (!record.equals(other.record))
			return false;
		return true;
	}

}
