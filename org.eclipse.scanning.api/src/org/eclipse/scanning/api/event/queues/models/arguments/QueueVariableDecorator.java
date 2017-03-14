package org.eclipse.scanning.api.event.queues.models.arguments;

/**
 * Abstract class for the decorator pattern. It allows {@link IQueueValues} to 
 * be augmented with additional data/tables from which to find their values.
 * 
 * @author Michael Wharmby
 *
 * @param <A> Type of input argument {@link IQueueValue}.
 * @param <V> Type of output value.
 */
public abstract class QueueVariableDecorator<A, V> implements IQueueVariable<A, V> {
	
	protected IQueueValue<A> arg;
	protected V value;
	
	protected QueueVariableDecorator(IQueueValue<A> arg) {
		this.arg = arg;
	}
	
	@Override
	public void setArg(IQueueValue<A> arg) {
		this.arg = arg;
	}
	
	/**
	 * Get the argument of the argument {@link IQueueValue} and use it to 
	 * determine the value of this QueueVariableDecorator.
	 * 
	 * @return value V of this QueueVariableDecorator, determined by given arg.
	 */
	@Override
	public V evaluate() {
		return processArg(arg.evaluate());
	}
	
	/*
	 * Take the value determined from the child {@link IArg} and use it to 
	 * determine the value of this ArgDecorator.
	 */
	protected abstract V processArg(A parameter);
}
