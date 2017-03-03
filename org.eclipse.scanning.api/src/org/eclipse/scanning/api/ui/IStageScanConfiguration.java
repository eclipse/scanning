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

package org.eclipse.scanning.api.ui;

import java.beans.PropertyChangeListener;

/**
 * Interface to allow the current configuration of a mapping scan
 * (or how it is displayed in the UI) to be determined
 * 
 */
public interface IStageScanConfiguration {

	/**
	 * Return the name of the fast scan axis
	 * @return fast axis name
	 */
	String getActiveFastScanAxis();

	/**
	 * Return the name of the fast scan axis
	 * @return slow axis name
	 */
	String getActiveSlowScanAxis();
	
	/**
	 * Returns the name of the associated axis. This may be the z-axis, for example.
	 * @return associated axis name
	 */
	String getAssociatedAxis();
	
	/**
	 * Add a property change listener
	 * 
	 * @param listener
	 */
	void addPropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Remove a property change listener
	 * 
	 * @param listener
	 */
	void removePropertyChangeListener(PropertyChangeListener listener);


	

}