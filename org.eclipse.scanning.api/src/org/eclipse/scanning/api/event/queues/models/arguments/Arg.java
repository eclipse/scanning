package org.eclipse.scanning.api.event.queues.models.arguments;

/**
 * Basic model argument which holds a single object. Typically this would be a 
 * String, Double, Integer or another simple type. 
 * 
 * @author Michael Wharmby
 *
 * @param <V> Type of the value held by this argument.
 */
public class Arg<V> implements IArg<V> {
	
	private V parameter,value;
	
	/**
	 * Construct a new argument with a given parameter.
	 * 
	 * @param parameter value V to be stored in this argument.
	 */
	public Arg(V parameter) {
		this.parameter = parameter;
	}
	
	/**
	 * Sets the value equal to the parameter.
	 */
	@Override
	public void evaluate() {
		value = parameter;
	}
	
	/**
	 * Return the parameter. This field is populated prior to evaluate() call.
	 * 
	 * @return V representing the configured parameter of this object.
	 */
	public V getParameter() {
		return parameter;
	}
	
	@Override
	public V getValue() {
		return value;
	}

}
