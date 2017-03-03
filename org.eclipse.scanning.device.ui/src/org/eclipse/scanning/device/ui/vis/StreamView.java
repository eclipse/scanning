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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dawnsci.plotting.api.IPlottingService;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace.DownsampleType;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.richbeans.widgets.menu.CheckableActionGroup;
import org.eclipse.richbeans.widgets.menu.MenuAction;
import org.eclipse.scanning.api.streams.IStreamConnection;
import org.eclipse.scanning.api.streams.StreamConnectionException;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A view which displays a view of a stream, either MJpeg or Epics array.
 * 
 * TODO: Matt Taylor this needs to be your absolute best commented and organised code
 * please, because it will go in the open source scanning project!
 * 
 * @author Matthew Taylor
 * @author Matthew Gerring
 *
 */
public class StreamView extends ViewPart implements IAdaptable {
	
	public static final String ID = "org.eclipse.scanning.device.ui.vis.StreamView";
	
	private static final Logger logger = LoggerFactory.getLogger(StreamView.class);
		
	// UI
	protected IPlottingSystem<Composite> system;
	
	// Connectors
	private IStreamConnection<ILazyDataset>       selected;
	private List<IStreamConnection<ILazyDataset>> connectors;
	
	public StreamView() {
		try {
			this.connectors = new ArrayList<>();
			IPlottingService plottingService = ServiceHolder.getPlottingService();
			system = plottingService.createPlottingSystem();
			
		} catch (Exception ne) {
			logger.error("Unable to make plotting system", ne);
			system = null; // It creates the view but there will be no plotting system 
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		createConnectionActions();

		system.createPlotPart(parent, getPartName(), getViewSite().getActionBars(), PlotType.IMAGE, this);
		connect(findLastConnection()); // TODO Should it be the first one in the list.
	}
	
	private IStreamConnection<ILazyDataset> findLastConnection() {
		
		String id = Activator.getDefault().getPreferenceStore().getString(DevicePreferenceConstants.STREAM_ID);
		if (id==null) return connectors.get(0);
		
		for (IStreamConnection<ILazyDataset> connector : connectors) {
			if (connector.getId().equals(id)) return connector;
		}
		return connectors.get(0);
	}

	private void createConnectionActions() {
		
		connectors.clear();
		
		String lastId = Activator.getDefault().getPreferenceStore().getString(DevicePreferenceConstants.STREAM_ID);

		
		final IConfigurationElement[] eles = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.scanning.api.stream");
		for (IConfigurationElement e : eles) {

			CheckableActionGroup group = new CheckableActionGroup();
			try {
				final IStreamConnection<ILazyDataset> connection = (IStreamConnection<ILazyDataset>)e.createExecutableExtension("stream");
				connectors.add(connection);
				connection.setId(e.getAttribute("id"));
				connection.setLabel(e.getAttribute("label"));
				
				final String iconPath = e.getAttribute("icon");
				ImageDescriptor icon=null;
		    	if (iconPath!=null) {
			    	final String   id    = e.getContributor().getName();
			    	final Bundle   bundle= Platform.getBundle(id);
			    	final URL      entry = bundle.getEntry(iconPath);
			    	icon = ImageDescriptor.createFromURL(entry);
		    	}

		    	final MenuAction menu = new MenuAction(connection.getLabel());
		    	final IAction connect = new Action(connection.getLabel(), IAction.AS_CHECK_BOX) {
		    		public void run() {
		    			connect(connection);
		    		}
		    	};
		    	connect.setImageDescriptor(icon);
		    	connect.setChecked(lastId!=null && lastId.equals(connection.getId()));
		    	group.add(connect);
		    	menu.add(connect);
		    	menu.setSelectedAction(connect);
		    	
		    	final IAction configure = new Action("Configure...") {
		    		public void run() {
		    			configure(connection);
		    		}
		    	};
		    	menu.add(configure);

		    	getViewSite().getActionBars().getToolBarManager().add(menu);
			
			} catch (Exception ne) {
				logger.error("Problem creating stream connection for "+e, ne);
			}
			
			getViewSite().getActionBars().getToolBarManager().add(new Separator());
		}
		
	}

	private void configure(IStreamConnection<ILazyDataset> connection) {
		try {
			connection.configure();
			if (selected == connection) connect(connection);
		} catch (StreamConnectionException sce) {			
			logger.error("Internal error, connection cannot be configured!", sce);
		}
	}

	private void connect(IStreamConnection<ILazyDataset> connection) {
		
		try {
			if (selected!=null) {
				try {
					selected.disconnect();
				} catch (StreamConnectionException sce) {				
					logger.error("Internal error, connection cannot be disconnected!", sce);
				}
			}
			ILazyDataset image = connection.connect();
			selected = connection;
			Activator.getDefault().getPreferenceStore().setValue(DevicePreferenceConstants.STREAM_ID, selected.getId());
			
			IImageTrace trace = system.createImageTrace("Image"); // TODO
			trace.setData(image, null, false);
			system.addTrace(trace);
			
			// Settings to increase the render speed
			trace.setDownsampleType(DownsampleType.POINT);
			trace.setRescaleHistogram(false);
			
			// Fix the aspect ratio as is typically required for visible cameras
			system.setKeepAspect(true);
			
			// Disable auto rescale as the live stream is constantly refreshing
			system.setRescale(false);
			
		} catch (StreamConnectionException sce) {			
			logger.error("Internal error, connection cannot be reached!", sce);
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public  <T> T getAdapter(Class<T> adapter) {
		if (IPlottingSystem.class == adapter) return (T)system;
		if (IToolPageSystem.class == adapter) return system.getAdapter(adapter);
		return super.getAdapter(adapter);
	}

	@Override
	public void setFocus() {
		if (system!=null) system.setFocus();
	}

	@Override
	public void dispose() {
		if (system!=null) system.dispose();
		for (IStreamConnection<ILazyDataset> iStreamConnection : connectors) {
			try {
				iStreamConnection.disconnect();
			} catch (StreamConnectionException e) {
				logger.error("Cannot disconnect "+iStreamConnection.getLabel(), e);
			}
		}
		super.dispose();
	}

}
