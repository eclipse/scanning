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

import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.INamedNode;

public class ControlGroup extends AbstractControl implements INameable {

	
	public ControlGroup() {
		
	}

	public List<INamedNode> getControls() {
		return Arrays.asList(getChildren());
	}

	public void setControls(List<INamedNode> controls) {
		for (INamedNode scannableControl : controls) {
			scannableControl.setParentName(getName());
		}
		this.setChildren(controls.toArray(new INamedNode[controls.size()]));
	}
}
