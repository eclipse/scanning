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

import java.util.List;

/**
 * Model for a malcolm device.
 */
public interface IMalcolmModel extends IDetectorModel {
	
	/**
	 * Get the directory where malcolm will write its h5 files to. The directory should exist at
	 * the point that the malcolm device is configured, malcolm is not responsible for creating it.
	 * @return path of malcolm output directory
	 */
	public String getFileDir();
	
	/**
	 * Get the names of the scan axes that are controlled by malcolm.
	 * @return axes to move
	 */
	public List<String> getAxesToMove();

}
