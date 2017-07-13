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
package org.eclipse.scanning.sequencer.nexus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.dawnsci.nexus.IMultipleNexusDevice;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusScanInfo.ScanRole;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.sequencer.ServiceHolder;
import org.eclipse.scanning.sequencer.SubscanModerator;

/**
 * Extends {@link NexusScanFileManager} to build nexus file for scans that include
 * Malcolm devices.
 */
public class MalcolmNexusScanFileManager extends NexusScanFileManager {

	private static final Map<NexusBaseClass, ScanRole> DEFAULT_SCAN_ROLES;
	
	private List<IMalcolmDevice<?>> malcolmDevices = null;
	
	static {
		DEFAULT_SCAN_ROLES = new HashMap<>();// not an enum map as most base classes not mapped
		DEFAULT_SCAN_ROLES.put(NexusBaseClass.NX_DETECTOR, ScanRole.DETECTOR);
		DEFAULT_SCAN_ROLES.put(NexusBaseClass.NX_MONITOR, ScanRole.MONITOR_PER_POINT);
		DEFAULT_SCAN_ROLES.put(NexusBaseClass.NX_POSITIONER, ScanRole.SCANNABLE);
	}
	
	public MalcolmNexusScanFileManager(AbstractRunnableDevice<ScanModel> scanDevice) throws ScanningException {
		super(scanDevice);
		
		malcolmDevices = scanDevice.getModel().getDetectors().stream().
				filter(IMalcolmDevice.class::isInstance).map(IMalcolmDevice.class::cast).
				collect(Collectors.toList());
	}
	
	@Override
	protected Map<ScanRole, List<NexusObjectProvider<?>>> extractNexusProviders() throws ScanningException {
		Map<ScanRole, List<NexusObjectProvider<?>>> nexusProviders = super.extractNexusProviders();
		
		try {
			for (NexusObjectProvider<?> nexusProvider : getNexusObjectProvidersForMalcolmDevices()) {
				ScanRole role = getScanRole(nexusProvider);
				nexusProviders.get(role).add(nexusProvider);
			}
		} catch (Exception e) {
			handleException(e);
		}
		
		return nexusProviders;
	}

	protected SolsticeScanMonitor createSolsticeScanMonitor(ScanModel scanModel) {
		SolsticeScanMonitor scanPointsWriter = super.createSolsticeScanMonitor(scanModel);
		scanPointsWriter.setMalcolmScan(true);
		return scanPointsWriter;
	}
	
	protected int getScanRank(ScanModel model) throws ScanningException {
		SubscanModerator moderator = new SubscanModerator(model.getPositionIterable(),
				null, model.getDetectors(), ServiceHolder.getGeneratorService());
		return getScanRank(moderator.getOuterIterable());
	}

	private ScanRole getScanRole(NexusObjectProvider<?> nexusProvider) throws ScanningException {
		// Malcolm devices should only return NXdetectors, NXmonitors and NXpositions
		// based on the type of device.
		ScanRole scanRole = DEFAULT_SCAN_ROLES.get(nexusProvider.getNexusBaseClass());
		if (scanRole == null) {
			throw new ScanningException("Unable to determine scan role for nexus object of type " +
					nexusProvider.getNexusBaseClass());
		}
		return scanRole;
	}
	
	private void handleException(Exception e) throws ScanningException {
		if (e instanceof RuntimeException && e.getCause() != null) {
			e = (Exception) e.getCause();
		}
		if (e instanceof ScanningException) throw (ScanningException) e;
		throw new ScanningException(e);
	}
	
	private List<NexusObjectProvider<?>> getNexusObjectProvidersForMalcolmDevices() {
		return malcolmDevices.stream().filter(IMultipleNexusDevice.class::isInstance)
				.map(IMultipleNexusDevice.class::cast).flatMap(d -> getNexusProviders(d).stream())
				.collect(Collectors.toList());
	}
	
	private List<NexusObjectProvider<?>> getNexusProviders(IMultipleNexusDevice malcolmDevice) {
		try {
			return malcolmDevice.getNexusProviders(getNexusScanInfo());
		} catch (NexusException e) {
			throw new RuntimeException(e);
		}
	}

}
