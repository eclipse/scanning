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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.axis.IAxis;
import org.eclipse.dawnsci.plotting.api.preferences.BasePlottingConstants;
import org.eclipse.dawnsci.plotting.api.region.ColorConstants;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.region.IRegionListener;
import org.eclipse.dawnsci.plotting.api.region.RegionEvent;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.richbeans.widgets.file.FileSelectionDialog;
import org.eclipse.richbeans.widgets.internal.GridUtils;
import org.eclipse.richbeans.widgets.menu.MenuAction;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.PlotUtil;
import org.eclipse.scanning.device.ui.util.ScanRegions;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * A tool for editing scan regions. The view creates/edits regions
 * and then encapsulates then into a ScanRegion object which the axes
 * upon which they act.
 * 
 * It translates the IROI's created in data coordinates to IROI's in
 * axis coordinates and can be used via getAdpter(...) to return a list
 * of these ScanRegions which may be packaged in the CompoundModel which
 * is given to the server.
 * 
 * TODO This view is only linked to one plotting system which is 
 * read when the view is created. Should the view actually be a 
 * static tool which updates the scan regions for any plotting system?
 * Or should there be an action to change plotting systems that
 * the view is linked to?
 * 
 * @author Matthew Gerring
 *
 */
public class ScanRegionView extends ViewPart {
	
	public static final String ID = "org.eclipse.scanning.device.ui.points.scanRegionView";
	
	private static Logger logger = LoggerFactory.getLogger(ScanRegionView.class);

	private static final Collection<RegionType> regionTypes;
	static {
		regionTypes = new ArrayList<RegionType>(6);
		regionTypes.add(RegionType.BOX);
		regionTypes.add(RegionType.POLYGON);
		regionTypes.add(RegionType.RING);
		regionTypes.add(RegionType.SECTOR);
		regionTypes.add(RegionType.ELLIPSE);
	}

	// UI
	private TableViewer        viewer;
	private IPlottingSystem<?> system;

	// Listeners
	private IRegionListener regionListener;
	
	// File
	private IStashing stash;

	private DelegatingSelectionProvider selectionDelegate;
	
