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
	 * Construct new LookupArg from an {@link IQueueValue} containing a lookup 
	 * table ({@link Map}) and an {@link IQueueValue} which provides a key. The
	 *  key is used to select the value from the table.
	 * 
	 * @param arg Supplying the key
	 * @param lookupTable Containing keys and values to be returned
	 */
	public QueueTableVariable(IQueueValue<A> arg, IQueueValue<Map<A, V>> table) {
		super(arg);
		this.table = table;
	}

	@Override
	protected V processArg(A parameter) {
		V tableValue = table.evaluate().get(parameter);
		if (tableValue == null) throw new QueueModelException("No value for "+parameter+" in table");
		return tableValue;
	}

}
