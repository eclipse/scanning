package org.eclipse.scanning.api.event.queues.models.arguments;

import java.util.Map;

/**
 * {@link IArg} which uses a lookup table (Map/dict) to determine its value.
 * 
 * @author Michael Wharmby
 *
 * @param <P> Type of the key field of the lookup table.
 * @param <V> Type of value field of the lookup table.
 */
public class LookupArg<P, V> extends ArgDecorator<P, V> {
	
	private Map<P, V> lookupTable;

	/**
	 * Construct new LookupArg from a lookup table ({@link Map}) and an 
	 * {@link IArg} which provides a key. The key is used to select the value 
	 * from the table.
	 * 
	 * @param childArg Supplying the key
	 * @param lookupTable Containing keys and values to be returned
	 */
	public LookupArg(IArg<P> childArg, Map<P, V> lookupTable) {
		super(childArg);
		this.lookupTable = lookupTable;
	}

	@Override
	protected void processArg(P parameter) {
		value = lookupTable.get(parameter);
	}

}
