package org.eclipse.scanning.api.event.queues.models.arguments;

import org.eclipse.scanning.api.event.queues.models.QueueModelException;

/**
 * Basic model argument which holds a single object. Typically this would be a 
 * String, Double, Integer or another simple type, but could also be a List, 
 * Map or other collection. 
 * 
 * @author Michael Wharmby
 *
 * @param <V> Type of the value held by this argument.
 */
public class QueueValue<V> implements IQueueValue<V> {
	
	private String name;
	private V value;
	
	/**
	 * Construct a new QueueValue with a given value.
	 * 
	 * @param parameter value V to be stored in this argument.
	 */
	public QueueValue(V value) {
		this(null, value);
	}
	
	public QueueValue(String name, V value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public V evaluate() {
		if (value == null) throw new QueueModelException("Value was never initialised");
		return value;
	}
	
	/**
	 * Change the value of this QueueValue.
	 * 
	 * @param value V of this QueueValue.
	 */
	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	} 
	
}
