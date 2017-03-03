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
package org.eclipse.scanning.device.ui.vis;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.dawnsci.plotting.api.IPlottingService;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.region.IRegionSystem;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.scanning.api.IModelProvider;
import org.eclipse.scanning.api.annotation.ui.FieldValue;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ScanningPerspective;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A view which sends IROI events, implements IROI.class in getAdapter(...) and 
 * responds to model selections which contain rois.
 * 
 * @author Matthew Gerring
 *
 */
public class VisualiseView extends ViewPart implements IAdaptable, ISelectionListener {
	
	public static final String ID = "org.eclipse.scanning.device.ui.vis.visualiseView";
	
	private static final Logger logger = LoggerFactory.getLogger(VisualiseView.class);
			
	// Delegated control
	private   PlottingController      controller;

	// The plot
	private   IPlottingSystem<Object> system;

	public VisualiseView() {
		
		try {
			IPlottingService service = ServiceHolder.getPlottingService();
			system = service.createPlottingSystem();
			system.getPlotActionSystem().setShowCustomPlotActions(false); // Disable the custom plot actions.
			
 
		} catch (Exception ne) {
			logger.error("Unable to make plotting system", ne);
			system = null; // It creates the view but there will be no plotting system 
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		
        controller = new PlottingController(system, getViewSite());
        		
		system.createPlotPart(parent, getPartName(), getViewSite().getActionBars(), PlotType.IMAGE, this);  
		system.getSelectedXAxis().setTitle("stage_x");
		system.getSelectedYAxis().setTitle("stage_y");
		
		// Connect to existing regions, although they might not be desirable ones
		controller.connect();
        
        getSite().setSelectionProvider(controller);
        
        getSite().getPage().addSelectionListener(this);
        
		ScanningPerspective.createKeyPlayers();

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection==null) return;
		if (!(selection instanceof StructuredSelection)) return;
		final Object object = ((StructuredSelection)selection).getFirstElement();
		if (object==null) return;
		if (object instanceof FieldValue) {
			processModel(((FieldValue)object).getModel());
		} else if (object instanceof IModelProvider<?>) {
			processModel(((IModelProvider)object).getModel());
		} else if (object instanceof ScanRegion) {
			if (controller!=null) controller.refresh(); // Axes might have changed
		}
	}
	
	private void processModel(Object model) {
		try {
			if (controller!=null) controller.setModel(model);
		} catch (Exception ignored) {
			logger.trace("Unable to deal with model "+model, ignored);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter){
		if (controller!=null) {
			if (controller.getAdapter(adapter)!=null) return controller.getAdapter(adapter);
		}
		if (IPlottingSystem.class == adapter) return (T)system;
		if (IRegionSystem.class == adapter)   return (T)system;
		if (IToolPageSystem.class == adapter) return system.getAdapter(adapter);
		return super.getAdapter(adapter);
	}

	@Override
	public void setFocus() {
		if (system!=null) system.setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		if (controller!=null) controller.dispose();
 		super.dispose();
	}

}
