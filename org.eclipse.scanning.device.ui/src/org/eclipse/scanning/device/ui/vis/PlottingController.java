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

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.region.IROIListener;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegionListener;
import org.eclipse.dawnsci.plotting.api.region.IRegionSystem;
import org.eclipse.dawnsci.plotting.api.region.ROIEvent;
import org.eclipse.dawnsci.plotting.api.region.RegionEvent;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace.PointStyle;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.Random;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.points.models.IBoundingLineModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.scan.AxisConfiguration;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.api.ui.auto.IModelDialog;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.PageUtil;
import org.eclipse.scanning.device.ui.util.ScanRegions;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This controller can be used by any plotting
 * system to make it process maps. When regions are
 * selected or moved, the scan path is replotted using a job.
 * {@link org.dawnsci.plotting.tools.profile.ProfileTool.ProfileJob}
 * 
 * @author Matthew Gerring
 * @author James Mudd
 * @author Colin Palmer
 *
 */
public class PlottingController implements ISelectionProvider, IAdaptable {
	
	private static final Logger logger = LoggerFactory.getLogger(PlottingController.class);

	
	private static final String MAPPING_PATH_NAME = "Mapping Scan Path";
	
	// UI
	private   IPlottingSystem<?> system;
	private   final Color        scanPathColour;
	private   final IViewSite    site;
	
	// Data
	private   volatile Object    model; // We make accessing the model single threaded because worker threads are involved.
	
	// Events
	private   IRegionListener    regionListener;
	private   IROIListener       roiListener;
	
	// Workers
	private PathInfoCalculatorJob job;


	public PlottingController(IPlottingSystem<?> system, IViewSite site) {
		
		this.system         = system;
		this.site           = site;
		this.listeners      = new HashSet<ISelectionChangedListener>(11);
		this.job            = new PathInfoCalculatorJob(this);
		this.scanPathColour = new Color(null, 160, 32, 240); // purple

		roiListener    = new IROIListener.Stub() {
			@Override
			public void roiDragged(ROIEvent evt) {
				ILineTrace pathTrace = (ILineTrace)system.getTrace(MAPPING_PATH_NAME);
				if (pathTrace!=null) pathTrace.setVisible(false);
				fireRegion((IRegion)evt.getSource(), evt.getROI(), false);
			}
			@Override
			public void update(ROIEvent evt) {
				fireRegion((IRegion)evt.getSource(), evt.getROI(), true);
			}
		};
		regionListener = new IRegionListener.Stub() {

			@Override
			public void regionAdded(RegionEvent evt) {
				evt.getRegion().addROIListener(roiListener);
				fireRegion(evt.getRegion(), evt.getRegion().getROI(), true);
			}
			@Override
			public void regionRemoved(RegionEvent evt) {
				evt.getRegion().removeROIListener(roiListener);
				fireRegion((IRegion)evt.getSource(), evt.getRegion().getROI(), true);
			}
			@Override
			public void regionsRemoved(RegionEvent evt) {
				for (IRegion region : evt.getRegions()) {
					region.removeROIListener(roiListener);
				}
				fireRegion((IRegion)evt.getSource(), evt.getRegion().getROI(), true);
			}
		};
		
		createActions(site);
	}
	

