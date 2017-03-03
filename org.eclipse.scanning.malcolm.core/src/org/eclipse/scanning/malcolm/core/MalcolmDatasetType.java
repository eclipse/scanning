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
package org.eclipse.scanning.malcolm.core;

import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusScanInfo.ScanRole;

public enum MalcolmDatasetType {
	
	PRIMARY(ScanRole.DETECTOR, NexusBaseClass.NX_DETECTOR),
	SECONDARY(ScanRole.DETECTOR, NexusBaseClass.NX_DETECTOR),
	MONITOR(ScanRole.MONITOR, NexusBaseClass.NX_MONITOR),
	POSITION_VALUE(ScanRole.SCANNABLE, NexusBaseClass.NX_POSITIONER),
	POSITION_SET(ScanRole.SCANNABLE, NexusBaseClass.NX_POSITIONER);
	
	private final ScanRole scanRole;
	private final NexusBaseClass nexusBaseClass;
	
	private MalcolmDatasetType(ScanRole scanRole, NexusBaseClass nexusBaseClass) {
		this.scanRole = scanRole;
		this.nexusBaseClass = nexusBaseClass;
	}
	
	public ScanRole getScanRole() {
		return scanRole;
	}
	
	public NexusBaseClass getNexusBaseClass() {
		return nexusBaseClass;
	}

}
