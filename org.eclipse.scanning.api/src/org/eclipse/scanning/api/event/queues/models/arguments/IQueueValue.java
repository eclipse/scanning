package org.eclipse.scanning.api.event.queues.models.arguments;

import java.lang.reflect.Method;

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
	public V evaluate();
	
	/**
	 * Flag to indicate this {@link IQueueValue} hold a reference to another 
	 * in the {@link IQueueBeanFactory}.
	 * 
	 * @return true if this holds a reference
	 */
	public boolean isReference();
	
	/**
	 * Tests whether this {@link IQueueValue} refers to the given 
	 * {@link IQueueValue} and should therefore be replaced by it when 
	 * processed by the {@link IQueueBeanFactory}.
	 * @param value {@link IQueueValue} being referred to
	 * @return true if this {@link IQueueValue is the reference
	 */
	public boolean isReferenceFor(IQueueValue<?> value);
	
	/**
	 * Tests whether the given method is a setter for a field with the same 
	 * name as this {@link IQueueValue} and that the argument types match. 
	 * @param method Method with name to compare
	 * @return true if the method name is set+{@link #getName()}.
	 */
	public default boolean isSetMethodForName(Method method) {
		return (method.getName().toLowerCase().equals(("set"+getName()).toLowerCase()) && 
				method.getParameterCount() == 1 &&
				method.getParameterTypes()[0].equals(getValueType()));
	}
	
	/**
	 * Get the Class object representing the value type.
	 * @return Class<?> representing value type
	 */
	public Class<?> getValueType();

}
