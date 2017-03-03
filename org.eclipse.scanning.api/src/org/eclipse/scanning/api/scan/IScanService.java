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
package org.eclipse.scanning.api.scan;

import java.util.Collection;

import org.eclipse.scanning.api.device.IRunnableDeviceService;

/**
 * 
 * This interface is used to provide custom scanning methods 
 * to the IRunnableDeviceService.
 * 
 * @author Matthew Gerring
 *
 */
public interface IScanService extends IRunnableDeviceService {

	/**
	 * Used to register a scan participant. Once registered any scan
	 * created will use the particpant.
	 * 
	 * @param device
	 */
	void addScanParticipant(Object device);

	
	/**
	 * Used to remove a scan participant. Once registered any scan
	 * created will use the particpant it must be removed to stop this.
	 * 
	 * @param device
	 */
	void removeScanParticipant(Object device);

	/**
	 * The list of objects to be run as particpants with the scan.
	 * @return
	 */
	Collection<Object> getScanParticipants();
}
