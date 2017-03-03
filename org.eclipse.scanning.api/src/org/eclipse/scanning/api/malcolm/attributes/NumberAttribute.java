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
 * Encapsulates a number attribute as read from a malcolm device
 * 
 * @author Matt Taylor
 *
 */
public class NumberAttribute extends MalcolmAttribute {
	
	public static final String NUMBER_ID = "malcolm:core/NumberMeta:";
	
	String dtype;
	Number value;
	
	public String getDtype() {
		return dtype;
	}

	public void setDtype(String dtype) {
		this.dtype = dtype;
	}

	@Override
	public Number getValue() {
		return value;
	}

	public void setValue(Number value) {
		this.value = value;
	}
	
}
