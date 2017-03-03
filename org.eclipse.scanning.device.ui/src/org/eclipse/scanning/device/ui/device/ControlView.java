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
package org.eclipse.scanning.device.ui.device;


import java.net.URI;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.device.scannable.ControlTreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlView extends ViewPart {
	
	private static final Logger logger = LoggerFactory.getLogger(ControlView.class);

	public static final String ID = "org.eclipse.scanning.device.ui.device.ControlView"; //$NON-NLS-1$
	
	// UI
	private ControlTreeViewer viewer;

	// File
	private IStashing stash;

	public ControlView() {
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_CONTROL_TOOLTIPS, true);
		this.stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.device.controls.json");
	}
	
	@Override
    public void saveState(IMemento memento) {
    	super.saveState(memento);
    	try {
    		stash.stash(viewer.getControlTree());
		} catch (Exception e) {
			logger.error("Problem stashing control factory!", e);
		}
    }
	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		try {
			IScannableDeviceService cservice = ServiceHolder.getEventService().createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), IScannableDeviceService.class);

			ControlTree defaultTree = ControlTreeUtils.parseDefaultXML();
			if (defaultTree==null) {
				defaultTree = new ControlTree();
				defaultTree.globalize();
			}
			viewer = new ControlTreeViewer(defaultTree, cservice); // Widget linked to hardware, use ControlViewerMode.INDIRECT_NO_SET_VALUE to edit without setting hardware.
			
			ControlTree stashedTree = stash.unstash(ControlTree.class); // Or null if couldn't
			if (stashedTree!=null) stashedTree.build();
			viewer.createPartControl(parent, stashedTree, getViewSite().getActionBars().getMenuManager(), getViewSite().getActionBars().getToolBarManager());
		
		    getSite().setSelectionProvider(viewer.getSelectionProvider());

		} catch (Exception e) {
			logger.error("Cannot build ControlTreeViewer!", e);
		}

	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		viewer.dispose();
	}
	
}
