package org.eclipse.scanning.api.event.queues.models.arguments;

/**
 * A model argument. Used as a placeholder for values used within models which 
 * are unknown at compile time, but can be determined based on user input. 
 * Values are calculated using the {@see IQueueValue#evaluate()} method, with 
 * arguments for the variable determination supplied through the 
 * {@see #setArg()} method (or through the constructor, if implemented).
 * 
 * @author wnm24546
 *
 * @param <A> Type of the argument supplied to this IQueueVariable
 * @param <V> Type of the value held by this IQueueVariable.
 */
public interface IQueueVariable<A,V> extends IQueueValue<V> {
	
	/**
	 * Set the {@link IQueueValue} containing the argument needed to evaluate 
	 * this variable.
	 * 
	 * @param arg {@link IQueueValue} defining argument for variable.
	 */
	public void setArg(IQueueValue<A> arg);

}
