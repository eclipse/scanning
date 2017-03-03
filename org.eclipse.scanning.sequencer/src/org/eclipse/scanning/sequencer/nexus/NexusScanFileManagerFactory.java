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

import java.util.Set;

import org.eclipse.dawnsci.nexus.IMultipleNexusDevice;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.sequencer.ServiceHolder;

public class NexusScanFileManagerFactory {
	
	private static class DummyNexusScanFileManager implements INexusScanFileManager {

		@Override
		public void configure(ScanModel model) throws ScanningException {
			// do nothing
		}

		@Override
		public String createNexusFile(boolean async) throws ScanningException {
			// do nothing
			return null;
		}

		@Override
		public void flushNexusFile() throws ScanningException {
			// do nothing
		}

		@Override
		public void scanFinished() throws ScanningException {
			// do nothing
		}

		@Override
		public boolean isNexusWritingEnabled() {
			return false;
		}

		@Override
		public NexusScanInfo getNexusScanInfo() {
			return null;
		}

		@Override
		public Set<String> getExternalFilePaths() {
			return null;
		}

	}
	
	public static INexusScanFileManager createNexusScanFileManager(
			AbstractRunnableDevice<ScanModel> scanDevice) throws ScanningException {
		final ScanModel scanModel = scanDevice.getModel();
		if (scanModel.getFilePath() == null || ServiceHolder.getFactory() == null) {
			return new DummyNexusScanFileManager();
		}
		
		if (isMalcolmNexusScan(scanModel)) {
			return new MalcolmNexusScanFileManager(scanDevice);
		}
		
		return new NexusScanFileManager(scanDevice);
	}
	
	private static boolean isMalcolmNexusScan(ScanModel scanModel) {
		return scanModel.getDetectors().stream().anyMatch(det -> (det instanceof IMultipleNexusDevice));
	}

}
