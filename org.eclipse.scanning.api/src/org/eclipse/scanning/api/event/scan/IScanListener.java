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
package org.eclipse.scanning.api.event.scan;

import java.util.EventListener;


/**
 * Listener for scan events. The listener will be notified if
 * any scans are made active or inactive. In order to listen
 * for a given scan a listener must be added and then have its
 * scanId registered for that scan. 
 * 
 * @author Matthew Gerring
 *
 */
public interface IScanListener extends EventListener {
		
	/**
	 * Called by all broadcast events
	 * @param evt
	 */
	default void scanEventPerformed(ScanEvent evt) {
		// default implementation does nothing, subclasses should override as necessary
	}
	
	/**
	 * Called to notify that state was changed. Happens in addition to
	 * scanEventPerformed(...) when state changes. For instance from IDLE to CONFIGURED
	 * CONFIGURED to RUNNING etc.
	 * 
	 * @param evt
	 */
	default void scanStateChanged(ScanEvent evt) {
		// default implementation does nothing, subclasses should override as necessary
	}
}
