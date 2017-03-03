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
package org.eclipse.scanning.api.points;

/**
 * Interface to check if a given point is contained
 * 
 * IPointContainer.containsPoint(...) != IROI.containsPoint(...) because
 * the IROI is in the data coordinates and the IPointContainer is in the 
 * motor coordinates.
 * 
 * @author Matthew Gerring
 *
 */
public interface IPointContainer {

	/**
	 * Check a given point is contained by the implementor of this interface.
	 * @return
	 */
	public boolean containsPoint(IPosition point);
	
}
