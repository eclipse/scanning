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

public class ControlFileNode extends AbstractControl implements INameable {

	private String file;
	
	public ControlFileNode() {
	}
	public ControlFileNode(String name) {
		this();
		setName(name);
	}
	public ControlFileNode(String parentName, String name) {
		this(name);
		setParentName(parentName);
	}

	
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	
}
