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
package org.eclipse.scanning.device.ui.device.scannable;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.richbeans.widgets.internal.GridUtils;
import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.scan.ui.ControlGroup;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.device.ControlTreeUtils;
import org.eclipse.scanning.device.ui.util.ViewUtil;
import org.eclipse.scanning.device.ui.util.ViewerUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.FilteredTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Widget to edit a tree of scannables.
 * 
 * <p>1. The widget requires a ControlTree to define what the user can edit.
 * This may come in from XML or JSON or code.
 * <p>2. The widget must be given a mode, such as where it changes values of
 * scannables directly or allows the user to enter values which do not change
 * hardware straight away.
 * <p>3. ControlTree.toPosition() may be used to create an IPosition representing
 * the values when the user just set up. So ControlTreeViewer.getControlTree().toPosition()
 * for instance.
 * 
 * <p>
 * <p>Example XML (@see {@link ControlTreeViewer} or client-fragment.xml for XML example)
 
<pre> 
 
	<!-- Create some live controls for specific devices. -->
	<bean id="Control_Factory" class="org.eclipse.scanning.api.scan.ui.ControlTree" init-method="globalize">
		<property name="name" value="Control Factory" />
	</bean>
	
	<bean id="Translations" class="org.eclipse.scanning.api.scan.ui.ControlGroup" init-method="add">
		<property name="name" value="Translations" />
		<property name="controls">
			<list>
				<ref bean="x" />
				<ref bean="y" />
				<ref bean="z" />
			</list>
		</property>
	</bean>
	<bean id="ExperimentalConditions" class="org.eclipse.scanning.api.scan.ui.ControlGroup" init-method="add">
		<property name="name" value="Experimental Conditions" />
		<property name="controls">
			<list>
				<ref bean="T" />
			</list>
		</property>
	</bean>
	
	<bean id="stage_x" class="org.eclipse.scanning.api.scan.ui.ControlNode" init-method="add" >
		<property name="displayName" value="Stage X" />
		<property name="scannableName" value="stage_x" />
		<property name="increment" value="0.1" />
	</bean>
	<bean id="stage_y" class="org.eclipse.scanning.api.scan.ui.ControlNode" init-method="add">
		<property name="displayName" value="Stage Y" />
		<property name="scannableName" value="stage_y" />
		<property name="increment" value="0.1" />
	</bean>
	<bean id="stage_z" class="org.eclipse.scanning.api.scan.ui.ControlNode" init-method="add">
		<property name="displayName" value="Stage Z" />
		<property name="scannableName" value="stage_z" />
		<property name="increment" value="0.1" />
	</bean>
	<bean id="T" class="org.eclipse.scanning.api.scan.ui.ControlNode" init-method="add">
		<property name="displayName" value="Temperature" />
		<property name="scannableName" value="T" />
		<property name="increment" value="1" />
	</bean>

</pre>

 * 
 * @author Matthew Gerring
 *
 */
public class ControlTreeViewer {
	
	private static final Logger logger = LoggerFactory.getLogger(ControlTreeViewer.class);

	// Services
	private final IScannableDeviceService cservice;
	
	// UI
	private TreeViewer viewer;
	private Composite  content;
	private List<IAction> editActions;
	private boolean setUseFilteredTree = true;
	
	// Data
	private ControlTree       defaultTree;
	private String            defaultGroupName = null;
	private final ControlViewerMode controlViewerMode;

	
	/**
	 * Create a ContolTreeViewer with the given mode, specifying whether to link to
	 * hardware directly or keep values locally.
	 * @param cservice
	 * @param mode true to set values to the hardware directly, false to take values and then keep them locally.
	 */
	public ControlTreeViewer(IScannableDeviceService cservice, ControlViewerMode mode) {
		this(null, cservice, mode);
	}

    /**
     * Create a ControlTreeViewer linked to hardware with the default tree specified.
     * @param defaultTree may be null
     * @param cservice
     */
	public ControlTreeViewer(ControlTree defaultTree, IScannableDeviceService cservice) {
		this(defaultTree, cservice, ControlViewerMode.DIRECT);
	}
	
