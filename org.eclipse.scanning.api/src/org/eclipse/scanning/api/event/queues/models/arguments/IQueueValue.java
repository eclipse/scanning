package org.eclipse.scanning.api.event.queues.models.arguments;

/**
 * A model argument. Used as a placeholder for values used within models which 
 * are known at compile time. Value is returned by calling {@see #evaluate()}.
 * 
 * @author Michael Wharmby
 *
 * @param <V> Type of the value held by this argument.
 */
public interface IQueueValue<V> {
	
	/**
	 * Calculate the new value of this IQueueValue and return it.
	 * 
	 * @return value V of this IQueueValue.
	 */
	public V evaluate();

}
