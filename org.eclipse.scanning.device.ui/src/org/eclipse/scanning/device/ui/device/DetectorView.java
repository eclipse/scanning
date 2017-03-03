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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.device.IActivatable;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.EventConnectionView;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.points.ValidateResults;
import org.eclipse.scanning.device.ui.points.ValidateResultsView;
import org.eclipse.scanning.device.ui.util.PageUtil;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.scanning.event.ui.view.DelegatingSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows a list of available detectors to the user. 
 * They may click on one and configure it.
 * 
 * @author Matthew Gerring
 *
 */
public class DetectorView extends EventConnectionView {

	private static final Logger logger = LoggerFactory.getLogger(DetectorView.class);
	public  static final String ID     = "org.eclipse.scanning.device.ui.detectorView";
	
	// UI
	private TableViewer       viewer;
	private Image             ticked, unticked, defaultIcon;
	private Map<String,Image> iconMap;
	private DelegatingSelectionProvider       selectionProvider;
	
	// Services
	private IRunnableDeviceService dservice;
	
	public DetectorView() {
		
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_HARDWARE, true);
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_MALCOLM, true);
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_PROCESSING, true);
		try {
		    this.defaultIcon = Activator.getImageDescriptor("icons/camera-lens.png").createImage();
		    this.ticked      = Activator.getImageDescriptor("icons/ticked.png").createImage();
		    this.unticked    = Activator.getImageDescriptor("icons/unticked.gif").createImage();
		} catch (Throwable ne) {
			ne.printStackTrace(); // Should not happen
		}
		this.iconMap     = new HashMap<>(7);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class clazz) {
		if (clazz == DeviceInformation.class || clazz == DeviceInformation[].class) {
			DetectorContentProvider prov = (DetectorContentProvider)viewer.getContentProvider();
			return prov.getInfo();
		}
        return super.getAdapter(clazz);
	}
            
	@Override
	public void createPartControl(Composite parent) {
		
		viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(false);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		createColumns(viewer, "", "Name");
		

		try {
			this.dservice = ServiceHolder.getEventService().createRemoteService(getUri(), IRunnableDeviceService.class);
			viewer.setContentProvider(new DetectorContentProvider(dservice));
		} catch (Exception e) {
			logger.error("Cannot create content provider", e);
		}
		viewer.setInput(new Object());
		
		selectionProvider = new DelegatingSelectionProvider(viewer);
		getSite().setSelectionProvider(selectionProvider);
		
		createActions();
		initializeToolBar();
		initializeMenu();

	}
	
	private void createColumns(TableViewer tableViewer, String icon, String name) {
		
		TableViewerColumn tickedColumn = new TableViewerColumn(tableViewer, SWT.CENTER);
		tickedColumn.getColumn().setWidth(24);
		tickedColumn.getColumn().setMoveable(false);
		tickedColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				
				if (!(element instanceof DeviceInformation)) return null;
				DeviceInformation<?> info = (DeviceInformation<?>)element;
				
				return info.isActivated() ? ticked : unticked;
			}
			@Override
			public String getText(Object element) {
				return null;
			}
		});
		
        MouseAdapter mouseClick = new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
			    Point pt = new Point(e.x, e.y);
				TableItem item = viewer.getTable().getItem(pt);
				if (item == null) return;
				Rectangle rect = item.getBounds(0);
				if (rect.contains(pt)) {
					final DeviceInformation<?> info = (DeviceInformation<?>)item.getData();
					try {
						IActivatable device = (IActivatable)dservice.getRunnableDevice(info.getName());
						device.setActivated(!info.isActivated());
						info.setActivated(!info.isActivated());
						getViewSite().getActionBars().getStatusLineManager().setErrorMessage(null);
					} catch (ScanningException e1) {
						logger.error("Unable to set activated state!", e1);
						getViewSite().getActionBars().getStatusLineManager().setErrorMessage("Server is not available: "+e1.getLocalizedMessage());
					}
					tableViewer.refresh(info);
				}
			}
        };
		tableViewer.getTable().addMouseListener(mouseClick);

		
		TableViewerColumn iconColumn = new TableViewerColumn(tableViewer, SWT.CENTER);
		iconColumn.getColumn().setWidth(24);
		iconColumn.getColumn().setMoveable(false);
		iconColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (!(element instanceof DeviceInformation)) return null;
				DeviceInformation<?> info = (DeviceInformation<?>)element;
				if (info.getIcon()==null) return defaultIcon;
				try {
					return getIcon(info.getIcon());
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			@Override
			public String getText(Object element) {
				return null;
			}
		});

		TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.LEFT);
		nameColumn.getColumn().setWidth(300);
		nameColumn.getColumn().setMoveable(false);
		nameColumn.getColumn().setText(name);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (!(element instanceof DeviceInformation)) return null;
				DeviceInformation<?> info = (DeviceInformation<?>)element;
				String label = info.getLabel();
				if (label==null) label = info.getName();
				if (label==null) label = info.getId();
				if (label==null) label = "Unamed Device "+info;
 				return label;
			}
		});
		
	}

	protected Image getIcon(String fullPath) throws IOException {
		
		if (fullPath==null)      return defaultIcon;
		if ("".equals(fullPath)) return defaultIcon;
		
		try {
			if (iconMap.containsKey(fullPath)) return iconMap.get(fullPath);
			final String[] sa = fullPath.split("/");
			final Bundle bundle = Platform.getBundle(sa[0]);
			if (bundle==null) return defaultIcon;
			if (bundle!=null) {
				Image image = new Image(null, bundle.getResource(sa[1]+"/"+sa[2]).openStream());
				iconMap.put(fullPath, image);
			}
			return iconMap.get(fullPath);
		} catch (Exception ne) {
			logger.debug("Cannot get icon for "+fullPath, ne);
			return defaultIcon;
		}
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}

	public static String createId(final String uri, final String requestName, final String responseName) {
		
		final StringBuilder buf = new StringBuilder();
		buf.append(ID);
		buf.append(":");
		buf.append(createSecondaryId(uri, requestName, responseName));
		return buf.toString();
	}
	/**
	 * Create the actions.
	 */
	private void createActions() {

		IMenuManager popup = new MenuManager();
		List<IContributionManager> mans = Arrays.asList(getViewSite().getActionBars().getToolBarManager(), getViewSite().getActionBars().getMenuManager(), popup);

		IAction showHardware   = createPreferenceAction("Show Devices",   DevicePreferenceConstants.SHOW_HARDWARE,   "icons/camera-lens.png");
		IAction showMalcolm   = createPreferenceAction("Show Malcolm Devices",   DevicePreferenceConstants.SHOW_MALCOLM,   "icons/alarm-clock-select.png");
		IAction showProcessing = createPreferenceAction("Show Processing", DevicePreferenceConstants.SHOW_PROCESSING, "icons/processing.png");
		ViewUtil.addGroups("visibility", mans, showHardware, showMalcolm, showProcessing);
		
		IAction refresh = new Action("Refresh", Activator.getImageDescriptor("icons/recycle.png")) {
			public void run() {
				refresh();
			}
		};
		
		ViewUtil.addGroups("refresh", mans, refresh);
		
		IAction configure = new Action("Configure", Activator.getImageDescriptor("icons/configure.png")) {
			public void run() {
				configure();
			}
		};
		ViewUtil.addGroups("camera", mans, configure);

	}
	
	private IAction createPreferenceAction(String label, String preference, String icon) {
		IAction ret = new Action(label, IAction.AS_CHECK_BOX) {
			public void run() {
				Activator.getDefault().getPreferenceStore().setValue(preference, isChecked());
				viewer.refresh();
			}
		};
		ret.setImageDescriptor(Activator.getImageDescriptor(icon));
		ret.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(preference));	
		return ret;
	}


	protected void refresh() {
		
		DeviceInformation<?> info = getSelection();

		boolean ok = MessageDialog.openQuestion(getViewSite().getShell(), "Confirm Refresh", 
				                "This action will go to the devices and re-read their models.\n"+
				                "It will mean that if you have made local edits, they could be lost.\n\n"+
				                "Are you sure you want continue?\n\n"+
				                "(If not the 'Configure' action can be used to send your local edits to a device.)");
		if (!ok) return;
		
		if (info!=null) {
			try {
			    Collection<DeviceInformation<?>> devices = dservice.getDeviceInformationIncludingNonAlive();
				viewer.setInput(new Object());
			    for (DeviceInformation<?> di : devices) {
					if (di.getName()!=null && info.getName()!=null && di.getName().equals(info.getName())) {
						viewer.setSelection(new StructuredSelection(di));
						break;
					}
				}
			} catch (ScanningException se) {
				logger.error("Problem getting device information", se);
			}
		}
	}


	protected void configure() {
		
		DeviceInformation<?> info = getSelection();
		if (info==null) return; // Nothing to configure
		
		boolean ok = MessageDialog.openQuestion(getViewSite().getShell(), "Confirm Configure", "Are you sure you want to configure '"+info.getName()+"' now?\n\n"+
		                                             "If the device is active or being used this will change its behaviour.");
		if (!ok) return;
		
		try {
			IRunnableDevice<Object> device = dservice.getRunnableDevice(info.getName());
			Object model = info.getModel();

			// Pass null to 'clear' the validation results view
			selectionProvider.fireSelection(new StructuredSelection(new ValidateResults(info.getName(), null)));
			
			Object validateReturn = device.validateWithReturn(model);

			ValidateResults validateResults = new ValidateResults(info.getName(), validateReturn);
				
			showValidationResultsView(validateResults);
				
			selectionProvider.fireSelection(new StructuredSelection(validateResults));
						
			device.configure(model);
			
		} catch (ScanningException|ValidationException ne) {
			ErrorDialog.openError(getViewSite().getShell(), "Configure Failed", "The configure of '"+info.getName()+"' failed", 
                                            new Status(IStatus.ERROR, "org.eclipse.scanning.device.ui", ne.getMessage(), ne));
			logger.error("Cannot configure '"+info.getName()+"'", ne);
		}
	}
	
	private void showValidationResultsView(ValidateResults validateResults) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			try {
				if (PageUtil.getPage().findView(ValidateResultsView.ID) == null) {
					IViewPart viewPart = PageUtil.getPage().showView(ValidateResultsView.ID);
					if (viewPart instanceof ValidateResultsView) {
						ValidateResultsView validateResultsView = (ValidateResultsView)viewPart;
						validateResultsView.update(validateResults);
					}
				} else {
					PageUtil.getPage().showView(ValidateResultsView.ID);
				}
			} catch (PartInitException e) {
				logger.warn("Unable to show validate results view " + e);
			}
		});
	}

	private DeviceInformation<?> getSelection() {
		if (viewer.getSelection() == null || viewer.getSelection().isEmpty()) return null;
		return (DeviceInformation<?>)((IStructuredSelection)viewer.getSelection()).getFirstElement();
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager toolbarManager = getViewSite().getActionBars()
				.getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		@SuppressWarnings("unused")
		IMenuManager menuManager = getViewSite().getActionBars()
				.getMenuManager();
	}

	
	public void dispose() {
		super.dispose();
		if (ticked!=null)   ticked.dispose();
		if (unticked!=null) unticked.dispose();
		if (defaultIcon!=null) defaultIcon.dispose();
		for (String path : iconMap.keySet()) iconMap.get(path).dispose();
		iconMap.clear();
	}
}
