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
package org.eclipse.scanning.api.malcolm.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.scanning.api.device.models.MalcolmModel;


/**
 * A MalcolmDetectorModel implementation which contains a map of parameters. The (required) exposure time parameter is
 * stored in the map but has dedicated getter and setter methods.
 * <p>
 * This is intended to be a temporary model for use in testing, until we have dedicated classes for each Malcolm model
 *
 * @author Colin Palmer
 *
 */
public class MapMalcolmModel extends MalcolmModel {

	private static final String EXPOSURE_NAME = "exposure";

	private Map<String, Object> attributes;
	
	public MapMalcolmModel() {
		attributes = new LinkedHashMap<>();
	}
	public MapMalcolmModel(Map<String, Object> config) {
		attributes = config;
	}

	public double getExposureTime() {
		Object exposure = attributes.get(EXPOSURE_NAME);
		if (exposure instanceof Number) {
			return ((Number) exposure).doubleValue();
		} else {
			return 0.0;
		}
	}

	public void setExposureTime(double exposureTime) {
		attributes.put(EXPOSURE_NAME, Double.valueOf(exposureTime));
	}

	public Map<String, Object> getParameterMap() {
		return attributes;
	}

	public void setParameterMap(Map<String, Object> parameterMap) {
		attributes = parameterMap;
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
