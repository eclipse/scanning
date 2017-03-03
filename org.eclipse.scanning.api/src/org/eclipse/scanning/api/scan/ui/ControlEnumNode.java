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
package org.eclipse.scanning.api.scan.ui;

import org.eclipse.scanning.api.INameable;

public class ControlEnumNode extends AbstractControl implements INameable {

	private Enum value;
	
	public ControlEnumNode() {
	}
	
	public ControlEnumNode(String parentName, String name, Enum value) {
		setName(name);
		setParentName(parentName);
		this.value = value;
	}

	public String[] getChoices() {
		Enum[] values = value.getClass().getEnumConstants();
		String[] ret = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			ret[i] = values[i].name();
		}
		return ret;
	}

	public Enum getValue() {
		return value;
	}

	public void setValue(Enum value) {
		this.value = value;
	}
	
}
