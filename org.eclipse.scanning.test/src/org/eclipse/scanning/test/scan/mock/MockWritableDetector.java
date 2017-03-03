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
package org.eclipse.scanning.test.scan.mock;

import org.eclipse.january.dataset.Random;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;

public class MockWritableDetector extends AbstractRunnableDevice<MockDetectorModel> implements IWritableDetector<MockDetectorModel> {
	
	public MockWritableDetector() {
		super(null);
	}
	
	public MockWritableDetector(String name) {
		this();
		setName(name);
	}
		
	@Override
	public void run(IPosition pos) throws ScanningException {
		try {
			if (getModel().getExposureTime()>0) Thread.sleep((long)(getModel().getExposureTime()*1000));
			getModel().setRan(getModel().getRan()+1);
		} catch (Exception ne) {
			throw new ScanningException("Cannot to do readout", ne);
		}
		
		if (getModel().getAbortCount()>-1 && getModel().getAbortCount()<=getModel().getRan()) {
			throw new ScanningException("The detector had a problem running! This exception should stop the scan running!");
		}
	}
	
	@Override
	public boolean write(IPosition position) throws ScanningException {
		
		// Grab some memory for a given image size to simulate a CPU detector.
		if (model.isCreateImage()) {
			Random.rand(model.getImageSize());
		}
		
		getModel().setWritten(getModel().getWritten()+1);
		
		return true;
	}

	@Override
	public String toString() {
		return "MockDetector [level=" + getLevel() + ", name=" + getName() +  "]";
	}

	@Override
	public void configure(MockDetectorModel model) throws ScanningException {
		setModel(model);
	}

	@Override
	public void abort() throws ScanningException {

	}

	@Override
	public void pause() throws ScanningException {
        try {
	        setDeviceState(DeviceState.SEEKING);
	        Thread.sleep(100);
	        setDeviceState(DeviceState.PAUSED);
		} catch (Exception e) {
			throw new ScanningException(e);
		}
	}

	@Override
	public void resume() throws ScanningException {
        try {
			setDeviceState(getModel()!=null ? DeviceState.READY : DeviceState.IDLE);
		} catch (Exception e) {
			throw new ScanningException(this, e);
		}
  	}
}
