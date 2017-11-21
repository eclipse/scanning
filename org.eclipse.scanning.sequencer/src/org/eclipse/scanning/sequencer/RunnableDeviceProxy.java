/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.sequencer;

import org.eclipse.scanning.api.annotation.scan.LevelEnd;
import org.eclipse.scanning.api.annotation.scan.LevelStart;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanAbort;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.annotation.scan.ScanFault;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanPause;
import org.eclipse.scanning.api.annotation.scan.ScanResume;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.annotation.scan.WriteComplete;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.LevelInformation;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnableDeviceProxy<T> extends AbstractRunnableDevice<T> {
	private static final Logger logger = LoggerFactory.getLogger(RunnableDeviceProxy.class);

	private AbstractRunnableDeviceDelegate delegate;

	public RunnableDeviceProxy() {
		super(ServiceHolder.getRunnableDeviceService());
	}

	// AbstractRunnableDevice<AreaDetectorRunnableDeviceModel>

	@Override
	public void configure(Object model) throws ScanningException {
		logger.trace("configure({}) on {}", model, getName());
		setDeviceState(DeviceState.CONFIGURING);

		// Get the detector by name defined in the model
		if (delegate == null) throw new ScanningException("No delegate defined for " + getName());

		try {
			delegate.configure(model);
		} catch (Exception e) {
			setDeviceState(DeviceState.FAULT);
			throw new ScanningException("Failed configuring detector " + getName(), e);
		}
		setDeviceState(DeviceState.ARMED);
	}

	/**
	 * Allow delegates to set the device state.
	 *
	 * @param nstate New State
	 */
	@Override
	public void setDeviceState(DeviceState nstate) throws ScanningException {
		super.setDeviceState(nstate);
	}

	// Delegated interface IRunnableDevice<AreaDetectorRunnableDeviceModel> methods

	@Override
	public void run(IPosition position) throws ScanningException, InterruptedException {
		logger.trace("run({}) on {}", position, getName());
		delegate.run(position);
	}

	// Delegated annotated methods

	@PreConfigure
	public void preConfigure(Object model, ScanBean scanBean, IPublisher<?> publisher) throws ScanningException {
		logger.trace("preConfigure({}, {}, {}) on {}", model, scanBean, publisher, getName());
		delegate.preConfigure(model, scanBean, publisher);
	}

	@PostConfigure
	public void postConfigure(Object model, ScanBean scanBean, IPublisher<?> publisher) throws ScanningException {
		logger.trace("postConfigure({}, {}, {}) on {}", model, scanBean, publisher, getName());
		delegate.postConfigure(model, scanBean, publisher);
	}

	@LevelStart
	public void levelStart(LevelInformation info) throws ScanningException {
		logger.trace("levelStart({}) on {}", info, getName());
		delegate.levelStart(info);
	}

	@LevelEnd
	public void levelEnd(LevelInformation info) throws ScanningException {
		logger.trace("levelEnd({}) on {}", info, getName());
		delegate.levelEnd(info);
	}

	@PointStart
	public void pointStart(IPosition point) throws ScanningException {
		logger.trace("pointStart({}) on {} stepIndex={}", point, getName(), point.getStepIndex());
		delegate.pointStart(point);
	}

	@PointEnd
	public void pointEnd(IPosition point) throws ScanningException {
		logger.trace("pointEnd({}) on {} stepIndex={}", point, getName(), point.getStepIndex());
		delegate.pointEnd(point);
	}

	@ScanStart
	public void scanStart(ScanInformation info) throws ScanningException {
		logger.trace("scanStart({}) on {} filePath={}", info, getName(), info.getFilePath());
		delegate.scanStart(info);
	}

	@ScanEnd
	public void scanEnd(ScanInformation info) throws ScanningException {
		logger.trace("scanEnd({}) on {} filePath={}", info, getName(), info.getFilePath());
		delegate.scanEnd(info);
	}

	@WriteComplete
	public void writeComplete(ScanInformation info) throws ScanningException {
		logger.trace("writeComplete({}) on {} filePath={}", info, getName(), info.getFilePath());
		delegate.writeComplete(info);
	}

	@ScanAbort
	public void scanAbort(ScanInformation info) throws ScanningException {
		logger.trace("scanAbort({}) on {}", info, getName());
		delegate.scanAbort(info);
	}

	@ScanFault
	public void scanFault(ScanInformation info) throws ScanningException {
		logger.trace("scanFault({}) on {}", info, getName());
		delegate.scanFault(info);
	}

	@ScanFinally
	public void scanFinally(ScanInformation info) throws ScanningException {
		logger.trace("scanFinally({}) on {}", info, getName());
		delegate.scanFinally(info);
	}

	@ScanPause
	public void scanPaused() throws ScanningException {
		logger.trace("scanPaused() on {}", getName());
		delegate.scanPause();
	}

	@ScanResume
	public void scanResumed() throws ScanningException {
		logger.trace("scanResumed() on {}", getName());
		delegate.scanResume();
	}

	// Class methods

	public AbstractRunnableDeviceDelegate getDelegate() {
		return delegate;
	}

	public void setDelegate(AbstractRunnableDeviceDelegate delegate) {
		logger.trace("setDelegate({}) on {}", delegate, getName());

		this.delegate = delegate;
		if (delegate.getRunnableDeviceProxy() != this ) {
			throw new RuntimeException("Delegates runnable device is not this!");
		}
	}

}
