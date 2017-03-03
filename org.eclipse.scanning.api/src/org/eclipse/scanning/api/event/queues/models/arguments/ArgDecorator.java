package org.eclipse.scanning.api.event.queues.models.arguments;

/**
 * Abstract class for the decorator pattern. It allows {@link IArgs} to be 
 * augmented with additional data/tables from which to find their values.
 * 
 * @author Michael Wharmby
 *
 * @param <P> Type of input parameter.
 * @param <V> Type of output value.
 */
public abstract class ArgDecorator<P, V> implements IArg<V> {
	
	protected IArg<P> childArg;
	protected V value;
	
	protected ArgDecorator(IArg<P> childArg) {
		this.childArg = childArg;
	}
	
	@Override
	public V getValue() {
		return value;
	}
	
	/**
	 * Take the value of the decorated {@link IArg} use it to determine the 
	 * value of this ArgDecorator.
	 */
	@Override
	public void evaluate() {
		childArg.evaluate();
		processArg(childArg.getValue());
	}
	
	/*
	 * Take the value determined from the child {@link IArg} and use it to 
	 * determine the value of this ArgDecorator.
	 */
	protected abstract void processArg(P parameter);
}
