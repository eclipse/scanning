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
package org.eclipse.scanning.device.ui.device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Connects to server and gets the list of detectors with their current
 * models.
 * 
 * @author Matthew Gerring
 *
 */
class DetectorContentProvider implements IStructuredContentProvider {

	private static final Logger logger = LoggerFactory.getLogger(DetectorContentProvider.class);
	
	/**
	 * Remote device service.
	 */
	private IRunnableDeviceService dservice;
	private Collection<DeviceInformation<?>> infos;
	
	public DetectorContentProvider(IRunnableDeviceService dservice) throws EventException, ScanningException {
		this.dservice = dservice;
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		try {
		    infos = dservice.getDeviceInformation();
		} catch (Exception ne ) {
			logger.error("Cannot get device information!", ne);
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		
		try {
		    final List<DeviceInformation<?>> devices = new ArrayList<>(infos.size());
		    final boolean isHardware   = Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_HARDWARE);
		    final boolean isMalcolm    = Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_MALCOLM);
		    final boolean isProcessing = Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_PROCESSING);
		    infos.forEach(info-> {
		    	if (info.getDeviceRole()==DeviceRole.HARDWARE) {
		    		if (isHardware) devices.add(info);
		    	} else if (info.getDeviceRole()==DeviceRole.PROCESSING) {
		    		if (isProcessing) devices.add(info);
		    	}else if (info.getDeviceRole()==DeviceRole.MALCOLM) {
		    		if (isMalcolm) devices.add(info);
		    	}
		    });
		    // Since infos will be iterated through in an arbitrary order (from a user perspective), return a sorted 
		    // list for display 
		    devices.sort(Comparator.comparing(e -> e.getLabel()));
		    return devices.toArray(new DeviceInformation[devices.size()]);
		    
		} catch (Exception ne) {
			ne.printStackTrace();
			logger.error("Cannot get devices!", ne);
			return new DeviceInformation<?>[]{new DeviceInformation<Object>("No devices found")};
		}
	}
	
	public Collection<DeviceInformation<?>> getInfo() {
		return infos;
	}

}
