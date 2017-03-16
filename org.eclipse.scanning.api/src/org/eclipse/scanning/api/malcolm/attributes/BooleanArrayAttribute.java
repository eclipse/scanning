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

import java.util.Arrays;

/**
 * 
 * Encapsulates a boolean array attribute as read from a malcolm device
 * 
 * @author Matt Taylor
 *
 */
public class BooleanArrayAttribute extends MalcolmAttribute<boolean[]> {
	public static final String BOOLEANARRAY_ID = "malcolm:core/BooleanArrayMeta:";
	
	private boolean value[];

	public void setValue(boolean[] value) {
		this.value = value;
	}

	@Override
	public boolean[] getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(value);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BooleanArrayAttribute other = (BooleanArrayAttribute) obj;
		if (!Arrays.equals(value, other.value))
			return false;
		return true;
	}
	
}
