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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.richbeans.widgets.file.FileSelectionDialog;
import org.eclipse.richbeans.widgets.internal.GridUtils;
import org.eclipse.richbeans.widgets.table.ISeriesItemDescriptor;
import org.eclipse.richbeans.widgets.table.SeriesTable;
import org.eclipse.richbeans.widgets.table.event.SeriesItemEvent;
import org.eclipse.richbeans.widgets.table.event.SeriesItemListener;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.scan.ui.ControlEnumNode;
import org.eclipse.scanning.api.scan.ui.ControlFileNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.api.script.ScriptLanguage;
import org.eclipse.scanning.api.script.ScriptRequest;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.ScanningPerspective;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.device.ControlTreeUtils;
import org.eclipse.scanning.device.ui.util.PageUtil;
import org.eclipse.scanning.device.ui.util.PlotUtil;
import org.eclipse.scanning.device.ui.util.ScanRegions;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This view allows users to build up arbitrary scans
 * and run them.
 * 
 * @author Matthew Gerring
 * 
 * TODO Convert to e4 view?
 *
 */
public class ScanView  extends ViewPart implements SeriesItemListener {
	
	public static final String ID = "org.eclipse.scanning.device.ui.scanEditor";
	
	private static final Logger logger = LoggerFactory.getLogger(ScanView.class);
	
	// Services
	private IPointGeneratorService  pservice;
	private IScannableDeviceService cservice;

	// UI
	private SeriesTable  seriesTable;
	private GeneratorFilter pointsFilter;

	// Data
	private List<GeneratorDescriptor<?>> saved;
	private Map<String, ControlTree>     trees;

	// File
	private IStashing stash;

	// Preferences
	private IPreferenceStore store;


	
	public ScanView() {
		
		this.pservice     = ServiceHolder.getGeneratorService();
        this.seriesTable  = new SeriesTable();
		this.pointsFilter = new GeneratorFilter(pservice, seriesTable, this);
		this.stash = ServiceHolder.getStashingService().createStash("org.eclipse.scanning.device.ui.scan.models.json");

		this.store        = Activator.getDefault().getPreferenceStore();
		store.setDefault(DevicePreferenceConstants.START_POSITION, false);
		store.setDefault(DevicePreferenceConstants.END_POSITION,   false);
		store.setDefault(DevicePreferenceConstants.BEFORE_SCRIPT,  false);
		store.setDefault(DevicePreferenceConstants.AFTER_SCRIPT,   false);
		store.setDefault(DevicePreferenceConstants.SHOW_CONTROL_TOOLTIPS, true);
		
		this.trees = new HashMap<>(7);
	}
	
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {

		super.init(site, memento);

		if (stash.isStashed()) {
			try {
				final List<IScanPathModel> models = stash.unstash(List.class);
				this.saved = pointsFilter.createDescriptors(models);
			} catch (Exception e) {
				logger.error("Cannot load generators to memento!", e);
			}
		}
	}
	
	@Override
    public void saveState(IMemento memento) {
		super.saveState(memento);
		try {
			final List<Object> models = pointsFilter.getModels(seriesTable.getSeriesItems());
	    	stash.stash(models);
	    	
	        for (String propName : trees.keySet()) {
	        	IStashing tstash = ServiceHolder.getStashingService().createStash(propName+".json");
				tstash.stash(trees.get(propName));
			}

		} catch (Exception ne) {
			logger.error("Cannot save generators to memento!", ne);
		}
    }
    
