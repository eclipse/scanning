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

import org.eclipse.scanning.api.annotation.UiHidden;

public interface ITimeoutable {

	/**
	 * This is the timeout time in seconds which defaults to
	 * -1
	 * 
	 * If set the default timeout for an action on a device 
	 * will use this value. For instance for detectors the run
	 * and write time will timeout if this field is set>0 or
	 * 10 seconds if none of the detector models have this field
	 * set. For scannables the IScannable interface extends this
	 * interface. If any motor at a given level implements this
	 * timeout, this time out (or the max of all the timeouts)
	 * will be used. If none are set the default is three minutes.
	 * 
	 * @return
	 */
	@UiHidden
	default long getTimeout() {
		return -1;
	}
	
	/**
	 * 
	 * @param time in seconds
	 */
	@UiHidden
	default void setTimeout(long time) {
		// Does nothing
	}

}
