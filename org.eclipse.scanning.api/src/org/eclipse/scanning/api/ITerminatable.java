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

/**
 * 
 * A device that may be terminated, such as a motor moving to a location,
 * may implement this interface.
 * 
 * TODO Should we have an annotation for the termination instead? The current methodology
 * is that core things like terminate and setPosition are in interfaces and extra things
 * which are optional to implement inside a scan are done with annotations.
 * 
 * @author Matthew Gerring
 *
 */
public interface ITerminatable {
	
	public enum TerminationPreference {
		CONTROLLED, PANIC;
	}

	/**
	 * ## Blocking Call ##
	 * 
	 * <p>Terminate the move of a given device such that if it is moving,
	 * it will stop where it is. The termination preference defines 
	 * if the device should do a panic stop if it supports such an
	 * action. Many devices have one type of stop, therefore in their
	 * cases the preference to panic or controlled (etc) is treated as
	 * aspiration rather than reality i.e. ignored.
	 * 
	 * <p>Important: This call is blocking, it does not only set a flag to 
	 * stop the move and return but it blocks until we are done moving! For instance
	 * it uses a CountDownLatch which is started on the move and released
	 * whenever the move is exited.
	 * 
	 * @param pref
	 */
	void terminate(TerminationPreference pref) throws Exception;
}
