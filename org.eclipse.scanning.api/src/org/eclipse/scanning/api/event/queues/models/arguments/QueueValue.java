package org.eclipse.scanning.api.event.queues.models.arguments;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;

/**
 * Basic model argument which holds a single object. Typically this would be a 
 * String, Double, Integer or another simple type, but could also be a List, 
 * Map or other collection. 
 * 
 * @author Michael Wharmby
 *
 * @param <V> Type of the value held by this argument
 */
public class QueueValue<V> implements IQueueValue<V> {
	
	private String name;
	private V value;
	private boolean reference;
	
	/**
	 * Construct a new {@link QueueValue} with a given value.
	 * 
	 * @param value V to be stored in this argument
	 */
	public QueueValue(V value) {
		this(null, value, false);
	}
	
	/**
	 * Construct a new QueueValue with a given value. Marking it as a variable 
	 * indicates to the {@link IQueueBeanFactory} that it only holds a 
	 * reference to a real value to be evaluated.
	 * @param value V to be stored in this argument
	 * @param reference if true this holds only a reference to another 
	 *        {@link IQueueValue}
	 */
	public QueueValue(V value, boolean reference) {
		this(null, value, reference);
	}
	
	/**
	 * Construct a new named {@link QueueValue} with a given value.
	 * @param name String name of {@link QueueValue}
	 * @param value V to be stored in this argument
	 */
	public QueueValue(String name, V value) {
		this(name, value, false);
	}
	
	/**
	 * Construct a new named {@link QueueValue} with a given value. Marking it 
	 * as a variable indicates to the {@link IQueueBeanFactory} that it only 
	 * holds a reference to a real value to be evaluated.
	 * @param name String name of {@link QueueValue}
	 * @param value V to be stored in this argument
	 * @param reference if true this holds only a reference to another 
	 *        {@link IQueueValue}
	 */
	public QueueValue(String name, V value, boolean reference) {
		this.name = name;
		this.value = value;
		this.reference = reference;
	}

	@Override
	public V evaluate() {
		if (value == null) throw new ModelEvaluationException("Value of '"+getName()+"' is not set");
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

	@Override
	public boolean isReference() {
		return reference;
	}
	
	@Override
	public boolean isReferenceFor(IQueueValue<?> value) {
			return this.value == value.getName() && reference;
	}
	
	@Override
	public Class<?> getValueType() {
		return value.getClass();
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + (reference ? 1231 : 1237);
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
		QueueValue<?> other = (QueueValue<?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (reference != other.reference)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "QueueValue [name=" + name + ", value=" + value + ", reference=" + reference + "]";
	}
}
