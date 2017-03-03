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
package org.eclipse.scanning.api.scan.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A subclass of {@link ScanModel} for when we need more precise control
 * over how the NXdata group or groups are created in the NeXus file for the scan.
 */
public class ScanDataModel extends ScanModel {
	
	private Map<String, ScanDeviceModel> scanDevices = new HashMap<String, ScanDeviceModel>();
	
	/**
	 * Adds a {@link ScanDeviceModel} for the device with the given name to this model.
	 * @param deviceName device name
	 * @param scanDeviceModel {@link ScanDeviceModel}
	 */
	public void addScanDevice(String deviceName, ScanDeviceModel scanDeviceModel) {
		scanDevices.put(deviceName, scanDeviceModel);
	}
	
	/**
	 * Returns the {@link ScanDeviceModel} for the device with the given name,
	 * or <code>null</code> if none
	 * @param deviceName device name
	 * @return {@link ScanDeviceModel} for the given name or <code>null</code>
	 */
	public ScanDeviceModel getScanDevice(String deviceName) {
		return scanDevices.get(deviceName);
	}
	
	/**
	 * Returns the list of names of those scan devices for which 
	 * {@link ScanDeviceModel}s have been added to this this {@link ScanDataModel}.
	 * Note that this is not the full list of devices
	 * in the scan, just those which have a {@link ScanDeviceModel} customizing
	 * how they are added to an NXdata group in the NeXus file for the scan
	 * @return names of scan devices for which there is a {@link ScanDataModel}.
	 */
	public List<String> getScanDeviceNames() {
		return new ArrayList<>(scanDevices.keySet());
	}
	
}
