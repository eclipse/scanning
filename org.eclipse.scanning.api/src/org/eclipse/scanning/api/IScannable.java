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

import org.eclipse.scanning.api.points.IPosition;

/**
 * 
 * Something that can take part in a sequenced scan which can have its position set.
 * 
 * This scannable is inspired by the original GDA8 Scannable. Very approximately Scannable
 * is IScannable<Object>. Some methods have be changed because the original Scannable had
 * some stale design. For instance IScanable has one setPosition which is blocking until 
 * it has moved. Also it is possible to set position knowing where the demand values of
 * other scannables are. This is important so that during a scan, each scannable when it is
 * told to move, has available all the other moves at that IPosition in the scan.
 * 
 * Important note - please do not extend this interface, it must be kept simple. In GDA9 extra
 * methods such as scanStart() etc. are dealt with by annotations. The idea is to have core
 * functionality as interface declarations and optional extensions as annotated methods.
 * See @ScanStart @ScanEnd @ScanFinally @ScanFault @ScanAbort etc.
 * 
 * @author Matthew Gerring
 * @param <T> the type of value returned by {@link #getPosition()}
 *
 */
public interface IScannable<T> extends 
						           /* A list of mostly defaulted and vanilla interfaces optionally used for scannables */
						           ILevel, INameable, ITimeoutable, 
						           IBoundable<T>, ITolerable<T>, IMonitoredDevice {
	
	/**
	 * Returns the current position of the Scannable. Called by ConcurentScan at the end of the point. 
	 * 
	 * @return Current position with an element for each input and extra field. null if their are no fields.
	 * @throws Exception
	 */
	public T getPosition() throws Exception;
	
	/**
	 * Moves to the position required, blocking until it is complete.
	 * Similar to moveTo(...) in GDA8
	 * 
	 * Same as calling setPosition(value, null);
	 * 
	 * @param value
	 * @throws Exception
	 * @return the new position attained by the device, if known. (Saves additional call to getPosition()) If not know the demand value is returned. NOTE if null is returned the system will call getPosition() again.
	 */
	default T setPosition(T value) throws Exception {
		return setPosition(value, null);
	}

	
	/**
	 * Moves to the position required, blocking until it is complete.
	 * Similar to moveTo(...) in GDA8
	 * 
	 * @param value that this scalar should take.
	 * @param position if within in a scan or null if not within a scan.
	 * @return the new position attained by the device, if known. (Saves additional call to getPosition()) If not know the demand value is returned. NOTE if null is returned the system will call getPosition() again.
	 * @throws Exception
	 */
	public T setPosition(T value, IPosition position) throws Exception;

	/**
	 * The unit is the unit in which the setPosition and getPosition values are in.
	 * @return String representation of unit which setPosition and getPosition are using.
	 */
	default String getUnit() {
		return null;
	}


}
