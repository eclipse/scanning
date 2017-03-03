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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.device.IRunnableDeviceService;

/**
 * A position is a location of one or more values. It is
 * used to group scannables when moving to a position
 * by level.
 * <p>
 * For instance a group of scannables which need to be at a
 * certain location. For example at the start of a scan or
 * for a scan datapoint.
 * <p>
 * <B>NOTE:</B> An object is available using IDeviceService.createPositioner()
 * called IPositioner which can move scannables to a given
 * position by level.
 * <p>
 * Implementations of this class should be immutable where possible. Where this is not
 * possible they should not be modified once they have been fully initialized. 
 * 
 * @see IRunnableDeviceService
 * @see org.eclipse.scanning.api.scan.event.IPositioner
 *
 * @author Matthew Gerring
 *
 */
public interface IPosition {
	
	/**
	 * The number of named scalars in this position
	 * @return
	 */
	int size();

	/**
	 * The names of all the scalars set for this position.
	 * For instance 'x' and 'y' for a map or 'Temperature'
	 * <em>Note to implementers:</em> should never return <code>null</code> 
	 * 
	 * @return name of scalars
	 */
	List<String> getNames();

	/**
	 * Get the data index of this point for a given scan dimension.
	 * 
	 * For instance 
	 * 
	 * @param dimension
	 * @return
	 */
	int getIndex(int dimension);
	
	/** 
	 * Get the index of the data for instance in a scan of temperature from 290 to 300 step 1,
	 * the indices will be 0-10.
	 * 
	 * If one dimension has more than one motor with it, for instance x and y in a line scan,
	 * both getIndex("x") and getIndex("y") return the same value.
	 * 
	 * @return
	 */
	int getIndex(String name); 

	/**
	 * The value of a named position. For instance get("X") to return the value of the 
	 * X IScannable double.
	 * 
	 * @param name
	 * @return
	 */
	Object get(String name);
	
	/**
	 * Same as ((Number)get(name)).doubleValue()
	 * Available for convenience
	 * 
	 * @param name
	 * @return
	 */
	default double getValue(String name) {
		return ((Number)get(name)).doubleValue();
	}
	
	/**
	 * Creates a composite position with the values of this position 
	 * and the values of the passed in position. The passed in position
	 * is assumed to be the parent in the scan.
	 * 
	 * This position's values take precedence if the names conflict.
	 * It is like an implementation of Map where the putAll does not
	 * overwrite.
	 * 
	 * NOTE The scan names are not calculated on the call to compound
	 * because maintaining the list of names in each dimension is inefficient
	 * to calculate for each point (they do not change). Instead the names are 
	 * created once and set into the position using setDimensionNames(...)
	 * available on abstract position. If generators producing positions
	 * are to be used within a cponound generator 
	 * 
	 * @param parent
	 * @return
	 */
	IPosition compound(IPosition parent);
	
	/**
	 * The step where the position was in a scan, if it is a position being
	 * generated from a scan. If not the value will be -1
	 * @return
	 */
	default int getStepIndex() {
		return -1;
	}
	
	/**
	 * The step where the position was in a scan, if it is a position being
	 * generated from a scan. If not the value will be -1
	 * @param step
	 */
	default void setStepIndex(int step) {
		return; 
	}
	
	/**
	 * Most scans have rank 1 event though they move more
	 * motors line a line scan or a spiral scan. As scans are
	 * aggregated these scan dimensions sum together.
	 * 
	 * Some scans start out as having two dimensions like a grid
	 * or raster scan.
	 * 
	 * @return
	 */
	int getScanRank();
	
	/**
	 * It is not required of an IPosition to provide getValues() but it
	 * may do so to avoid a new map being built up. Implement this method
	 * to ensure that your position runs faster. The default implementation
	 * works.
	 * 
	 * @see org.eclipse.scanning.api.points.MapPosition#getValues()
	 * @see org.eclipse.scanning.api.points.Point#getValues()
	 * @return
	 */
	default Map<String, Object> getValues() {
		final Map<String,Object> values = new LinkedHashMap<>(size());
		for (String name : getNames()) values.put(name, getValue(name));
		return values;
	}

	/**
	 * It is not required of an IPosition to provide getIndices() but it
	 * may do so to avoid a new map being built up. Implement this method
	 * to ensure that your position runs faster. The default implementation
	 * works.
	 * 
	 * @see org.eclipse.scanning.api.points.MapPosition#getIndices()
	 * @see org.eclipse.scanning.api.points.Point#getIndices()
	 * @return
	 */
	default Map<String, Integer> getIndices() {
		final Map<String,Integer> indices = new LinkedHashMap<>(size());
		for (String name : getNames()) indices.put(name, getIndex(name));
		return indices;
	}
}