	public ControlTreeViewer(ControlTree defaultTree, IScannableDeviceService cservice, ControlViewerMode mode) {
		this.cservice          = cservice;
		this.defaultTree       = defaultTree;
		this.controlViewerMode = mode;
	}

	
	/**
	 * Creates an editor for the tree passed in
	 * @param parent
	 * @param tree which is edited. If this ControlTreeViewer was created without specifiying a default, this is cloned for the default.
	 * @param managers, may be null
	 * @throws Exception 
	 */
	public Composite createPartControl(Composite parent, ControlTree tree, IContributionManager... managers) throws Exception {
		
		if (viewer!=null) throw new IllegalArgumentException("The createPartControl() method must only be called once!");

		if (defaultTree==null && tree==null) throw new IllegalArgumentException("No control tree has been defined!");
		
		// Clone this tree so that they can reset it!
		if (defaultTree==null) defaultTree = ControlTreeUtils.clone(tree);
        if (tree == null)      tree        = ControlTreeUtils.clone(defaultTree);
		
       if (setUseFilteredTree) {
        	FilteredTree ftree = new FilteredTree(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE, new NamedNodeFilter(), true);
        	this.viewer  = ftree.getViewer();
        	this.content = ftree;
       } else {
        	this.viewer  = new TreeViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        	this.content = viewer.getTree();
        }
		
		viewer.getTree().setLinesVisible(true);
		viewer.getTree().setHeaderVisible(false);
		ViewerUtils.setItemHeight(viewer.getTree(), 30);
		content.setLayoutData(new GridData(GridData.FILL_BOTH));

		createColumns(viewer);
		
		try {
			viewer.setContentProvider(new ControlContentProvider());
		} catch (Exception e) {
			logger.error("Cannot create content provider", e);
		}
		viewer.setInput(tree);
		viewer.expandAll();
		
		createActions(viewer, tree, managers);
		setSearchVisible(false);
		
		try {
		    registerAll();
		} catch (Exception ne) {
			logger.error("Cannot listen to motor values changing...");
		}

		return content;
	}

	public Control getControl() {
		return content;
	}

