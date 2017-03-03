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

import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * Interface for creating positioners. Normally
 * 
 * This is done by getting the IRunnableDeviceService which
 * is usually available and calling createPositioner() on that.
 * 
 * @author Matthew Gerring
 *
 */
public interface IPositionerService {

	
	/**
	 * This method sets the value of the scannables named to this position.
	 * It takes into account the levels of the scannbles. 
	 * It is blocking until all the scannables have reached the desired location.
	 * 
	 * @return
	 * @throws ScanningException
	 */
	IPositioner createPositioner() throws ScanningException;

}
