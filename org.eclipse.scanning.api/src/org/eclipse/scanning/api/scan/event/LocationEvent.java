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
package org.eclipse.scanning.api.scan.event;

import java.util.EventObject;

public class LocationEvent extends EventObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5183358734729833586L;

	public LocationEvent(Location source) {
		super(source);
	}

	public Location getLocation() {
		return (Location)getSource();
	}

	@Override
	public String toString() {
		return "LocationEvent [getSource()=" + getSource() + "]";
	}

}
