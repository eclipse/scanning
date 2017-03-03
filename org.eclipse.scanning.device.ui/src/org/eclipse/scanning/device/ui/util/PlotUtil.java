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
package org.eclipse.scanning.device.ui.util;

import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.scanning.device.ui.vis.PlottingController;
import org.eclipse.scanning.device.ui.vis.VisualiseView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;

public class PlotUtil {
    
	public static IPlottingSystem<?> getRegionSystem() {
    	IViewPart part = getRegionView();
    	if (part !=null) return part.getAdapter(IPlottingSystem.class);
    	return null;
    }

	public static String getRegionViewName() {
    	IViewPart part = getRegionView();
    	if (part !=null) return part.getTitle();
    	return null;
    }
    /**
     * 
     * @return the plotting systems, if any, whose parent part respond to getAdapter(IROI.class)
     */
	public static IViewPart getRegionView() {
    	final IViewReference[] views = PageUtil.getPage().getViewReferences();
    	for (IViewReference vr : views) {
			IViewPart part = vr.getView(false);
			if (part==null) continue;
			final PlottingController controller = part.getAdapter(PlottingController.class);
			if (controller!=null) return part;
		}
    	// We know that there might be a view with the right plot on
    	IViewReference ref = PageUtil.getPage().findViewReference(VisualiseView.ID);
    	if (ref!=null) {
    		IViewPart part = ref.getView(true);
			final PlottingController controller = part.getAdapter(PlottingController.class);
			if (controller!=null) return part;
    	}
    	return null;
    }


}
