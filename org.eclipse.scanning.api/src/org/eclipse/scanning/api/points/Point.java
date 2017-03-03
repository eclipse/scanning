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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.annotation.UiHidden;

/**
 * 
 * This class represents an x,y position for a mapping scan.
 * 
 * By default Points are 2D values used in things like GridScans. If used in a
 * LineScan or a Spiral scan where one dimension has two motors, the
 * constructor with is2D=false should be used.
 * 
 * The Point location is immutable: you may not change the values of x and y after it
 * is created.
 *
 * @authors James Mudd, Matthew Gerring
 */
public final class Point extends AbstractPosition {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2946649777289185552L;
	
	
	private final Double  x;
	private final Double  y;
	private final Integer xIndex;
	private final Integer yIndex;
	private final String  xName;
	private final String  yName;
	
	public Point(int xIndex, double xPosition, int yIndex, double yPosition) {
		this(xIndex, xPosition, yIndex, yPosition, true);
	}
	
	public Point(int xIndex, double xPosition, int yIndex, double yPosition, boolean is2D) {
		this("x", xIndex, xPosition, "y", yIndex, yPosition, is2D);
	}

	public Point(String xName, int xIndex, double xPosition, String yName, int yIndex, double yPosition) {
		this(xName, xIndex, xPosition, yName, yIndex, yPosition, true);
	}
	
	public Point(String xName, int xIndex, double xPosition, String yName, int yIndex, double yPosition, boolean is2D) {
		this.xName  = xName;
		this.xIndex = xIndex;
		this.x      = xPosition;
		this.yName  = yName;
		this.yIndex = yIndex;
		this.y      = yPosition;
		
		this.dimensionNames = is2D
                ? Arrays.asList(Arrays.asList(yName), Arrays.asList(xName))
                : Arrays.asList(Arrays.asList(yName, xName));
	}
	
	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	@Override
	public int size() {
		return 2;
	}

	private List<String> names;
	@UiHidden
	@Override
	public List<String> getNames() {
		if (names==null) names = Arrays.asList(yName, xName);
		return names;
	}

	@Override
	public Double get(String name) {
		if (xName.equalsIgnoreCase(name)) return getX();
		if (yName.equalsIgnoreCase(name)) return getY();
		return null;
	}
	
	@Override
	public int getIndex(String name) {
		if (xName.equalsIgnoreCase(name)) return xIndex;
		if (yName.equalsIgnoreCase(name)) return yIndex;
		return -1;
	}
	
	private Map<String, Object>  values;

	@UiHidden
	@Override
	public Map<String, Object> getValues() {
		if (values == null) {
			values = new LinkedHashMap<>(2);
			values.put(yName, y);
			values.put(xName, x);
		}
		return values;
	}

	
	private Map<String, Integer>  indices;

	@UiHidden
	@Override
	public Map<String, Integer> getIndices() {
		if (indices == null) {
			indices = new LinkedHashMap<>(2);
			indices.put(yName, yIndex);
			indices.put(xName, xIndex);
		}
		return indices;
	}
}