	private void createColumns(TreeViewer viewer) {
		
		viewer.setColumnProperties(new String[] { "Name", "Value"});
		ColumnViewerToolTipSupport.enableFor(viewer);
		
		TreeViewerColumn nameColumn = new TreeViewerColumn(viewer, SWT.LEFT, 0);
		nameColumn.getColumn().setText("Name");
		nameColumn.getColumn().setWidth(200);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				INamedNode node = (INamedNode)element;
				return node.getDisplayName();
			}
		});
		nameColumn.setEditingSupport(new NameEditingSupport(this));
		
		TreeViewerColumn valueColumn = new TreeViewerColumn(viewer, SWT.LEFT, 1);
		valueColumn.getColumn().setText("Value");
		valueColumn.getColumn().setWidth(300);
		valueColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new ControlValueLabelProvider(cservice, this)));
		valueColumn.setEditingSupport(new ControlEditingSupport(viewer, cservice, controlViewerMode));
	}

	/**
	 * Create the actions.
	 */
	private void createActions(final TreeViewer tviewer, ControlTree tree, IContributionManager... managers) {
		
		List<IContributionManager> mans = new ArrayList<>(Arrays.asList(managers));
		MenuManager     rightClick     = new MenuManager();
		mans.add(rightClick);
		
		// Action to add a new ControlGroup
		final IAction addGroup = new Action("Add group", Activator.getImageDescriptor("icons/ui-toolbar--purpleplus.png")) {
			public void run() {
				INamedNode nnode = getControlTree().insert(getControlTree(), new ControlGroup());
				edit(nnode, 0);
			}
		};
		
		// Action to add a new ControlNode
		final IAction addNode = new Action("Add control", Activator.getImageDescriptor("icons/ui-toolbar--plus.png")) {
			public void run() {
				addNode();
			}
		};
		addNode.setEnabled(defaultGroupName != null);
		
		// Action to remove the currently selected ControlNode or ControlGroup
		final IAction removeNode = new Action("Remove", Activator.getImageDescriptor("icons/ui-toolbar--minus.png")) {
			public void run() {
				removeNode();
			}
		};
		removeNode.setEnabled(false);
			
		ViewUtil.addGroups("add", mans, addGroup, addNode, removeNode);

		// Action to fully expand the control tree
		IAction expandAll = new Action("Expand All", Activator.getImageDescriptor("icons/expand_all.png")) {
			public void run() {
				try {
					registerAll();
				} catch (Exception e) {
					logger.error("Unable to reconnect all listeners!", e);
				}
				refresh();
			}
		};
		
		// action to show the search field
		IAction showSearch = new Action("Show search", IAction.AS_CHECK_BOX) {
			public void run() {
				setSearchVisible(isChecked());
			}
		};
		showSearch.setImageDescriptor(Activator.getImageDescriptor("icons/magnifier--pencil.png"));

		// Action to edit the currently selected ControlNode or ControlGroup
		IAction edit = new Action("Edit", Activator.getImageDescriptor("icons/pencil.png")) {
			public void run() {
				try {
					setEditNode(true);
					INamedNode node = getSelection();
					if (node!=null) {
						viewer.editElement(node, 0); // edit name of control
					}
				} finally {
					setEditNode(false);
				}
			}
		};
		
		ViewUtil.addGroups("refresh", mans, expandAll, showSearch, edit);
		
		IAction setToCurrentValue;
		IAction setAllToCurrentValue;
		if (controlViewerMode.isDirectlyConnected()) {
			setToCurrentValue = null;
			setAllToCurrentValue = null;
		} else {
			// Action to set the selected control node to the current value of the underlying scannable
			setToCurrentValue = new Action("Set to current value", Activator.getImageDescriptor("icons/reset-value.png")) {
				public void run() {
					setSelectedToCurrentValue();
				}
			};
			
			// Action to set all controls to the values of their underlying scannables
			setAllToCurrentValue = new Action("Set all to current value", Activator.getImageDescriptor("icons/reset-values.png")) {
				public void run() {
					setAllToCurrentValue();
				}
			};
			ViewUtil.addGroups("setToCurrentValue", mans, setToCurrentValue, setAllToCurrentValue);
		}
	
		// Action to reset all controls to their default value
		IAction resetAll = new Action("Reset all controls to default", Activator.getImageDescriptor("icons/arrow-return-180-left.png")) {
			public void run() {
				boolean ok = MessageDialog.openConfirm(content.getShell(), "Confirm Reset Controls", "Are you sure that you want to reset all controls to default?");
				if (!ok) return;
				try {
					setControlTree(ControlTreeUtils.clone(defaultTree));
				} catch (Exception e) {
					logger.error("Unable to set input back to default!", e);
				}
				expandAll.run();
			}
		};
		ViewUtil.addGroups("reset", mans, resetAll);
				
		// Toggles whether to show tooltips on edit
		IAction setShowTip = new Action("Show tooltip on edit", IAction.AS_CHECK_BOX) {
			public void run() {
				Activator.getStore().setValue(DevicePreferenceConstants.SHOW_CONTROL_TOOLTIPS, isChecked());
			}
		};
		setShowTip.setChecked(Activator.getStore().getBoolean(DevicePreferenceConstants.SHOW_CONTROL_TOOLTIPS));
		setShowTip.setImageDescriptor(Activator.getImageDescriptor("icons/balloon.png"));
		ViewUtil.addGroups("tip", mans, setShowTip);

		tviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				INamedNode selectedNode = getSelection();
				ControlTree tree = (ControlTree)viewer.getInput(); // They might have called setControlTree()!
				removeNode.setEnabled(tree.isTreeEditable() && selectedNode != null); // can only remove node if one is selected
				addNode.setEnabled(tree.isTreeEditable() && selectedNode != null || defaultGroupName != null); // can only add a node if one is selected (can still add a group)
				if (setToCurrentValue != null) {
					setToCurrentValue.setEnabled(selectedNode instanceof ControlNode);
				}
			}
		});

		this.editActions = Arrays.asList(addGroup, removeNode, addNode, edit, resetAll);
		setNodeActionsEnabled(tree.isTreeEditable());
		viewer.getControl().setMenu(rightClick.createContextMenu(viewer.getControl()));
	}

	private void setNodeActionsEnabled(boolean treeEditable) {
		for (IAction action : editActions) {
			action.setEnabled(treeEditable);
		}
	}

	public void setSearchVisible(boolean b) {
		if (content instanceof FilteredTree) {
			FilteredTree ftree = (FilteredTree)content;
			GridUtils.setVisible(ftree.getFilterControl().getParent(), b);
			ftree.layout(new Control[]{ftree.getFilterControl().getParent()});
		}
	}

	public ISelectionProvider getSelectionProvider() {
		return viewer;
	}

	public void setFocus() {
		if (!viewer.getTree().isDisposed()) viewer.getTree().setFocus();
	}
	
	public void edit(INamedNode node, int index) {
     	refresh();
    	viewer.editElement(node, index);
	}

	public void refresh() {
		viewer.refresh();
		viewer.expandAll();
	}

	public INamedNode getSelection() {
		final ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			return (INamedNode)ssel.getFirstElement();
		}
		return null;
	}
	
	public void setSelection(INamedNode node) {
		final IStructuredSelection sel = new StructuredSelection(node);
		viewer.setSelection(sel);
	}


	
	private boolean editNode = false; // Can be set to true when UI wants to edit

	public boolean isEditNode() {
		return editNode;
	}

	public void setEditNode(boolean editNode) {
		this.editNode = editNode;
	}

	ColumnViewer getViewer() {
		return viewer;
	}

	public ControlTree getControlTree() {
		return (ControlTree)viewer.getInput();
	}

	public void setControlTree(ControlTree controlTree) {
		controlTree.build();
		viewer.setInput(controlTree);
		setNodeActionsEnabled(controlTree.isTreeEditable());
		content.getParent().layout(new Control[]{content});
		viewer.expandAll();
	}
	
	public void setDefaultGroupName(String defaultGroupName) {
		this.defaultGroupName = defaultGroupName;
	}

	public void dispose() {

	}


	/**
	 * Clears all the current listeners and registers all the new ones.
	 * 
	 * @throws EventException
	 * @throws URISyntaxException 
	 */
	private void registerAll() throws EventException, URISyntaxException {
		
		if (!controlViewerMode.isDirectlyConnected()) return; // Nothing to monitor.
		
	}

	/**
	 * Call to programmatically add a node. The user will be shown a combo of available scannable names.
	 */
	public void addNode() {
		INamedNode selectedNode = getSelection();
		ControlTree controlTree = getControlTree();
		if (selectedNode == null) {
			if (defaultGroupName == null) return;
			selectedNode = controlTree.getNode(defaultGroupName); // add new node to default group
		}
		if (!(selectedNode instanceof ControlGroup)) selectedNode = controlTree.getNode(selectedNode.getParentName());
		if (selectedNode instanceof ControlGroup) {
			INamedNode control = controlTree.insert(selectedNode, new ControlNode("", 0.1));
			edit(control, 0);
		}
	}

	private void removeNode() {
		final INamedNode selectedNode = getSelection();
		ControlTree controlTree = getControlTree();
		INamedNode parent = controlTree.getNode(selectedNode.getParentName());
		if (selectedNode.getChildren()==null || selectedNode.getChildren().length<1) {
			controlTree.delete(selectedNode);
		} else {
			boolean ok = MessageDialog.openQuestion(content.getShell(), "Confirm Delete", "The item '"+selectedNode.getName()+"' is a group.\n\nAre you sure you would like to delete it?");
			if (ok) controlTree.delete(selectedNode);
		}
		viewer.refresh();
		if (parent.hasChildren()) {
			setSelection(parent.getChildren()[parent.getChildren().length-1]);
		} else {
		    setSelection(parent);
		}
	}
	
	private void setSelectedToCurrentValue() {
		INamedNode selectedNode = getSelection();
		if ((selectedNode instanceof ControlNode)) {
			setToCurrentValue((ControlNode) selectedNode);
			viewer.refresh(selectedNode);
		}
	}

	private void setToCurrentValue(ControlNode controlNode) {
		try {
			IScannable<?> scannable = cservice.getScannable(controlNode.getName());
			controlNode.setValue(scannable.getPosition());
		} catch (Exception e) {
			logger.error("Cannot get value for " + controlNode.getName(), e);
		}
	}
	
	private void setAllToCurrentValue() {
		ControlTree controlTree = (ControlTree) viewer.getInput();
		setAllToCurrentValue(controlTree);
		viewer.refresh();
	}
	
	private void setAllToCurrentValue(INamedNode namedNode) {
		for (INamedNode childNode : namedNode.getChildren()) {
			if (childNode instanceof ControlGroup) {
				setAllToCurrentValue(childNode); // recursive call
			} else if (childNode instanceof ControlNode) {
				setToCurrentValue((ControlNode) childNode);
			}
		}
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener i) {
		viewer.addSelectionChangedListener(i);
	}
	public void removeSelectionChangedListener(ISelectionChangedListener i) {
		viewer.removeSelectionChangedListener(i);
	}
	
	public void applyEditorValue() {
		if (!viewer.isCellEditorActive()) return;
		viewer.applyEditorValue();
	}

	public void setUseFilteredTree(boolean useFilteredTree) {
		this.setUseFilteredTree  = useFilteredTree;
	}

	public ControlViewerMode getMode() {
		return controlViewerMode;
	}

	public void safeRefresh(INamedNode node) {
		if (node == null) return;
		if (viewer.getControl().isDisposed()) return;
		if (viewer.getControl().getDisplay().isDisposed()) return;
		viewer.getControl().getDisplay().syncExec(()->{
			try {
				viewer.refresh(node);
			} catch (Exception ignored) {
				// Sometimes happens in unit tests.
			}
		});
	}

}
