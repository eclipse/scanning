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
package org.eclipse.scanning.api.device;

import java.util.List;

import org.eclipse.scanning.api.device.models.DeviceWatchdogModel;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * A device controller takes input from several clients
 * and ensures that each of their preferences about pausing
 * the device being controlled is taken into account.
 * 
 * For instance three classes control a scan, a class from the
 * user interface, one from the topup monitor and one from the
 * beam stop monitor, call them gui, topup, shutter
 * 
 * If any one of gui, topup, shutter pause, the device gets paused.
 * If all of gui, topup and shutter say resume, the device gets resumed.
 * 
 * gui, topup and shutter have an id that goes in a map. They must always
 * use the same id to send commands to the controller or it will not work.
 * 
 * @author Matthew Gerring
 *
 */
public interface IDeviceController {
	
	/**
	 * The list of objects which will participate in a scan.
	 * @return
	 */
	<T> List<T> getObjects();
	
	/**
	 * The device we are controlling.
	 * @return
	 */
	IPausableDevice<?> getDevice();

	/**
	 * Make a pause
	 * @param id
	 */
	void pause(String id, DeviceWatchdogModel model) throws ScanningException;
	
	/**
	 * Make a resume
	 * @param id
	 */
	void resume(String id) throws ScanningException;
	
	/**
	 * Make a seek
	 * @param id
	 */
	void seek(String id, int step)throws ScanningException, InterruptedException;
	
	/**
	 * Make an abort
	 * @param id
	 */
	void abort(String id) throws ScanningException;

	/**
	 * 
	 * @return the name of the device that we are controlling
	 */
	String getName();
	
	/**
	 * 
	 * @return true if the devices (watchdogs) in this contoller all think they are running in a scan.
	 */
	boolean isActive();
}
