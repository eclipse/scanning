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
package org.eclipse.scanning.api.scan.event;

import java.util.List;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;


/**
 * This class deals with setting the beamline to a specific position.
 * <pre>
IPositioner     pos    = sservice.createPositioner();
pos.setPosition(new MapPosition("x:1, y:2"));

//'sservice' is the scanning service.  setPosition moves the motors to their positions by level until they are all there and then returns. It is the same thing as used to move to a scan point during a scan. 
//The MapPosition is just a convenience for Java. 
//In Jython since the MapPosition also takes a map, a dictionary can be used:

pos.setPosition(new MapPosition({'x':1, 'y':2}));
// returns after the position is reached moving the motors based on level, lowest first.

</pre>
 * @author Matthew Gerring
 *
 */
public interface IPositioner extends IPositionListenable {

	/**
	 * This method moves to the position passed in and returns when the move
	 * is complete.
	 * 
	 * It takes into account the levels of the scannbles and moves them
	 * using a separate thread for each one. An executor service is used 
	 * for each level and attempts to process all motors on a given level
	 * before existing.
	 * 
	 * It is blocking until all the scannables have reached the desired location.
	 * 
	 * @param position
	 * @return
	 * @return false if the position could not be reached. Normally an exception will be thrown if this is the case.
	 * @throws ScanningException
	 */
	boolean setPosition(IPosition position) throws ScanningException, InterruptedException;

	/**
	 * This method will return null if the positioner has not been told to move to a
	 * position. If it has it will read the scannable values from the last position it 
	 * was told to go to and return a position of those values. For instance if the
	 * IPositioner was last told to go to x:1 and y:2, it would return the current values
	 * of the scannables x and y by reading them again.
	 * 
	 * @param position
	 * @return Read position
	 * @throws ScanningException
	 */
	IPosition getPosition() throws ScanningException;
	
	/**
	 * Monitors are a set of scannables which will have setPosition(null, IPosition) called and
	 * may block until happy or write an additional record during the scan. 
	 * 
	 * For instance the beam current or ambient temperature could be monitors which are written
	 * to the NeXus file. Monitors are sorted into level with the scannbles of the current position.
	 * 
	 * @return monitors
	 * @throws ScanningException
	 */
	List<IScannable<?>> getMonitors()  throws ScanningException;
	
	/**
	 * Monitors are a set of scannables which will have setPosition(null, IPosition) called and
	 * may block until happy or write an additional record during the scan. 
	 * 
	 * For instance the beam current or ambient temperature could be monitors which are written
	 * to the NeXus file. Monitors are sorted into level with the scannbles of the current position.
     *
	 * @param monitors
	 * @throws ScanningException
	 */
	void setMonitors(List<IScannable<?>> monitors) throws ScanningException;

	/**
	 * Monitors are a set of scannables which will have setPosition(null, IPosition) called and
	 * may block until happy or write an additional record during the scan. 
	 * 
	 * For instance the beam current or ambient temperature could be monitors which are written
	 * to the NeXus file. Monitors are sorted into level with the scannbles of the current position.
     *
	 * @param monitors
	 * @throws ScanningException
	 */
	void setMonitors(IScannable<?>... monitors) throws ScanningException;

	/**
	 * Calling this method instructs the ExecutorService running the levels to shutdownNow().
	 * 
	 * from ExecutorService.shutdownNow() : "Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution."
	 */
	void abort();
	
	/**
	 * Calling this method tells the thread pool to close in the positioner.
	 * This frees up threads and the positioner may still be used (it will create
	 * a new thread pool if reused after it has been closed).
	 */
	void close();
}