	public ScanRegionView() {
		
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.AUTO_SAVE_REGIONS, true);
		Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_SCAN_REGIONS, true);
		this.stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.scan.regions.json");
		
		this.regionListener = new IRegionListener.Stub() {

			protected void update(RegionEvent evt) {
				viewer.refresh();
			}
		};
	}

	@Override
	public void createPartControl(Composite ancestor) {
		
		// TODO Action to choose a different plotting system?
		this.system = PlotUtil.getRegionSystem();

		Composite control = new Composite(ancestor, SWT.NONE);
		control.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(control);
		
		this.viewer = new TableViewer(control, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ScanRegionContentProvider());
		
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		// resize the row height using a MeasureItem listener
		viewer.getTable().addListener(SWT.MeasureItem, new Listener() {
	        public void handleEvent(Event event) {
	            event.height = 24;
	        }
	    });

	    //added 'event.height=rowHeight' here just to check if it will draw as I want
		viewer.getTable().addListener(SWT.EraseItem, new Listener() {
	        public void handleEvent(Event event) {
	            event.height=24;
	        }
	    });		
		
		viewer.getTable().addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F1) {
					// TODO Help!
				}
				if (e.character == SWT.DEL) {
					try {
						Object ob = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
						ScanRegion<IROI> region = (ScanRegion<IROI>)ob;
						IRegion sregion = system.getRegion(region.getName());
						if (sregion!=null) system.removeRegion(sregion);
						viewer.refresh(); // Must do global refresh because might effect units of other parameters.
					} catch (Exception ne) {
						logger.error("Cannot delete item "+(IStructuredSelection)viewer.getSelection(), ne);
					}

				}
			}
		});

		
		createActions();
		this.selectionDelegate = new DelegatingSelectionProvider(viewer);
		try {
 			createColumns(viewer, selectionDelegate);
		} catch (EventException | URISyntaxException e1) {
			logger.error("Serious internal error trying to create table columns!", e1);
		}
		
		getSite().setSelectionProvider(selectionDelegate);
		viewer.setInput(system);
		system.addRegionListener(regionListener);
		
		if (Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.AUTO_SAVE_REGIONS)) {
			List<ScanRegion<IROI>> regions;
			try {
				regions = stash.unstash(List.class);
				createRegions(regions);
				viewer.refresh();
			} catch (Exception e1) {
				logger.error("Cannot unstash regions", e1);
			}
		}
		
		// Called when user clicks on UI
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {		
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				try {
					ScanRegion<IROI> region = (ScanRegion<IROI>)((IStructuredSelection)event.getSelection()).getFirstElement();
					if (region!=null) setSelectedRegion(region);
				} catch (Exception ne) {
					logger.warn("Cannot select scan region", ne); // Not serious.
				}
			}
		});
		
		// Called when user clicks on UI and when axes change.
		selectionDelegate.addSelectionChangedListener(new ISelectionChangedListener() {		
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				try {
					ScanRegion<IROI> region = (ScanRegion<IROI>)((IStructuredSelection)event.getSelection()).getFirstElement();
					checkAxes(Arrays.asList(region));
				} catch (Exception ne) {
					logger.warn("Cannot select scan region", ne); // Not serious.
				}
			}
		});

		
		viewer.getControl().addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				setSelectedRegion(null);
			}
		});

	}
	
	private void checkAxes(Collection<ScanRegion<IROI>> regions) {
		if (regions==null || regions.isEmpty()) return;
		for (ScanRegion<IROI> region : regions) {
			if (region!=null) {
				IRegion iregion = system.getRegion(region.getName());
				if (iregion!=null) {
					List<String> axes = getAxisNames(system);
					iregion.setVisible(axes.containsAll(region.getScannables()));
				}
			}
		}
	}

	private List<String> getAxisNames(IPlottingSystem<?> system) {
		List<String> axes = new ArrayList<>();
		for (IAxis axis : system.getAxes()) axes.add(axis.getTitle());
		return axes;
	}

	private void setSelectedRegion(ScanRegion<IROI> sregion){
		
		Collection<IRegion> regions = system.getRegions();
		for (IRegion iRegion : regions) {
			if (!(iRegion.getUserObject() instanceof ScanRegion)) continue;
			if (sregion!=null && sregion.getName().equals(iRegion.getName())){
				iRegion.setRegionColor(ColorConstants.red);
				iRegion.setAlpha(30);
			} else {
				iRegion.setRegionColor(ColorConstants.blue);
				iRegion.setAlpha(25);
			}
		}
	}

	@Override
    public void saveState(IMemento memento) {
    	super.saveState(memento);
    	
    	if (!Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.AUTO_SAVE_REGIONS)) return;
    	try {
    		stash.stash(ScanRegions.getScanRegions(system));
		} catch (Exception e) {
			logger.error("Problem stashing control factory!", e);
		}
    }


	private String lastPath = null;
	private final static String[] extensions = new String[]{"json", "*.*"};
	private final static String[] files = new String[]{"Region files (json)", "All Files"};

	private void createActions() {

		IToolBarManager toolBarMan = getViewSite().getActionBars().getToolBarManager();
		IMenuManager    menuMan    = getViewSite().getActionBars().getMenuManager();
		MenuManager     rightClick     = new MenuManager();
		List<IContributionManager> mans = Arrays.asList(toolBarMan, menuMan, rightClick);
				
		final IAction showRegions = new Action("Show regions", IAction.AS_CHECK_BOX) {
		    public void run() {
				Activator.getDefault().getPreferenceStore().setValue(DevicePreferenceConstants.SHOW_SCAN_REGIONS, isChecked());
				setRegionsVisible(isChecked());
		    }
		};
		showRegions.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_SCAN_REGIONS));
		showRegions.setImageDescriptor(Activator.getImageDescriptor("icons/show-regions.png"));
		
		addGroups("add", mans, showRegions, createRegionActions());
		
		final IAction save = new Action("Save regions", IAction.AS_PUSH_BUTTON) {
		    public void run() {
				
				List<ScanRegion<IROI>> regions = ScanRegions.getScanRegions(system);
				
				if (regions == null) return;
				FileSelectionDialog dialog = new FileSelectionDialog(getViewSite().getShell());
				if (lastPath != null) dialog.setPath(lastPath);
				dialog.setExtensions(extensions);
				dialog.setFiles(files);
				dialog.setNewFile(true);
				dialog.setFolderSelector(false);
				
				dialog.create();
				if (dialog.open() == Dialog.CANCEL) return;
				String path = dialog.getPath();
				if (!path.endsWith(extensions[0])) { //pipeline should always be saved to .nxs
					path = path.concat("." + extensions[0]);
				}
				saveRegions(path, regions);
				lastPath = path;
			}
		};
		
		final IAction load = new Action("Load regions", IAction.AS_PUSH_BUTTON) {
			public void run() {
				
				FileSelectionDialog dialog = new FileSelectionDialog(getViewSite().getShell());
				dialog.setExtensions(extensions);
				dialog.setFiles(files);
				dialog.setNewFile(false);
				dialog.setFolderSelector(false);
				if (lastPath != null) dialog.setPath(lastPath);
				
				dialog.create();
				if (dialog.open() == Dialog.CANCEL) return;
				String path = dialog.getPath();
				readRegions(path);
				lastPath = path;
			}
		};
		save.setImageDescriptor(Activator.getImageDescriptor("icons/mask-import-wiz.png"));
		load.setImageDescriptor(Activator.getImageDescriptor("icons/mask-export-wiz.png"));
		
		addGroups("file", mans, save, load);

		IAction autoSave = new Action("Automatically save regions\nThis will keep the regions as they are\nif the application is restarted.", IAction.AS_CHECK_BOX) {
			public void run() {
				Activator.getDefault().getPreferenceStore().setValue(DevicePreferenceConstants.AUTO_SAVE_REGIONS, isChecked());
			}
		};
		autoSave.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.AUTO_SAVE_REGIONS));
		autoSave.setImageDescriptor(Activator.getImageDescriptor("icons/autosave.png"));
		addGroups("auto", mans, autoSave);
		
		viewer.getControl().setMenu(rightClick.createContextMenu(viewer.getControl()));

	}
	
	private void setRegionsVisible(boolean vis) {
		Collection<IRegion> regions = system.getRegions();
		for (IRegion iRegion : regions) {
			if (iRegion.getUserObject() instanceof ScanRegion) iRegion.setVisible(vis);
		}
	}

	private void saveRegions(String filename, List<ScanRegion<IROI>> regions) {	
		IStashing stash = ServiceHolder.getStashingService().createStash(new File(filename));
		stash.save(regions);
	}
	
	private void readRegions(String filePath) {
		IStashing stash = ServiceHolder.getStashingService().createStash(new File(filePath));
		@SuppressWarnings("unchecked")
		List<ScanRegion<IROI>> regions = stash.load(List.class);
		createRegions(regions);
		viewer.refresh();
	}
	
	private void createRegions(List<ScanRegion<IROI>> regions) {
		try {
			ScanRegions.createRegions(system, regions);
			checkAxes(regions);
		} catch (Exception e) {
			logger.error("Problem reading regions", e);
		}
	}

	private void createColumns(TableViewer viewer, DelegatingSelectionProvider prov) throws EventException, URISyntaxException {
		
        TableViewerColumn var   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		var.getColumn().setText("Name");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((ScanRegion<IROI>)element).getName();
			}
		});
		
		var   = new TableViewerColumn(viewer, SWT.LEFT, 1);
		var.getColumn().setText("Axes");
		var.getColumn().setWidth(500);
		
		var.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element==null || ((ScanRegion<IROI>)element).getScannables()==null) return "";
				return ((ScanRegion<IROI>)element).getScannables().toString();
			}
		});
		IScannableDeviceService cservice = ServiceHolder.getEventService().createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), IScannableDeviceService.class);
		var.setEditingSupport(new AxesEditingSupport(viewer, prov, cservice));
	}

	
	@Override
	public void setFocus() {
		if (viewer!=null) viewer.getTable().setFocus();
	}
	
	@Override
	public void dispose() {
		if (selectionDelegate!=null) selectionDelegate.dispose();
		if (viewer!=null) viewer.getTable().dispose();
		if (system!=null) system.removeRegionListener(regionListener);
		super.dispose();
	}

	private void addGroups(String id, List<IContributionManager> managers, IAction... actions) {
		for (IContributionManager man : managers) addGroup(id, man, actions);
	}
	private void addGroup(String id, IContributionManager manager, IAction... actions) {
		manager.add(new Separator(id));
		for (IAction action : actions) {
			manager.add(action);
		}
	}

	private IAction createRegionActions() {
		
		final String regionViewName = PlotUtil.getRegionViewName();
		final ToolTip tip = new ToolTip(viewer.getTable().getShell(), SWT.BALLOON);
        
		MenuAction rois = new MenuAction("Add Region");

		ActionContributionItem menu  = (ActionContributionItem)system.getActionBars().getMenuManager().find(BasePlottingConstants.ADD_REGION);
		IAction        menuAction = (IAction)menu.getAction();	

		for (RegionType regionType : regionTypes) {
			
            IAction action = new Action("Press to click and drag a "+regionType.getName()+" on '"+PlotUtil.getRegionViewName()+"'") {
            	public void run() {
            		try {
						ScanRegions.createRegion(system, regionType, null);
						ViewUtil.showTip(tip, "Drag a box in the '"+regionViewName+"' to create a scan region.");
					} catch (Exception e) {
						logger.error("Unable to create region!", e);
					}
            		rois.setSelectedAction(this);
            	}
            };

			final ImageDescriptor des = findImageDescriptor(menuAction, regionType.getId());
            action.setImageDescriptor(des);
            rois.add(action);
		}
		
		rois.setSelectedAction(rois.getAction(0));
		return rois;
	}

	private ImageDescriptor findImageDescriptor(IAction menuAction, String id) {

		try {
	        final Method method = menuAction.getClass().getMethod("findAction", String.class);
	        IAction paction = (IAction)method.invoke(menuAction, id);
	        return paction.getImageDescriptor();
		} catch (Exception ne) {
			logger.error("Cannot get plotting menu for adding regions!", ne);
			return Activator.getImageDescriptor("icons/ProfileBox.png");
		}
	}

}
