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
package org.eclipse.scanning.api.device.models;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.ITimeoutable;
import org.eclipse.scanning.api.annotation.MinimumValue;


/**
 * Interface to be implemented by all detector models, to ensure they have an exposure time.
 *
 * @author Colin Palmer
 *
 */
public interface IDetectorModel extends ITimeoutable, INameable, IReflectedModel {

	/**
	 * Get the exposure time to be used for the detector, in seconds.
	 *
	 * @return the exposure time in seconds. Can be zero but not negative.
	 */
	@MinimumValue("0")
	public double getExposureTime();
	
}
