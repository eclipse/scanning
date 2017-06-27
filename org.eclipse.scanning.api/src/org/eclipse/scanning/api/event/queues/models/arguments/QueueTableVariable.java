package org.eclipse.scanning.api.event.queues.models.arguments;

import java.util.Map;

import org.eclipse.scanning.api.event.queues.models.QueueModelException;

/**
 * {@link IQueueValue} which uses a lookup table (Map/dict) to determine its value.
 * 
 * @author Michael Wharmby
 *
 * @param <A> Type of the key field of the lookup table.
 * @param <V> Type of value field of the lookup table.
 */
public class QueueTableVariable<A, V> extends QueueVariableDecorator<A, V> {
	
	private IQueueValue<Map<A, V>> table;

	/**
	 * Construct new QueueTableVariable from an {@link IQueueValue} containing 
	 * a lookup table ({@link Map}) and an {@link IQueueValue} which provides 
	 * a key. The key is used to select the value from the table. A name can 
	 * also optionally be provided for this variable.
	 * 
	 * @param name String name for argument
	 * @param arg Supplying the key
	 * @param lookupTable Containing keys and values to be returned
	 */
	public QueueTableVariable(String name, IQueueValue<A> arg, IQueueValue<Map<A, V>> table) {
		super(name, arg);
		this.table = table;
	}
	
	/**
	 * Construct a new QueueTableVariable without a name (for full details, 
	 * {@see #QueueTableVariable(String, IQueueValue, IQueueValue)})
	 */
	public QueueTableVariable(IQueueValue<A> arg, IQueueValue<Map<A, V>> table) {
		this(null, arg, table);
	}

	@Override
	protected V processArg(A parameter) throws QueueModelException {
		V tableValue = table.evaluate().get(parameter);
		if (tableValue == null) throw new QueueModelException("No value for '"+parameter+"' in table variable '"+getName()+"'");
		return tableValue;
	}

}