	private AxisConfiguration axisConfig;
	/**
	 * Actions in addition to the standard plotting actions which
	 * should be shown on the plot.
	 * 
	 * @param site
	 */
	private void createActions(IViewSite site) {
		
		IActionBars bars = site.getActionBars();
		final List<IContributionManager> mans = Arrays.asList(bars.getToolBarManager(), bars.getMenuManager());
	
	    final IAction configureAxes = new Action("Configure...", Activator.getImageDescriptor("icons/home-for-sale-sign-blue.png")) {

			public void run() {
	    		
	    		try {
		    		IModelDialog<AxisConfiguration>  dialog = ServiceHolder.getInterfaceService().createModelDialog(site.getShell());
		    		dialog.setPreamble("Please define the axes and their ranges we will map within.");
		    		dialog.create();
		    		dialog.setSize(550,450); // As needed
		    		dialog.setText("Scan Area");
		    		dialog.setModel(getAxisConfiguration());
		    		int ok = dialog.open();
		    		if (ok==IModelDialog.OK) {
		    			AxisConfiguration conf = dialog.getModel();
		    			setAxisConfiguration(conf);
		    		}
	    		} catch (Exception ne) {
	    			ErrorDialog.openError(site.getShell(), "Error Showing Plot Configuration", "Please contact your support representative.", new Status(IStatus.ERROR, "org.eclipse.scanning.device.ui", ne.getMessage(), ne));
	    			logger.error("Cannot configure plot!", ne);
	    		}
	    	}
		};
		ViewUtil.addGroups("configure", mans, configureAxes);
	}

	protected void setAxisConfiguration(AxisConfiguration conf) {
		
		this.axisConfig = conf;
		if (conf==null) return;
		
		createPlot(conf);
		system.getSelectedXAxis().setTitle(conf.getFastAxisName());
		system.getSelectedYAxis().setTitle(conf.getSlowAxisName());
		
		// If we have to reaxis the regions, try to
		if (conf.isApplyRegions()) {
			List<ScanRegion<IROI>> regions = ScanRegions.getScanRegions(system);
			List<String> axes = Arrays.asList(conf.getFastAxisName(), conf.getSlowAxisName());
			if (regions!=null) for (ScanRegion<IROI> region : regions) {
				region.setScannables(axes);
			}
		}
		
		if (conf.isApplyModels()) {
			// TODO Use IScanBuilderService
			IViewReference[] refs = PageUtil.getPage().getViewReferences();
			for (IViewReference iViewReference : refs) {
				IViewPart part = iViewReference.getView(false);
				if (part==null) continue;
				IPointGenerator<?>[] pa = part.getAdapter(IPointGenerator[].class);
                if (pa !=null) {
                	for (int i = 0; i < pa.length; i++) {
                    	final Object model = pa[i].getModel();
                    	if (model instanceof IBoundingBoxModel) {
                    		IBoundingBoxModel bmodel = (IBoundingBoxModel)model;
                    		bmodel.setFastAxisName(conf.getFastAxisName());
                    		bmodel.setSlowAxisName(conf.getSlowAxisName());
                    	}
					}
                }
			}
		}

		send(conf);
		
		IStashing stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.axis.configuation.json");
		try {
			stash.stash(conf);          
		} catch (Exception ne) {
		    logger.error("Cannot save to "+stash.getFile(), ne);
		}
        
	}

	private void send(AxisConfiguration conf) {
		try {
			IEventService eservice = ServiceHolder.getEventService();
            IPublisher<AxisConfiguration> publisher = eservice.createPublisher(new URI(CommandConstants.getScanningBrokerUri()), EventConstants.AXIS_CONFIGURATION_TOPIC);
            publisher.broadcast(axisConfig);
            publisher.disconnect();
            
		} catch (Exception ne) {
		    logger.error("Cannot publish "+axisConfig, ne);
		}
	}

	protected AxisConfiguration getAxisConfiguration() {
		
		if (axisConfig==null) axisConfig = new AxisConfiguration();		
		axisConfig.setFastAxisName(system.getSelectedXAxis().getTitle());
		
		final IImageTrace image = (IImageTrace)system.getTrace("image");
		if (image!=null) {
			double[] da = image.getGlobalRange();
			axisConfig.setFastAxisStart(da[0]);
			axisConfig.setFastAxisEnd(da[1]);
			
			axisConfig.setSlowAxisStart(da[2]);
			axisConfig.setSlowAxisEnd(da[3]);
		} else {
		
			axisConfig.setFastAxisStart(system.getSelectedXAxis().getLower());
			axisConfig.setFastAxisEnd(system.getSelectedXAxis().getUpper());
			
			axisConfig.setSlowAxisStart(system.getSelectedYAxis().getLower());
			axisConfig.setSlowAxisEnd(system.getSelectedYAxis().getUpper());
		}
		
		axisConfig.setFastAxisName(system.getSelectedXAxis().getTitle());
		axisConfig.setSlowAxisName(system.getSelectedYAxis().getTitle());

		return axisConfig;
	}
	
