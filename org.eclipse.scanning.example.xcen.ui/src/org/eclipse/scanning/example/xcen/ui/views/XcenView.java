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

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.dataset.roi.GridROI;
import org.eclipse.dawnsci.analysis.dataset.roi.json.GridROIBean;
import org.eclipse.dawnsci.analysis.dataset.roi.json.ROIBeanFactory;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.region.RegionUtils;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage.ToolPageRole;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.richbeans.widgets.scalebox.ScaleBox;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.scanning.event.ui.view.StatusQueueView;
import org.eclipse.scanning.example.xcen.beans.XcenBean;
import org.eclipse.scanning.example.xcen.ui.XcenActivator;
import org.eclipse.scanning.example.xcen.ui.XcenServices;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XcenView extends ViewPart {
	
	private IEventService service;

	public XcenView() {
		this.service = XcenServices.getCurrent().getEventService();
	}

	public static final String ID = "org.eclipse.scanning.example.xcen.ui.views.XcenView"; //$NON-NLS-1$
	
	private static final Logger logger = LoggerFactory.getLogger(XcenView.class);
	
	private ISubscriber<IBeanListener<XcenBean>> topicMonitor;

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		
		container.setLayout(new GridLayout(2, false));
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Visit");
		
		final Text visit = new Text(container, SWT.NONE|SWT.BORDER);
		visit.setText("nt5073-40");
		visit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		label = new Label(container, SWT.NONE);
		label.setText("Collection");
		
		final Text collection = new Text(container, SWT.NONE|SWT.BORDER);
		collection.setText("sapA-x56_A");
		collection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		label = new Label(container, SWT.NONE);
		label.setText("Add grid (optional)");
		
		Button grid = new Button(container, SWT.PUSH);
		grid.setImage(XcenActivator.getImageDescriptor("icons/plot-tool-box-grid.png").createImage()); // Small memory leak possible.
		grid.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				createGrid();
			}
		});
		GridData gd = new GridData(SWT.LEFT, SWT.FILL, false, false);
		gd.widthHint = 100;
		grid.setLayoutData(gd);

		label = new Label(container, SWT.NONE);
		label.setText("Execute Centering");
		
		Button go = new Button(container, SWT.PUSH);
		go.setImage(XcenActivator.getImageDescriptor("icons/ruby.png").createImage()); // Small memory leak possible.
		gd = new GridData(SWT.LEFT, SWT.FILL, false, false);
		gd.widthHint = 100;
		go.setLayoutData(gd);
		
		go.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					submitCentering(visit.getText(), collection.getText());
				} catch (Exception e1) {
					logger.error("Submitting centering request failed!", e1);
				}
			}
		});
		
		final Label sep = new Label(container, SWT.SEPARATOR|SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		label = new Label(container, SWT.NONE);
		label.setText("Centering Run");
		
		final Label centeringValue = new Label(container, SWT.NONE);
		centeringValue.setText("");
		centeringValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	
		// TODO Hook up value into box.
		Map<String, ScaleBox> boxes = createBoxes(container, "x", "y", "z");

		createActions();
		initializeToolBar();
		initializeMenu();
		
		createCenteringListener(centeringValue, boxes);
	}
	
	private void createCenteringListener(final Label centeringValue, final Map<String, ScaleBox> boxes) {
		
		// We listen to the topic and when a complete bean comes through, we take this
		// as the current center for the UI
		// Use job because connection might timeout.
		final Job topicJob = new Job("Create topic listener") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					topicMonitor = service.createSubscriber(new URI(Activator.getJmsUri()), "dataacq.xcen.STATUS_TOPIC");
					topicMonitor.addListener(new IBeanListener<XcenBean>() {
						@Override
						public void beanChangePerformed(BeanEvent<XcenBean> evt) {
							final XcenBean bean = evt.getBean();
							if (bean.getStatus()==org.eclipse.scanning.api.event.status.Status.COMPLETE) {
								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										centeringValue.setText(bean.getName());
										boxes.get("x").setValue(bean.getX());
										boxes.get("y").setValue(bean.getY());
										boxes.get("z").setValue(bean.getZ());
									}
								});
							}
						}

						@Override
						public Class<XcenBean> getBeanClass() {
							return XcenBean.class;
						}
					});
			        return Status.OK_STATUS;
			        
				} catch (Exception ne) {
					logger.error("Cannot listen to topic changes because command server is not there", ne);
			        return Status.CANCEL_STATUS;
				}
			}
			
			
		};
		
		topicJob.setPriority(Job.INTERACTIVE);
		topicJob.setSystem(true);
		topicJob.setUser(false);
		topicJob.schedule();
		
	}

	private void createGrid() {
		IPlottingSystem<Composite> system = getPlottingSystem();
		IToolPageSystem tsys   = (IToolPageSystem)system.getAdapter(IToolPageSystem.class);
		try {
			tsys.setToolVisible("org.dawb.workbench.plotting.tools.gridTool", ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
			system.createRegion(RegionUtils.getUniqueName("Xcen Grid",  system), RegionType.GRID);
		} catch (Exception e1) {
			logger.error("Cannot start to create a grid!", e1);
		}
	}

	private IPlottingSystem<Composite> getPlottingSystem() {
		IViewPart       part   = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(XcenDiagram.ID);
		IPlottingSystem<Composite> system = (IPlottingSystem<Composite>)part.getAdapter(IPlottingSystem.class);
		return system;
	}

	private void submitCentering(String visit, String collection) throws Exception {
		
		XcenBean submit = new XcenBean();
		
		final DateFormat format = new SimpleDateFormat("  (hh:mm:ss)");
		submit.setName(collection+format.format(new Date()));
		submit.setBeamline("i04-1"); // TODO Hard coded!
		submit.setVisit(visit);
		submit.setCollection(collection);
		
		IPlottingSystem<Composite> system = getPlottingSystem();
		if (system!=null) {
			final Collection<IRegion> grids = system.getRegions(RegionType.GRID);
			if (grids!=null && grids.size()>0) {
				final GridROI[] rois = new GridROI[grids.size()];
				int i = 0;
				for (IRegion iRegion : grids) {
					rois[i] = (GridROI)iRegion.getROI();
					++i;
				}
				setROIs(submit, rois);
			}
		}
	        
		final ISubmitter<XcenBean> factory = service.createSubmitter(new URI(Activator.getJmsUri()), "dataacq.xcen.SUBMISSION_QUEUE");
		factory.setStatusTopicName("dataacq.xcen.STATUS_TOPIC");
		factory.submit(submit, true);

		showQueue();
	}	
	
	public void setROIs(XcenBean submit, GridROI... g) throws Exception {
		if (g == null) {
			submit.setGrids(null);
			return;
		}
		GridROIBean[] grids = new GridROIBean[g.length];
		for (int i = 0; i < g.length; i++) {
			grids[i] = (GridROIBean)ROIBeanFactory.encapsulate(g[i]);
		}
		submit.setGrids(grids);
	}


	private void showQueue() throws Exception {
		
		IViewReference[] refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		
		boolean foundStatus = false;
		for (IViewReference vr : refs) {
			if (StatusQueueView.ID.equals(vr.getId())) foundStatus = true;
		}
		if (!foundStatus) {
			String secondId = XcenServices.getQueueViewSecondaryId();
			IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(StatusQueueView.ID+":"+secondId, null, IWorkbenchPage.VIEW_VISIBLE);
			if (part !=null && part instanceof StatusQueueView) {
				StatusQueueView view = (StatusQueueView)part;
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().bringToTop(view);
				view.refresh();
			}
		}
	}
	
	private Map<String, ScaleBox> createBoxes(Composite container, String... names) {
		
		final Map<String, ScaleBox> boxes = new HashMap<String, ScaleBox>(3);
		for (String name : names) {
			Label label = new Label(container, SWT.NONE);
			label.setText(name);
			
			final ScaleBox data = new ScaleBox(container, SWT.NONE);
			data.setUnit("µm");
			data.setValue(0d);
			data.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			data.setEnabled(false);
			
			boxes.put(name, data);
		}
		
		return boxes;
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
	}
	
	public void dispose() {
		super.dispose();
		if (topicMonitor!=null) {
			try {
				topicMonitor.disconnect();
			} catch (Exception e) {
				logger.error("Cannot close connection!", e);
			}
		}
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars()
				.getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars()
				.getMenuManager();
	}

	@Override
	public void setFocus() {
		// Set the focus
	}
}
