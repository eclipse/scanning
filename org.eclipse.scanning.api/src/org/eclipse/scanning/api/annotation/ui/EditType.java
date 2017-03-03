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
package org.eclipse.scanning.api.annotation.ui;

public enum EditType {

	/**
	 * Directly edit the value using a cell editor appropriate for the value.
	 */
	DIRECT,
	
	/**
	 * Edit using a dialog because value is too long to be edited in place.
	 */
	LONG,

	/**
	 * Popup a form with a table for editing the values because there is more than one.
	 */
	COMPOUND;
}
