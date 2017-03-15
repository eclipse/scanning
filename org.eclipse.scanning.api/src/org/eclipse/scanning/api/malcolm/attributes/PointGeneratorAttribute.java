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
package org.eclipse.scanning.api.malcolm.attributes;

import java.util.Map;

/**
 * 
 * Encapsulates a PointGenerator attribute as read from a malcolm device.
 * The point generator is stored as a map.
 * 
 * @author Matt Taylor
 *
 */
public class PointGeneratorAttribute extends MalcolmAttribute<Map<?,?>> {
	public static final String POINTGENERATOR_ID = "malcolm:core/PointGenerator:";
	
	private Map<?,?> value;

	@Override
	public Map<?,?> getValue() {
		return value;
	}

	public void setValue(Map<?,?> value) {
		this.value = value;
	}
	
}
