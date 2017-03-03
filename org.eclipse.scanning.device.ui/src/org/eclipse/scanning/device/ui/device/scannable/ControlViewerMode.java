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
package org.eclipse.scanning.device.ui.device.scannable;

public enum ControlViewerMode {

	/**
	 * Links value to hardware, applying value when users change it.
	 */
	DIRECT, 
	
	/**
	 * Values default to the current hardware values but
	 * the user then enters new values which are set in the 
	 * ControlNode but not the hardware. Later use ControlTree.toPostion() to
	 * create an IPosition of the values to use with an IPositioner.
	 */
	INDIRECT_NO_SET_VALUE;
	
	/**
	 * Totally unlinked. The values are edited abstractly
	 * and returned as an IPosition
	 */
	// UNKLINKED               // Needed?
	
	public boolean isDirectlyConnected() {
		return this==DIRECT;
	}
	
	public boolean isDefaultValue() {
		return this==DIRECT || this == INDIRECT_NO_SET_VALUE;
	}
}
