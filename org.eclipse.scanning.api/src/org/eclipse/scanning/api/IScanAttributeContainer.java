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
package org.eclipse.scanning.api;

import java.util.Collections;
import java.util.Set;

/**
 * Interface to allow the getting and setting of scan attributes.
 * These are the attributes that should be written when the scan is performed, e.g. to a NeXus file
 * 
 * @author Matthew Gerring
 *
 */
public interface IScanAttributeContainer extends INameable {
	
	/**
	 * 
	 * @return null if no attributes, otherwise collection of the names of the attributes set
	 */
	default Set<String> getScanAttributeNames() {
		return Collections.emptySet();
	}

	/**
	 * Set any attribute the implementing classes may provide
	 * 
	 * @param attributeName
	 *            is the name of the attribute
	 * @param value
	 *            is the value of the attribute
	 * @throws DeviceException
	 *             if an attribute cannot be set
	 */
	default <T> void setScanAttribute(String attributeName, T value) throws Exception {
		// Do nothing
	}

	/**
	 * Get the value of the specified attribute
	 * 
	 * @param attributeName
	 *            is the name of the attribute
	 * @return the value of the attribute
	 * @throws DeviceException
	 *             if an attribute cannot be retrieved
	 */
	default <T> T getScanAttribute(String attributeName) throws Exception {
		return null;
	}

}
