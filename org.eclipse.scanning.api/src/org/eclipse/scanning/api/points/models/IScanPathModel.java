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
package org.eclipse.scanning.api.points.models;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

/**
 * A model which defines the movement parameters for one or more dimensions of a scan.
 *
 * @author Colin Palmer
 *
 */
public interface IScanPathModel {

	/**
	 * A 'friendly' name for display to the user
	 *
	 * @return the name of this path
	 */
	public String getName();
	
    /**
     * The names of the axes which will be scanned by this model.
     * @return
     */
	default List<String> getScannableNames() {
		return Arrays.asList(getName()); 
	}

	/**
	 * Property change support
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Property change support
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener);

}