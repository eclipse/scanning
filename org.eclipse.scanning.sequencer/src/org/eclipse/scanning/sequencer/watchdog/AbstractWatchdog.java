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
package org.eclipse.scanning.sequencer.watchdog;

import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.device.IDeviceController;
import org.eclipse.scanning.api.device.IDeviceWatchdog;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceWatchdogModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.sequencer.ServiceHolder;

public abstract class AbstractWatchdog implements IDeviceWatchdog {
	
	protected DeviceWatchdogModel model;
	protected IDeviceController  controller;
	protected boolean active = false;
	
	/**
	 * Name should be set by spring as it is the mechanism by
	 * which a watchdog can be retrieved and turned on or off.
	 */
	private String name = getClass().getSimpleName(); 
	
	/**
	 * A disabled watchdog will not monitor when a scan runs.
	 */
	private boolean enabled=true;

	public AbstractWatchdog() {
		this(null);
	}
	public AbstractWatchdog(DeviceWatchdogModel model2) {
		this.model = model2;
	}
	
	abstract String getId();
	
	public DeviceWatchdogModel getModel() {
		return model;
	}
	public void setModel(DeviceWatchdogModel model) {
		this.model = model;
	}

	
	protected long getValueMs(IPosition ipos, String name, String unit) {
		double pos = ipos.getValue(name);
		return getValueMs(pos, unit);
	}

	protected long getValueMs(String name, String unit) throws Exception {
	    IScannable<Number> scannable = getScannable(name);
		return getValueMs(scannable.getPosition().doubleValue(), unit);
	}

	protected long getValueMs(double pos, String unit) {
		TimeUnit tu = getTimeUnit(unit);
		switch (tu) {
			case MINUTES: return Math.round(pos * 1000 * 60);
			case SECONDS: return Math.round(pos * 1000);
			case MILLISECONDS: return Math.round(pos);
			default:
				// sanity check: not actually possible as getTimeUnit only return the units above
				throw new RuntimeException("Unexpected unit " + tu);
		}
		
	}

	protected <T> IScannable<T> getScannable(String name) throws ScanningException {
		if (ServiceHolder.getRunnableDeviceService()==null) return null;
		IScannableDeviceService cservice = ServiceHolder.getRunnableDeviceService().getDeviceConnectorService();
		return cservice.getScannable(name);
	}
	
	private static final TimeUnit getTimeUnit(String unit) {
		TimeUnit tu = TimeUnit.SECONDS; // if time unit not specified default to seconds
		if (unit != null) {
			if ("s".equalsIgnoreCase(unit))  tu = TimeUnit.SECONDS;
			if ("seconds".equalsIgnoreCase(unit))  tu = TimeUnit.SECONDS;
			if ("ms".equalsIgnoreCase(unit)) tu = TimeUnit.MILLISECONDS;
			if ("milliseconds".equalsIgnoreCase(unit)) tu = TimeUnit.MILLISECONDS;
			if ("m".equalsIgnoreCase(unit)) tu = TimeUnit.MINUTES;
			if ("min".equalsIgnoreCase(unit)) tu = TimeUnit.MINUTES;
		}
		return tu;
	}

	/**
	 * Used by spring
	 */
	@Override
	public void activate() {
		ServiceHolder.getWatchdogService().register(this);
	}
	public void deactivate() {
		ServiceHolder.getWatchdogService().unregister(this);
	}
	public IDeviceController getController() {
		return controller;
	}
	public void setController(IDeviceController controller) {
		this.controller = controller;
	}

	@ScanStart
	public void scanStarted() {
		active = true;
	}
	
	@ScanFinally
	public void scanFinally() {
		active = false;
	}
	
	@Override
	public boolean isActive() {
		return active;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
