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

import java.util.Map;

import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.data.AxisDataDevice;

public class NexusScanFileDataBuilder {
	
	public Map<NexusObjectProvider<?>, AxisDataDevice<?>> dataDevices;

	public NexusScanFileDataBuilder(Map<NexusObjectProvider<?>, AxisDataDevice<?>> dataDevices) {
		this.dataDevices = dataDevices;
	}
}