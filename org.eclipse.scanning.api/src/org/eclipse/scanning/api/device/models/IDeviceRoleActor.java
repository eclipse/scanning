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
package org.eclipse.scanning.api.device.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * A device which has a clear role in the system.
 * {@link DeviceRole}
 * 
 * @author Matthew Gerring
 *
 */
public interface IDeviceRoleActor {

	/**
	 * The role of the device.
	 * @return role of the device
	 */
	DeviceRole getRole();
	
	/**
	 * The role of the device.
	 * @param role role of the device
	 */
	void setRole(DeviceRole role);
	
	/**
	 * Returns a set of the scan modes that this device can participate in.
	 * @return possible roles of the device
	 */
	Set<ScanMode> getSupportedScanModes();
	
}
