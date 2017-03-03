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
package org.eclipse.scanning.device.ui.points;

import java.util.List;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.device.ui.util.ScanRegions;

class ScanRegionContentProvider implements IStructuredContentProvider {

	
	private IPlottingSystem<?> system;

	@Override
	public void dispose() {
		system = null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.system = (IPlottingSystem<?>)newInput;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		List<ScanRegion<IROI>> regions = ScanRegions.getScanRegions(system);
		if (regions==null) return new ScanRegion[]{new ScanRegion<IROI>()};
		return regions.toArray(new ScanRegion[regions.size()]);
	}

}
