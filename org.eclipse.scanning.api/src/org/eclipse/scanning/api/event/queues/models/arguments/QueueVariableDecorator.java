package org.eclipse.scanning.api.event.queues.models.arguments;

import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

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
	private String name;
	protected V value;

	protected QueueVariableDecorator(String name, IQueueValue<A> arg) {
		this.arg = arg;
		this.name = name;
	}

	@Override
	public void setArg(IQueueValue<A> arg) {
		this.arg = arg;
	}

	@Override
	public IQueueValue<A> getArg() {
		return arg;
	}

	/**
	 * Process the value of this {@link IQueueVariable} and return the result.
	 * This method is inherited from {@link IQueueValue} and allows
	 * {@link IQueueVariable}s to be processed as {@link IQueueValue}s.
	 *
	 * @return value V of this QueueVariableDecorator
	 * @throws QueueModelException if no value can be returned
	 */
	@Override
	public V evaluate() {
		return processArg(arg.evaluate());
	}

	/**
	 * Take the value determined from this {@link IQueueVariable} and use it
	 * to determine the value of this ArgDecorator.
	 *
	 * @return value V of the child {@link IQueueVariable}
	 * @throws QueueModelException if no value can be returned
	 */
	protected abstract V processArg(A parameter);

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * For simplicity, {@link QueueVariableDecorator}s should never be variables.
	 * This could be changed in the future. TODO?
	 */
	@Override
	public boolean isReference() {
		return false;
	}

	@Override
	public boolean isReferenceFor(IQueueValue<?> value) {
		throw new ModelEvaluationException("Not implemented: "+this.getClass().getSimpleName()+" "+name+" cannot be used as a reference to another IQueueValue");
		/*
		 * TODO This is for simplicity. In the future, the evaluated value
		 * could be used as the reference to test against (rather than the name
		 * as it is in the
		 */
	}

	@Override
	public Class<?> getValueType() {
		return value.getClass();
	}

}