	private void createPlot(AxisConfiguration conf) {
		
		if (conf==null) return;
		IDataset image = null;
		if (conf.getMicroscopeImage()!=null && !"".equals(conf.getMicroscopeImage())) {
			try {
			    image = ServiceHolder.getLoaderService().getDataset(conf.getMicroscopeImage(), null);
			} catch (Exception ne) {
				final File file = new File(conf.getMicroscopeImage());
				ErrorDialog.openError(site.getShell(), "Problem Reading '"+file.getName()+"'", 
						"There was a problem reading '"+file.getName()+"'\n\n"
								+ "Please contact our support representative.", new Status(IStatus.ERROR, Activator.PLUGIN_ID, ne.getMessage(), ne));
				logger.error("Cannot read file!", ne);
			}
		}
		if (image==null && conf.isRandomNoise()) {
			image = Random.rand(4096, 3012);
		}
		if (image==null) {
			system.getSelectedXAxis().setRange(conf.getFastAxisStart(), conf.getFastAxisEnd());
			system.getSelectedYAxis().setRange(conf.getSlowAxisStart(), conf.getSlowAxisEnd());
			return;
		}
		createTrace(conf, image);
	}
	
	private void createTrace(AxisConfiguration conf, IDataset image) {
		
		// Images are reversed
		int fsize = image.getShape()[1];
		int ssize = image.getShape()[0];
		
		IDataset x = DatasetFactory.createRange(conf.getFastAxisStart(), conf.getFastAxisEnd(), (conf.getFastAxisEnd()-conf.getFastAxisStart())/fsize, Dataset.FLOAT);
		x.setName(conf.getFastAxisName());
		IDataset y = DatasetFactory.createRange(conf.getSlowAxisStart(), conf.getSlowAxisEnd(), (conf.getSlowAxisEnd()-conf.getSlowAxisStart())/ssize, Dataset.FLOAT);
		y.setName(conf.getSlowAxisName());
		
		IImageTrace it = system.getTrace("image")!=null 
				       ? (IImageTrace)system.getTrace("image")
				       : system.createImageTrace("image");
		it.setData(image, Arrays.asList(new IDataset[]{x,y}), false);
		
		double[] globalRange = new double[4];
		globalRange[0] = conf.getFastAxisStart();
		globalRange[1] = conf.getFastAxisEnd();
		globalRange[2] = conf.getSlowAxisStart();
		globalRange[3] = conf.getSlowAxisEnd();
		it.setGlobalRange(globalRange);
		
		system.addTrace(it);
		job.schedule();
	}


	void plot(PathInfo info) {
		
		boolean newTrace = false;
		//Remove the previous trace
		ILineTrace pathTrace = (ILineTrace)system.getTrace(MAPPING_PATH_NAME);
		
		// If there are no scan regions at all, no trace to draw
		// If the model is not one we draw scan paths for, no trace to draw.
		if (!isScanPathModel() || ScanRegions.getScanRegions(system)==null) {
			if (pathTrace!=null) pathTrace.setVisible(false);
			return;
		}	
		
		if (pathTrace == null) {
			pathTrace = system.createLineTrace(MAPPING_PATH_NAME);
			pathTrace.setTraceColor(scanPathColour);
			pathTrace.setPointStyle(PointStyle.SQUARE);
			newTrace = true;
		}

		// Check if the scan region is currently plotted - if not, we don't want to plot the path either
		// (This fixes a synchronisation bug where the path is added while the scan region drawing event is still
		// active, cancelling the event and making it impossible to draw regions)
		if (info != null) {

			// Get the point coordinates from the last path info and add them to the trace
			pathTrace.setData(info.getX(), info.getY());
			if (!newTrace) system.removeTrace(pathTrace); // We always remove/add it because that puts it on top.
			system.addTrace(pathTrace);                   // We always remove/add it because that puts it on top.
			pathTrace.setVisible(true);
			system.setPlotType(PlotType.IMAGE);
			system.setShowLegend(false);
		}
	}


