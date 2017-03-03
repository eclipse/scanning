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

import org.eclipse.scanning.api.ITimeoutable;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * 
 * Runs detectors in a task.
 * 
 * This is the equivalent to the GDA8 collectData() called on multiple
 * detectors with multiple threads and waiting for isBusy to be unset.
 * 
 * @author Matthew Gerring
 *
 */
class DeviceRunner extends LevelRunner<IRunnableDevice<?>> {

	private Collection<IRunnableDevice<?>>  devices;

	DeviceRunner(Collection<IRunnableDevice<?>> devices) {	
		this.devices = devices;
		
		long timeout = calculateTimeout(devices);
		setTimeout(timeout);
	}

	/**
	 * Calculate the timeout for the given devices. This is calculated as follows:
	 * <ul>
	 *   <li>If one of the devices is a malcolm device, then no timeout (i.e. {@link Long#MAX_VALUE});</li>
	 *   <li>Otherwise use the maxium of the timeout per device calculated as:
	 *     <ul>
	 *       <li>The timeout value for the device if specified;</li>
	 *       <li>Otherwise the exposure time.</li>
	 *     </ul>
	 *   </li>
	 *   <li>If the value calculated is less than or equal to 0, default to a timeout of 10 seconds.</li>
	 * </ul>
	 * 
	 * @param devices
	 * @return
	 */
	private long calculateTimeout(Collection<IRunnableDevice<?>> devices) {
		long time = Long.MIN_VALUE;
		for (IRunnableDevice<?> device : devices) {
			time = Math.max(time, getTimeout(device));
		}
		if (time<=0) time = 10; // seconds
		return time;
	}

	private long getTimeout(IRunnableDevice<?> device) {
		
		Object model = device.getModel();
		long timeout = -1;
		if (model instanceof ITimeoutable) {
			timeout = ((ITimeoutable)model).getTimeout();
			if (timeout<0 && model instanceof IDetectorModel) {
				IDetectorModel dmodel = (IDetectorModel)model;
				timeout = Math.round(dmodel.getExposureTime());
			}
		} else if (model instanceof IDetectorModel) {
			IDetectorModel dmodel = (IDetectorModel)model;
			timeout = (long)Math.ceil(dmodel.getExposureTime());
		}
		return timeout;
	}

	@Override
	protected Callable<IPosition> create(IRunnableDevice<?> detector, IPosition position) throws ScanningException {
		return new RunTask(detector, position);
	}
	
	@Override
	protected Collection<IRunnableDevice<?>> getDevices() {
		return devices;
	}

	private final class RunTask implements Callable<IPosition> {

		private IRunnableDevice<?>   detector;
		private IPosition            position;

		public RunTask(IRunnableDevice<?> detector, IPosition position) {
			this.detector = detector;
			this.position = position;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public IPosition call() throws Exception {
			if (detector instanceof IRunnableEventDevice) {
				((IRunnableEventDevice)detector).fireRunWillPerform(position);
			}
			try {
				if (detector instanceof AbstractRunnableDevice) ((AbstractRunnableDevice)detector).setBusy(true);
			    detector.run(position);
			} catch (Throwable ne) {
				abort(detector, position, ne);
			} finally {
				if (detector instanceof AbstractRunnableDevice) ((AbstractRunnableDevice)detector).setBusy(false);
			}
			if (detector instanceof IRunnableEventDevice) {
				((IRunnableEventDevice)detector).fireRunPerformed(position);
			}
			return null; // Faster if we are not adding new information.
		}

	}

}
