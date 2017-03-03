package org.eclipse.scanning.api.event.queues.models.arguments;

/**
 * A model argument. Used as a placeholder for values used within models which 
 * are unknown at compile time, but can be determined based on user input. 
 * Values are calculated from, for example, an equation which should be passed 
 * to the IArg prior to running evaluate.
 * 
 * @author Michael Wharmby
 *
 * @param <V> Type of the value held by this argument.
 */
public interface IArg<V> {
	
	/**
	 * Calculate the new value of this argument.
	 */
	public void evaluate();
	
	/**
	 * Get the value of this argument, calculated after running evaluate().
	 * 
	 * @return value V of this argument.
	 */
	public V getValue();

}
