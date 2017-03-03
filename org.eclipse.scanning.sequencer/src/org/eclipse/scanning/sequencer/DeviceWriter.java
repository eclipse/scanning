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
package org.eclipse.scanning.sequencer;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * Reads detectors in a task.
 * 
 * This is the equivalent to the GDA8 readout() called on multiple
 * detectors with multiple threads and waiting for isBusy to be unset.
 * The latch method waits for the pool to exit if the run method is
 * called in non-blocking mode.
 * 
 * @author Matthew Gerring
 *
 */
final class DeviceWriter extends DeviceRunner {

	/**
	 * Checks each detector to find the maximum time
	 * that the await call should block for before
	 * the csan is terminated.
	 * 
	 * @param detectors
	 */
	DeviceWriter(Collection<IRunnableDevice<?>> detectors) {	
		super(detectors);
	}

	@Override
	protected Callable<IPosition> create(IRunnableDevice<?> device, IPosition position) throws ScanningException {
		if (!(device instanceof IWritableDetector<?>)) return null;
		return new WriteTask((IWritableDetector<?>)device, position);
	}

	private final class WriteTask implements Callable<IPosition> {

		private IWritableDetector<?> detector;
		private IPosition            position;

		public WriteTask(IWritableDetector<?> detector, IPosition position) {
			this.detector = detector;
			this.position = position;
		}

		@Override
		public IPosition call() throws Exception {
			if (detector instanceof IRunnableEventDevice) {
				((IRunnableEventDevice)detector).fireWriteWillPerform(position);
			}
			try {
				boolean wrote = detector.write(position);
				if (wrote) {
					if (detector instanceof IRunnableEventDevice) {
						((IRunnableEventDevice)detector).fireWritePerformed(position);
					}
				}
				return null; // faster if not adding new information
				
			} catch (Exception ne) {
				abort(detector, position, ne);
                throw ne;
			}
		}

	}

}
