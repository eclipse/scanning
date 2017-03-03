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
package org.eclipse.scanning.example.scannable;

import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Scalar;

public class MockStringScannable extends AbstractScannable<String> implements INameable {

	private String value;
	private String[] permittedValues;
	
	public MockStringScannable(String name, String pos, String... permittedValues) {
		setName(name);
		this.value = pos;
		this.permittedValues = permittedValues;
	}

	@Override
	public String getPosition() throws Exception {
		return value;
	}

	@Override
	public void setPosition(String value, IPosition position) throws Exception {
		this.value = value;
		delegate.firePositionChanged(getLevel(), new Scalar<String>(getName(), -1, value));
	}

	@Override
	public String[] getPermittedValues() throws Exception {
		return permittedValues;
	}

}
