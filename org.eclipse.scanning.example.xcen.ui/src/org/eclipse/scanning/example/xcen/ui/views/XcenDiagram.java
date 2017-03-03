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
package org.eclipse.scanning.example.xcen.ui.views;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.plotting.api.VanillaPlottingSystemView;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.scanning.example.xcen.ui.XcenActivator;
import org.eclipse.scanning.example.xcen.ui.XcenServices;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XcenDiagram extends VanillaPlottingSystemView {
	
	public static final String ID = "org.eclipse.scanning.example.xcen.ui.views.XcenDiagram";
	
	private static final Logger logger = LoggerFactory.getLogger(XcenDiagram.class);
	
	private ILoaderService service;
	
	public XcenDiagram() {
		super();
		service = XcenServices.getCurrent().getLoaderService();
	}

	@Override
	public void createPartControl(Composite parent) {
		
        if (system==null) {
        	parent.setLayout(new GridLayout(1, false));
        	final Label msg = new Label(parent, SWT.WRAP);
        	msg.setText("No plotting system found available.\nThere are probably no bundles providing plotting in the run configuration.\nThese may be obtained from dawn p2, for instance:\nhttp://opengda.org/DawnDiamond/2.0/updates/release/\n\nA static image is shown for our enjoyment.");
        	final Label img = new Label(parent, SWT.NONE);
        	img.setImage(XcenActivator.getImageDescriptor("icons/xstall.png").createImage());
        	return;
        }
        super.createPartControl(parent);
        
     
        // TODO Hard coded an x-stall, should come from current data acquisition.
        try {
			final File loc = new File(BundleUtils.getBundleLocation(XcenActivator.PLUGIN_ID), "icons/xstall.png");
			final IDataset image = service.getDataset(loc.getAbsolutePath(), new IMonitor.Stub());

			system.createPlot2D(image, null, new NullProgressMonitor());
			
        } catch (Exception ne) {
        	logger.error("Cannot load dataset!", ne);
        }
	}
}
