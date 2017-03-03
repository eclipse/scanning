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
package org.eclipse.scanning.test.event.queues.mocks;

import java.util.List;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.IPositioner;

public class MockPositioner implements IPositioner {
	
	private IPosition pos;
	private boolean aborted = false;
	private Boolean moveComplete;

	@Override
	public void addPositionListener(IPositionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePositionListener(IPositionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setPosition(IPosition position) throws ScanningException,
			InterruptedException {
		
		moveComplete = false;
		Thread.sleep(100);
		
		//This is to test positioning failing.
		if (position.getNames().contains("BadgerApocalypseButton") && position.get("BadgerApocalypseButton").equals("pushed")) {
			throw new ScanningException("The badger apocalypse cometh! (EXPECTED - we pressed the button...)");
		}
		
		pos = position;
		Thread.sleep(250);
		
		moveComplete = true;
		return true;
	}

	@Override
	public IPosition getPosition() throws ScanningException {
		return pos;
	}

	@Override
	public List<IScannable<?>> getMonitors() throws ScanningException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMonitors(List<IScannable<?>> monitors)
			throws ScanningException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMonitors(IScannable<?>... monitors) throws ScanningException {
		// TODO Auto-generated method stub

	}

	@Override
	public void abort() {
		aborted = true;
	}
	

	@Override
	public void close() {
		
	}
	
	public boolean isAborted() {
		return aborted;
	}
	
	public boolean isMoveComplete() {
		return moveComplete;
	}

}
