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

/**
 * 
 * Encapsulates a boolean array attribute as read from a malcolm device
 * 
 * @author Matt Taylor
 *
 */
public class BooleanArrayAttribute extends MalcolmAttribute {
	public static final String BOOLEANARRAY_ID = "malcolm:core/BooleanArrayMeta:";
	
	boolean value[];

	public void setValue(boolean[] value) {
		this.value = value;
	}

	@Override
	public boolean[] getValue() {
		return value;
	}
	
}