	private void fireRegion(IRegion region, IROI roi, boolean drawPath) {

		if (region==null) return;
		
		if (!(region.getUserObject() instanceof ScanRegion)) return; // Must be another region.
		roi.setName(region.getName());
		setSelection(new StructuredSelection(roi));
		
		List<ScanRegion<IROI>> sregions = ScanRegions.getScanRegions(system);
		if (drawPath) {
			if (sregions==null) {
				setPathVisible(false);
			} else {
				job.schedule(model, sregions);
			}
		}
	}

	
	private Set<ISelectionChangedListener> listeners;
	private ISelection currentSelection;
	
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return currentSelection;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Method calls listener in background thread mto make frequent updates possible.
	 */
	@Override
	public void setSelection(ISelection selection) {
		
		if (listeners.isEmpty()) return;
		currentSelection = selection;
		SelectionChangedEvent e = new SelectionChangedEvent(this, currentSelection);
		ISelectionChangedListener[] sl = listeners.toArray(new ISelectionChangedListener[listeners.size()]);
		for (ISelectionChangedListener s : sl) s.selectionChanged(e);
	}

	public void clear() {
		if (listeners!=null) listeners.clear();
	}

	public void refresh() {
		job.schedule(model, ScanRegions.getScanRegions(system));
	}
	
	public void setModel(Object model) throws Exception {
		
		this.model = model;
		if (isScanPathModel()) {			
			job.schedule(model, ScanRegions.getScanRegions(system));
		} else {
			setPathVisible(false);
		}
	}
	
	private boolean isScanPathModel() {
		// TODO We may want to change the definition of this.
		return model instanceof IBoundingBoxModel || model instanceof IBoundingLineModel;
	}

	void setPathVisible(boolean vis) {
		ILineTrace pathTrace = (ILineTrace)system.getTrace(MAPPING_PATH_NAME);
        if (pathTrace!=null) pathTrace.setVisible(vis);
	}

	public void dispose() {
		clear();
		if (system!=null) {
		    system.removeRegionListener(regionListener);
		    for (IRegion region : system.getRegions()) {
		    	if (region.getUserObject() instanceof ScanRegion) region.removeROIListener(roiListener);
			}
			system.dispose();
		}
		scanPathColour.dispose();
	}

	public void connect() {
		
	    connectRegions();
	    connectAxes();
	}

	private void connectAxes() {
		
		IStashing stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.axis.configuation.json");
		if (stash.isStashed()) {
			try {
				this.axisConfig = stash.unstash(AxisConfiguration.class);
			} catch (Exception e) {
				logger.error("Cannot unstash axis configuration!", e);
			}
			setAxisConfiguration(axisConfig);
		}
		
	}

	private void connectRegions() {
		for (IRegion region : system.getRegions()) {
	    	if (region.getUserObject() instanceof ScanRegion) region.addROIListener(roiListener);
		}
        system.addRegionListener(regionListener);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (PlottingController.class == adapter) return (T)this;
		if (IPlottingSystem.class == adapter) return (T)system;
		if (IRegionSystem.class == adapter)   return (T)system;
		if (IToolPageSystem.class == adapter) return system.getAdapter(adapter);
		return null;
	}

	public IImageTrace getImageTrace() {
		if (system.getTraces()==null || system.getTraces().isEmpty()) return null;
		try {
			return (IImageTrace)system.getTraces(IImageTrace.class).iterator().next();
		} catch (Exception ne) {
			return null;
		}
	}

}
