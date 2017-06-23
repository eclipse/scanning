package org.eclipse.scanning.api.event.queues.models.arguments;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

/**
 * A model argument. Used as a placeholder for values used within models which 
 * are known at compile time. Value is returned by calling {@see #evaluate()}.
 * 
 * @author Michael Wharmby
 *
 * @param <V> Type of the value held by this argument.
 */
public interface IQueueValue<V> {
	
	public String getName();
	
	public void setName(String name);
	
	/**
	 * Determine the new value of this IQueueValue and return it.
	 * 
	 * @return value V of this IQueueValue
	 * @throws QueueModelException if no value can be returned
	 */
	public V evaluate() throws QueueModelException;
	
	/**
	 * Flag to indicate this {@link IQueueValue} hold a reference to another 
	 * in the {@link IQueueBeanFactory}.
	 * @return true if this holds a reference
	 */
	public boolean isVariable();

}
