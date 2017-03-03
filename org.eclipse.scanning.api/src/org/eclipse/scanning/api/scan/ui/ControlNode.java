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
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;

public class ControlNode extends AbstractControl implements INameable {

	private Object value;       // If the user sets a temporary value which does not go straight to the scannable
	private double increment=1;
	
	public ControlNode() {
	}
	public ControlNode(String name, double increment) {
		this();
		setName(name);
		this.increment     = increment;
	}
	public ControlNode(String parentName, String name, double increment) {
		this(name, increment);
		setParentName(parentName);
	}
	
	public String getScannableName() {
		return getName();
	}
	public void setScannableName(String scannableName) {
		setName(scannableName);
		value = null;
	}
	public double getIncrement() {
		return increment;
	}
	public void setIncrement(double increment) {
		this.increment = increment;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue(boolean direct, IScannableDeviceService cservice) throws Exception {
		
		Object value = null;
		if (!direct && getValue()!=null) {
			value = getValue();
		} else {
			final IScannable<Number> scannable = cservice.getScannable(getScannableName());
			value = scannable.getPosition();
			setValue(value);
		}
		return value;
	}

}
