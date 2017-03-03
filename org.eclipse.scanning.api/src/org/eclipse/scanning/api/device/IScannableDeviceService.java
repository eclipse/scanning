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
package org.eclipse.scanning.api.device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * Clients do not need to consume this service, it is used to provide connection
 * to devices which already exist, like those in GDA8.
 * <p>
 * A bundle implements this service and the scanning consumes the service. The
 * tests provide the service directly.
 * <p>
 * This service is designed to be implemented by GDA8 to provide scannables
 * (existing ones) to the new system. This can be done easily as
 * {@link IScannable} is a subset of Scannable in GDA8.
 * <p>
 * It also provides {@link IWritableDetector}'s. These are analogous to, but not
 * the same as Detector in GDA. The service wraps both gda.device.Detector into
 * {@link IWritableDetector} and gda.px.detector.Detector into
 * {@link IWritableDetector}. In the first case the model provides the
 * collection time and anything else required. In the later case the model
 * provides the file path and omegaStart required.
 * <p>
 * It mirrors the other connector services like the JMS one, which push the
 * details of getting connections outside the core scanning API using
 * declarative services.
 * 
 * @author Matthew Gerring
 *
 */
public interface IScannableDeviceService {
	
	/**
	 * Used to register a device. This is required so that spring may create
	 * detectors and call the register method by telling the detector to register
	 * itself.
	 * 
	 * @param device
	 */
	<T> void register(IScannable<T> scannable);

	/**
	 * Get the names of all scannables known to the connector.
	 * @return
	 * @throws ScanningException
	 */
	List<String> getScannableNames() throws ScanningException;

	/**
	 * Get a scannable by name.
	 * @param name name of scannable to find
	 * @return scannable, never <code>null</code>
	 * @throws ScanningException if no scannable with the given name could be found
	 */
	<T> IScannable<T> getScannable(String name) throws ScanningException;

	/**
	 * Returns the set of global metadata scannable names that should be added to all scans.
	 * This is used to support legacy (GDA8) spring configurations. Should not be called
	 * by client code.
	 * @return global metadata scannable names
	 */
	@Deprecated
	default Set<String> getGlobalMetadataScannableNames() {
		return Collections.emptySet();
	}
	
	/**
	 * Returns the set of the names required metadata scannables for the given scannable name.
	 * This is used to support legacy (GDA8) spring configurations. Should not be called
	 * by client code. 
	 * @param scannableName scannable to get required metadata scannable names for
	 * @return names of required metadata scannables for the scannable with the given name
	 */
	@Deprecated
	default Set<String> getRequiredMetadataScannableNames(String scannableName) {
		return Collections.emptySet();
	}
	
	/**
	 * Get a list of all the IScannables known to the service as a list of DeviceInformation<?>
	 * objects. DeviceInformation is JSON serializable and this method is 
	 * 
	 * @return
	 * @throws Exception
	 */
	default Collection<DeviceInformation<?>> getDeviceInformation() throws Exception {

		final Collection<String> names = getScannableNames();
		final Collection<DeviceInformation<?>> ret = new ArrayList<>(names.size());
		for (String name : names) {
	
			IScannable<?> device = getScannable(name);
			if (device==null) throw new ScanningException("There is no created device called '"+name+"'");
	
			DeviceInformation<?> info = new DeviceInformation<Object>(name);
			Util.merge(info, device);
			ret.add(info);
		}
		return ret;
	}


}

final class Util {
	static void merge(DeviceInformation<?> info, IScannable<?> device) throws Exception {
		info.setLevel(device.getLevel());
		info.setUnit(device.getUnit());
		info.setUpper(device.getMaximum());	
		info.setLower(device.getMinimum());
		info.setPermittedValues(device.getPermittedValues());
		info.setActivated(device.isActivated());
	}
}
