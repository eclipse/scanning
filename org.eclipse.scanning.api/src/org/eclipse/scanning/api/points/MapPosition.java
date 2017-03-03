/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.api.points;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multiple values to the position, backed by a Map
 * @author Matthew Gerring
 *
 */
public final class MapPosition extends AbstractPosition {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3161176012921556875L;
	
	private Map<String, Object>  values;   // Name->Value
	private Map<String, Integer> indices;  // Name->Index
	
	public MapPosition() {
		values   = new LinkedHashMap<String, Object>(7);
		indices  = new LinkedHashMap<String, Integer>(7);
	}

	public MapPosition(Map<String, Object> vals) {
		this(vals, new LinkedHashMap<String, Integer>(7));
	}

	public MapPosition(Map<String, Object> vals, Map<String, Integer> inds) {
		values   = vals;
		if (values.containsKey("stepIndex")) {
			setStepIndex((Integer)values.remove("stepIndex"));
		}
		indices  = inds;
	}
	
	public MapPosition(String name, Integer index, Object value) {
		this();
		values.put(name, value);
		indices.put(name, index);
	}

	/**
	 * Define the values as a comma separate list of values of the form:
	 * name1:position1:value1, name2:position2:value2, ... etc. namen:valuen
	 * 
	 * The value string must parse as a double or a NumberFormatException is thrown
	 * 
	 * Spaces between commas and between names and values will be trimmed
	 * 
	 * @param value
	 * @throws NumberFormatException if the elements of the string could not be parsed as numbers
	 */
	public MapPosition(String value) throws NumberFormatException {
		
		values  = new LinkedHashMap<String, Object>(7);
		indices = new LinkedHashMap<String, Integer>(7);
		
		String[] pairs = value.split(",");
		for (String pair : pairs) {
			String[] nv = pair.trim().split("\\:");
			indices.put(nv[0].trim(), Integer.parseInt(nv[1].trim()));
			values.put(nv[0].trim(), Double.parseDouble(nv[2].trim()));
		}
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public List<String> getNames() {
		return new ArrayList<String>(values.keySet());
	}

	@Override
	public Object get(String name) {
		return values.get(name);
	}

	public Object put(String key, Object value) {
		return values.put(key, value);
	}
	
	public void putAll(IPosition pos) {
		final Collection<String> names = pos.getNames();
		if (names==null) return; // StaticPosition allowed.
		for (String name : names) {
			values.put(name, pos.get(name));
			indices.put(name, pos.getIndex(name));
		}
	}

	@Override
	public int getIndex(String name) {
		if (indices==null)              return -1;
		if (!indices.containsKey(name)) return -1; // Autoboxing protection.
		return indices.get(name);
	}

	public Object putIndex(String key, Integer value) {
		if (indices==null) indices = new LinkedHashMap<String, Integer>(7);
		return indices.put(key, value);
	}
	
	public void putAllIndices(IPosition pos) {
		if (indices==null) indices = new LinkedHashMap<String, Integer>(7);
		final Collection<String> names = pos.getNames();
		if (names==null) return;
		for (String name : names) {
			indices.put(name, pos.getIndex(name));
		}
	}

	@Override
	public Map<String, Integer> getIndices() {
		return indices;
	}
	
	@Override
	public Map<String, Object> getValues() {
		return values;
	}

}
