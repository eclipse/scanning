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

package org.eclipse.scanning.device.ui.vis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;

/**
 * Simple class to hold information about a scan path
 * The information is held in image coordinates not axis coordinates.
 * Therefore the datasets produces by getX and getY may be
 * directly plotted.
 */
class PathInfo {
	
	// TODO Encapsulate these fields properly...
	protected int    pointCount      = 0;
	protected double smallestXStep   = Double.MAX_VALUE;
	protected double smallestYStep   = Double.MAX_VALUE;
	protected double smallestAbsStep = Double.MAX_VALUE;
	
	// These fields have been encapsulated.
	private List<Double> xCoordinates = new ArrayList<>();
	private List<Double> yCoordinates = new ArrayList<>();

	private String pointCountFormat = "%,d";
	private String doubleFormat = "%.4g";

	public String getFormattedPointCount() {
		return String.format(pointCountFormat, pointCount);
	}
	public String getFormattedSmallestXStep() {
		return formatDouble(smallestXStep);
	}
	public String getFormattedSmallestYStep() {
		return formatDouble(smallestYStep);
	}
	public String getFormattedSmallestAbsStep() {
		return formatDouble(smallestAbsStep);
	}
	private double[] getXCoordinates() {
		double[] xCoords = new double[xCoordinates.size()];
		for (int index = 0; index < xCoordinates.size(); index++) {
			xCoords[index] = xCoordinates.get(index).doubleValue();
		}
		return xCoords;
	}
	private double[] getYCoordinates() {
		double[] yCoords = new double[yCoordinates.size()];
		for (int index = 0; index < yCoordinates.size(); index++) {
			yCoords[index] = yCoordinates.get(index).doubleValue();
		}
		return yCoords;
	}
	
	public IDataset getX() {
		return DatasetFactory.createFromObject(getXCoordinates());
	}
	public IDataset getY() {
		return DatasetFactory.createFromObject(getYCoordinates());
	}

	private String formatDouble(double value) {
		if (value == Double.MAX_VALUE) {
			return "N/A";
		}
		return String.format(doubleFormat, value);
	}
	
	public Map<String, Object> toDict() {
		Map<String, Object> ret = new HashMap<>(1);
		ret.put(PathInfo.class.getSimpleName(), this);
		return ret;
	}
	
	public static PathInfo fromDict(Map<String, Object> map) {
		return (PathInfo) map.get(PathInfo.class.getSimpleName());
	}
	
	public int size() {
		return xCoordinates.size();
	}
	public void add(Double x, Double y) {
		xCoordinates.add(x);
		yCoordinates.add(y);
	}

}