	@Override
	public void createPartControl(Composite parent) {
		
		final Composite content = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		content.setLayout(layout);
		GridUtils.removeMargins(content);
		layout.marginTop        = 10;
	
		Composite startButton  = createPositionButton(content, DevicePreferenceConstants.START_POSITION, "Start Position", "icons/position-start.png");
		Composite beforeScript = createPositionButton(content, DevicePreferenceConstants.BEFORE_SCRIPT, "Before Script", "icons/script-before.png");
		
		final GeneratorLabelProvider prov = new GeneratorLabelProvider(0);
		seriesTable.createControl(content, prov, SWT.FULL_SELECTION | SWT.SINGLE);
		seriesTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		seriesTable.setHeaderVisible(false);
		
		Composite afterScript = createPositionButton(content, DevicePreferenceConstants.AFTER_SCRIPT, "After Script", "icons/script-after.png");
		Composite endButton = createPositionButton(content, DevicePreferenceConstants.END_POSITION, "End Position", "icons/position-end.png");
		
		final IViewSite site = getViewSite();
		
		final DelegatingSelectionProvider selectionProvider = new DelegatingSelectionProvider(seriesTable.getSelectionProvider());
		site.setSelectionProvider(selectionProvider);
		
		createControlTree(DevicePreferenceConstants.START_POSITION, "Start Position");
		createFileTree(DevicePreferenceConstants.BEFORE_SCRIPT, "Before Script");
		createFileTree(DevicePreferenceConstants.AFTER_SCRIPT, "After Script");
		createControlTree(DevicePreferenceConstants.END_POSITION, "End Position");

		List<Composite> positions = Arrays.asList(startButton, beforeScript, afterScript, endButton);
		createListeners(startButton,  positions, DevicePreferenceConstants.START_POSITION, selectionProvider);
		createListeners(beforeScript, positions, DevicePreferenceConstants.BEFORE_SCRIPT,  selectionProvider);
		createListeners(afterScript,  positions, DevicePreferenceConstants.AFTER_SCRIPT,   selectionProvider);
		createListeners(endButton,    positions, DevicePreferenceConstants.END_POSITION,   selectionProvider);
		
		createActions(site, selectionProvider);
		
		final MenuManager rightClick = new MenuManager("#PopupMenu");
		rightClick.setRemoveAllWhenShown(true);
		//createActions(rightClick);
		rightClick.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				setDynamicMenuOptions(manager);
			}
		});
		
		// Here's the data, lets show it
		seriesTable.setMenuManager(rightClick);
		seriesTable.setInput(saved, pointsFilter);
		
		DropTarget dt = seriesTable.getDropTarget();
		dt.setTransfer(new Transfer[] { TextTransfer.getInstance(),
				FileTransfer.getInstance(), ResourceTransfer.getInstance(),
				LocalSelectionTransfer.getTransfer() });
		dt.addDropListener(new DropTargetAdapter() {
			
			@Override
			public void drop(DropTargetEvent event) {
				Object dropData = event.data;
				if (dropData instanceof TreeSelection) {
					TreeSelection selectedNode = (TreeSelection) dropData;
					Object obj[] = selectedNode.toArray();
					for (int i = 0; i < obj.length; i++) {
						if (obj[i] instanceof IFile) {
							IFile file = (IFile) obj[i];
							readScans(file.getLocation().toOSString());
							return;
						}
					}
				} else if (dropData instanceof String[]) {
					for (String path : (String[])dropData){
						readScans(path);
						return;
					}
				}
				
			}
		});
		
		ScanningPerspective.createKeyPlayers();
		
		seriesTable.addSeriesEventListener(this);
		
		final List<ISeriesItemDescriptor> desi = seriesTable.getSeriesItems();
        if (desi!=null && desi.size()>0) seriesTable.setSelection(desi.get(desi.size()-1));
      
	}
	
	private ControlTree createFileTree(String propName, String name) {

		// TODO FIXME The default control tree for the start and end positions should have their own definitions
		// or the ability to create them. This code remembers what the user sets for start/end but
		// the initial fields simply come from the same as the ControlView ones.
		IStashing stash = ServiceHolder.getStashingService().createStash(propName+".json");
		
		ControlTree tree = null;
		try {
			if (stash.isStashed()) tree = stash.unstash(ControlTree.class);
		} catch (Exception ne) {
			logger.warn("Getting tree from "+stash, ne);
			tree = null;
		}

		if (tree == null) {
			tree = new ControlTree(propName);
			tree.add(new ControlFileNode(propName, "Script file"));
			tree.add(new ControlEnumNode(propName, "Script type", ScriptLanguage.JYTHON));
		}
		
		tree.setName(propName);
		tree.setDisplayName(name);
		tree.build();
		tree.setTreeEditable(false);
		trees.put(propName, tree);
		return tree;

	}
	
	private ControlTree createControlTree(String propName, String name) {
		
		// TODO FIXME The default control tree for the start and end positions should have their own definitions
		// or the ability to create them. This code remembers what the user sets for start/end but
		// the initial fields simply come from the same as the ControlView ones.
		IStashing stash = ServiceHolder.getStashingService().createStash(propName+".json");
		
		ControlTree tree = null;
		try {
			if (stash.isStashed()) tree = stash.unstash(ControlTree.class);
		} catch (Exception ne) {
			logger.warn("Getting tree from "+stash, ne);
			tree = null;
		}
		if (tree == null) {
			tree = ControlTreeUtils.parseDefaultXML();
			try {
				tree = ControlTreeUtils.clone(tree);
			} catch (Exception e) {
				logger.warn("Getting tree from default XML", e);
			}
		}
		
		if (tree==null) return null;
		tree.setName(propName);
		tree.setDisplayName(name);
		tree.build();
		trees.put(propName, tree);
		return tree;
	}

	private void createListeners(Composite position, List<Composite> positions, String propName, DelegatingSelectionProvider prov) {
		
		position.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				setPositionSelected(position, positions, propName, prov);
			}
		});	
		
		store.addPropertyChangeListener(new IPropertyChangeListener() {		
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (position.isDisposed()) {
					store.removePropertyChangeListener(this);
					return;
				}
				if (!event.getProperty().equals(propName)) return;
				boolean show = store.getBoolean(propName);
				GridUtils.setVisible(position, show);
				position.getParent().layout(new Control[]{position});
				
				if (show) {
					setPositionSelected(position, positions, propName, prov);
				}
			}
		});

	}

	protected void setPositionSelected(Composite position, List<Composite> positions, String propName, DelegatingSelectionProvider prov) {
		
		position.setFocus();
		seriesTable.deselectAll();
		for (Composite composite : positions) composite.setBackground(position.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		position.setBackground(position.getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND));
		
		seriesTable.addSelectionListener(new ISelectionChangedListener() {	
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				position.setBackground(position.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				seriesTable.removeSelectionListener(this);
			}
		});
		
		if (trees.containsKey(propName)) prov.fireSelection(new StructuredSelection(trees.get(propName)));
	}

	private Composite createPositionButton(final Composite content, final String propName, String label, String iconPath) {
		
		final CLabel position = new CLabel(content, SWT.LEFT);
		position.setBackground(content.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		position.setImage(Activator.getImageDescriptor(iconPath).createImage());
		position.setText(label);
		position.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridUtils.setVisible(position, store.getBoolean(propName));
			
		return position;
	}

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		
		if (CompoundModel.class == clazz) {
			List<IScanPathModel> models = getModels();
			if (models==null) return null;
			CompoundModel<IROI> cm = new CompoundModel<IROI>(models);
			final List<ScanRegion<IROI>> regions = ScanRegions.getScanRegions(PlotUtil.getRegionSystem());
			cm.setRegions(regions);
			return (T)cm;
		}
		if (clazz==IScanPathModel.class) {
			ISeriesItemDescriptor selected = seriesTable.getSelected();
			if (!(selected instanceof GeneratorDescriptor)) return null;
			return (T)((GeneratorDescriptor)selected).getModel();
			
		} else if (clazz==IPointGenerator.class || clazz==IPointGenerator[].class) {
			return (T)getGenerators();
			
		} else if (clazz==Object[].class||clazz==List.class) {
			return (T)getModels();
			
		} else if (clazz==IPosition[].class) {
			
			if (cservice==null) {
				try {
					this.cservice = ServiceHolder.getEventService().createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), IScannableDeviceService.class);
				} catch (EventException | URISyntaxException e) {
					logger.error("Unable to get remote device service", e);
				}
			}
			IPosition[] ret = new IPosition[2];
			if (store.getBoolean(DevicePreferenceConstants.START_POSITION)) {
				ControlTree tree = trees.get(DevicePreferenceConstants.START_POSITION);
				ret[0] = tree!=null ? tree.toPosition(cservice) : null;
			}
			if (store.getBoolean(DevicePreferenceConstants.END_POSITION))   {
				ControlTree tree = trees.get(DevicePreferenceConstants.END_POSITION);
				ret[1] = tree!=null ? tree.toPosition(cservice) : null;
			}
			return (T)ret;
			
		} else if (clazz==ScriptRequest[].class) {
			ScriptRequest[] scripts = new ScriptRequest[2];
			if (store.getBoolean(DevicePreferenceConstants.BEFORE_SCRIPT)) {
				ControlTree tree = trees.get(DevicePreferenceConstants.BEFORE_SCRIPT);
				scripts[0] = tree!=null ? createScriptRequest(tree) : null;
			}
			if (store.getBoolean(DevicePreferenceConstants.AFTER_SCRIPT))   {
				ControlTree tree = trees.get(DevicePreferenceConstants.AFTER_SCRIPT);
				scripts[1] = tree!=null ? createScriptRequest(tree) : null;
			}
			return (T)scripts;
		}

		return null;
	}

	private ScriptRequest createScriptRequest(ControlTree tree) {
		
		ControlFileNode fnode = tree.findChild("Script file");
		final String filePath = fnode.getFile();
		
		ControlEnumNode enode = tree.findChild("Script type");
		final ScriptLanguage lang = enode!=null ? (ScriptLanguage)enode.getValue() : ScriptLanguage.JYTHON;

		return new ScriptRequest(filePath, lang);
	}

	private IAction add;
	private IAction delete;
	private IAction clear;
	
	private String lastPath = null;
	private final static String[] extensions = new String[]{"json", "*.*"};
	private final static String[] files = new String[]{"Scan files (json)", "All Files"};

	private void createActions(final IViewSite site, DelegatingSelectionProvider prov) {
		
		
		IToolBarManager tmanager = site.getActionBars().getToolBarManager();
		IMenuManager    mmanager = site.getActionBars().getMenuManager();
		
		IAction start = createButtonAction(DevicePreferenceConstants.START_POSITION, 
				"Set start position\nThis is the position before a scan", 
				"icons/position-start.png", prov);
		
		IAction before = createButtonAction(DevicePreferenceConstants.BEFORE_SCRIPT, 
				"Set the before script.\nA script run before the scan.", 
                "icons/script-before.png", prov);

		IAction after = createButtonAction(DevicePreferenceConstants.AFTER_SCRIPT, 
				"Set the after script.\nA script run after the scan.", 
                "icons/script-after.png", prov);

		IAction end = createButtonAction(DevicePreferenceConstants.END_POSITION, 
				"Set end position\nThe position after a scan", 
				"icons/position-end.png", prov);

		addGroup("location", tmanager, start, before, after, end);
		addGroup("location", mmanager, start, before, after, end);
	
		
		add = new Action("Insert", Activator.getImageDescriptor("icons/clipboard-list.png")) {
			public void run() {
				seriesTable.addNew();
			}
		};

		delete = new Action("Delete", Activator.getImageDescriptor("icons/clipboard--minus.png")) {
			public void run() {
				seriesTable.delete();
			}
		};

		clear = new Action("Clear", Activator.getImageDescriptor("icons/clipboard-empty.png")) {
			public void run() {
			    boolean ok = MessageDialog.openQuestion(site.getShell(), "Confirm Clear Scan", "Do you want to clear the scan?");
			    if (!ok) return;
				seriesTable.clear();
			}
		};
		
		addGroup("manage", tmanager, add, delete, clear);
		addGroup("manage", mmanager, add, delete, clear);
		
		final IAction save = new Action("Save scan", IAction.AS_PUSH_BUTTON) {
			public void run() {
				
				List<IScanPathModel> models = getModels();
				
				if (models == null) return;
				FileSelectionDialog dialog = new FileSelectionDialog(site.getShell());
				if (lastPath != null) dialog.setPath(lastPath);
				dialog.setExtensions(extensions);
				dialog.setNewFile(true);
				dialog.setFolderSelector(false);
				
				dialog.create();
				if (dialog.open() == Dialog.CANCEL) return;
				String path = dialog.getPath();
				if (!path.endsWith(extensions[0])) { //pipeline should always be saved to .nxs
					path = path.concat("." + extensions[0]);
				}
				saveScans(path, models);
				lastPath = path;
			}
		};
		
		final IAction load = new Action("Load scan", IAction.AS_PUSH_BUTTON) {
			public void run() {
				
				FileSelectionDialog dialog = new FileSelectionDialog(site.getShell());
				dialog.setExtensions(extensions);
				dialog.setFiles(files);
				dialog.setNewFile(false);
				dialog.setFolderSelector(false);
				if (lastPath != null) dialog.setPath(lastPath);
				
				dialog.create();
				if (dialog.open() == Dialog.CANCEL) return;
				String path = dialog.getPath();
				readScans(path);
				lastPath = path;
			}
		};
		save.setImageDescriptor(Activator.getImageDescriptor("icons/mask-import-wiz.png"));
		load.setImageDescriptor(Activator.getImageDescriptor("icons/mask-export-wiz.png"));
	
		addGroup("file", tmanager, save, load);
		addGroup("file", mmanager, save, load);
		
		final IAction lock = new Action("Lock scan editing", IAction.AS_CHECK_BOX) {
			public void run() {
				store.setValue(DevicePreferenceConstants.LOCK_SCAN_SEQUENCE, isChecked());
				seriesTable.setLockEditing(isChecked());
				add.setEnabled(!isChecked());
				delete.setEnabled(!isChecked());
				clear.setEnabled(!isChecked());
			}
		};
		lock.setImageDescriptor(Activator.getImageDescriptor("icons/lock.png"));

		lock.setChecked(store.getBoolean(DevicePreferenceConstants.LOCK_SCAN_SEQUENCE));
		add.setEnabled(!lock.isChecked());
		delete.setEnabled(!lock.isChecked());
		clear.setEnabled(!lock.isChecked());
		seriesTable.setLockEditing(lock.isChecked());
		
		addGroup("lock", tmanager, lock);
		addGroup("lock", mmanager, lock);

	}
	
	private IAction createButtonAction(String propName, String label, String iconPath, DelegatingSelectionProvider prov) {
		IAction ret = new Action(label, IAction.AS_CHECK_BOX) {
			public void run() {
				updatePositionSelection(propName, isChecked(), prov);
			}
		};
		ret.setChecked(store.getBoolean(propName));
		ret.setImageDescriptor(Activator.getImageDescriptor(iconPath));
		return ret;
	}

	private void updatePositionSelection(String propertyName, boolean checked, DelegatingSelectionProvider prov) {
		store.setValue(propertyName, checked);
		if (trees.containsKey(propertyName)) {
			prov.fireSelection(new StructuredSelection(trees.get(propertyName)));
		}
		if (!checked && seriesTable.getSeriesItems()!=null && seriesTable.getSeriesItems().size()>0) {
			seriesTable.setSelection(seriesTable.getSeriesItems().get(0));
		}
	}

	private void addGroup(String id, IContributionManager manager, IAction... actions) {
		manager.add(new Separator(id));
		for (IAction action : actions) {
			manager.add(action);
		}
	}
	
	private void saveScans(String filename, List<IScanPathModel> models) {	
		IStashing stash = ServiceHolder.getStashingService().createStash(new File(filename));
		stash.save(models);
	}
	
	private void readScans(String filePath) {
		IStashing stash =ServiceHolder.getStashingService().createStash(new File(filePath));
		List<IScanPathModel> models = stash.load(List.class);
		try {
			this.saved = pointsFilter.createDescriptors(models);
			this.seriesTable.setInput(saved, pointsFilter);
		} catch (Exception e) {
			logger.error("Unexpected error refreshing saved models in "+getClass().getSimpleName(), e);
		}
	}
	
	private IPointGenerator<?>[] getGenerators() {
		
		final List<ISeriesItemDescriptor> desi = seriesTable.getSeriesItems();
		
		if (desi != null) {
			Iterator<ISeriesItemDescriptor> it = desi.iterator();
			while (it.hasNext()) if ((!(it.next() instanceof GeneratorDescriptor))) it.remove();
		}
		
		if (desi==null || desi.isEmpty()) return null;
		final IPointGenerator<?>[] pipeline = new IPointGenerator<?>[desi.size()];
		for (int i = 0; i < desi.size(); i++) {
			try {
				pipeline[i] = (IPointGenerator<?>)desi.get(i).getSeriesObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return pipeline;
	}
	private List<IScanPathModel> getModels() {
		
		IPointGenerator<?>[] gens = getGenerators();
		if (gens==null || gens.length<1) return null;
		List<IScanPathModel> mods = new ArrayList<>(gens.length);
		for (int i = 0; i < gens.length; i++) mods.add((IScanPathModel)gens[i].getModel());
		return mods;
	}


	private void setDynamicMenuOptions(IMenuManager mm) {
		
		mm.add(add);
		mm.add(delete);
		mm.add(clear);
		mm.add(new Separator());
		
		IPointGenerator<?> gen = null;
		
		try {
			ISeriesItemDescriptor selected = seriesTable.getSelected();
			if (!(selected instanceof GeneratorDescriptor)) return;
			gen = ((GeneratorDescriptor)selected).getSeriesObject();
		} catch (Exception e1) {
			
		}
		
		final IAction passUnMod = new Action("Enabled", IAction.AS_CHECK_BOX) {
			public void run() {
				ISeriesItemDescriptor current = seriesTable.getSelected();
				if (current instanceof GeneratorDescriptor) {
					try {
						((GeneratorDescriptor)current).getSeriesObject().setEnabled(isChecked());
						seriesTable.refreshTable();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		
		if (gen != null && !gen.isEnabled()) passUnMod.setChecked(true);
		mm.add(passUnMod);
	}


	@Override
	public void dispose() {
		if (tip!=null) tip.dispose();
		seriesTable.removeSeriesEventListener(this);
		seriesTable.dispose();
    }
	
	@Override
	public void setFocus() {
		seriesTable.setFocus();
	}


	@Override
	public void itemAdded(SeriesItemEvent evt) {

		final IPlottingSystem<?> system = PlotUtil.getRegionSystem();
		if (system==null) return;

		if (ScanRegions.getScanRegions(system)!=null) {
			IViewReference ref = PageUtil.getPage().findViewReference(ScanRegionView.ID);
			String name = ref!=null ? ref.getPartName() : "Scan Regions";
			if(evt.getDescriptor()!=null && evt.getDescriptor() instanceof GeneratorDescriptor) {
				try {
					final IPointGenerator<?> generator = (IPointGenerator<?>)evt.getDescriptor().getSeriesObject();
					final Object model     = generator.getModel();
					if (model instanceof IBoundingBoxModel) {
						BoundingBox box = ScanRegions.createBoxFromPlot(model);
						((IBoundingBoxModel) model).setBoundingBox(box);
						showTip("There are already scan regions defined.\n"
								+ "Drag regions to move them. Go to '"+name+"' to manage them.\n"
								+ "The bounding box of the existing regions has been used.");
						
						ISelectionProvider prov = getViewSite().getSelectionProvider();
						prov.setSelection(new StructuredSelection(evt.getDescriptor()));
					}

				} catch (Exception e) {
					logger.error("Problem creating a plotted region!", e);
				}
			}
			return; // They already have some
		}

		try {
			final IPointGenerator<?> generator = (IPointGenerator<?>)evt.getDescriptor().getSeriesObject();
			final Object model     = generator.getModel();
			if (model instanceof IBoundingBoxModel) {
				IRegion created = ScanRegions.createRegion(system, RegionType.BOX, null);
				if (created!=null) showTip("Click and drag in '"+system.getPlotName()+"' to add a region for '"+generator.getLabel()+"'");
			}
		} catch (Exception e) {
			logger.error("Problem creating a plotted region!", e);
		}

	}
   	
   	private ToolTip         tip;

   	private void showTip(String message) {
		if (!store.getBoolean(DevicePreferenceConstants.SHOW_CONTROL_TOOLTIPS)) return;
        if (tip==null) this.tip = new ToolTip(seriesTable.getControl().getShell(), SWT.BALLOON);
        ViewUtil.showTip(tip, message);
   	}

	@Override
	public void itemRemoved(SeriesItemEvent evt) {
		// TODO Auto-generated method stub
		
	}

}
