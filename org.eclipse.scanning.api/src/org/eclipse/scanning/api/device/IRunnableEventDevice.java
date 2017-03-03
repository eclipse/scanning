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

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;

/**
 * 
 * A runnable device which also allows the ability to listen to when it
 * will run and after it has run. This allows for instance for a given
 * runnable device to configure it before the run method is called based on the 
 * position that the device will be run at. For instance for a device writing
 * different scan files depending on an outer scan, it can know the position
 * of the outer scan to generate the output file name and then call configure(...)
 * to ensure that the detector will write to the new location.
 * 
 * @author Matthew Gerring
 *
 * @param <T>
 */
public interface IRunnableEventDevice<T> extends IPausableDevice<T> {

	/**
	 * A listener which notifies when a run of the device will occur and after it
	 * has occurred. This can be used to modify the model before the next run.
	 * 
	 * @throws ScanningException
	 */
	public void addRunListener(IRunListener l) throws ScanningException;
	
	/**
	 * A listener which notifies when a run of the device will occur and after it
	 * has occurred. This can be used to modify the model before the next run.
	 * 
	 * @throws ScanningException
	 */
	public void removeRunListener(IRunListener l) throws ScanningException;

	/**
	 * Called during the scanning to notify the device that the run method will be
	 * called at a given position. This method is called on the same thread that the
	 * run() method is called on, e.g. parallel execution by level in the case of
	 * AcquisitionDevice.
	 * 
	 * @param position
	 */
	public void fireRunWillPerform(IPosition position)throws ScanningException;
	
	/**
	 * Called during the scanning to notify the device that the run method has been
	 * called at a given position. This method is called on the same thread that the
	 * run() method is called on, e.g. parallel execution by level in the case of
	 * AcquisitionDevice.
	 * 
	 * @param position
	 */
	public void fireRunPerformed(IPosition position)throws ScanningException;

	
	/**
	 * Called during the scanning to notify the device that the write method will be
	 * called at a given position, if the device implements one. 
	 * 
	 * This method is called on the same thread that the
	 * write() method is called on, e.g. parallel execution by level in the case of
	 * AcquisitionDevice.
	 * 
	 * <b>NOTE:</b> If the write method returns false, fireWritePerformed is not called. However
	 * fireWriteWillPerform will still be called. This is because until the device attempts
	 * to write we do not know if is has written.
	 * 
	 * @param position
	 */
	public void fireWriteWillPerform(IPosition position)throws ScanningException;
	
	/**
	 * 
	 * Called during the scanning to notify the device that the write method has been
	 * called at a given position, if the device implements one. 
	 * 
	 * This method is called on the same thread that the
	 * write() method is called on, e.g. parallel execution by level in the case of
	 * AcquisitionDevice.
	 * 
	 * <b>NOTE:</b> If the write method returns false, fireWritePerformed is not called. However
	 * fireWriteWillPerform will still be called. This is because until the device attempts
	 * to write we do not know if is has written.
	 * 
	 * @param position
	 */
	public void fireWritePerformed(IPosition position)throws ScanningException;

}
