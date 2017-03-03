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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.scan.ui.ControlGroup;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.scanning.device.ui.model.ModelFieldEditorFactory;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NameEditingSupport extends EditingSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(NameEditingSupport.class);
	private ControlTreeViewer controlView;
	private ModelFieldEditorFactory factory;
	
	public NameEditingSupport(ControlTreeViewer cview) {
		super(cview.getViewer());
		this.controlView = cview;
		this.factory = new ModelFieldEditorFactory();
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		try {
			if (element instanceof ControlNode) {
				return factory.getDeviceEditor(DeviceType.SCANNABLE, (Composite)getViewer().getControl());
			} 
		} catch (Exception ne) {
			logger.error("Cannot get a proper scannable editor!", ne);
		}
		return new TextCellEditor((Composite)getViewer().getControl()) {
			@Override
			protected void doSetValue(Object value) {
				if (value instanceof INamedNode) value = ((INamedNode)value).getDisplayName();
				String string = value!=null ? value.toString() : "";
				super.doSetValue(string);
			}
		};
	}

	@Override
	protected boolean canEdit(Object element) {
		INamedNode node = (INamedNode)element;
		if (controlView.isEditNode()) return true;
		return node.getName()==null || "".equals(node.getName());
	}

	@Override
	protected Object getValue(Object element) {
		INamedNode node = (INamedNode)element;
		return node instanceof ControlGroup ? node.getDisplayName() : node.getName();
	}

	@Override
	protected void setValue(Object element, Object value) {
		
		String name = (String)value;
		INamedNode node = (INamedNode)element;
		
		ControlTree tree = controlView.getControlTree();
		if (tree.contains(name)) {
			INamedNode other = tree.getNode(name);
			MessageDialog.openError(getViewer().getControl().getShell(), "Invalid Name '"+name+"'", "The name '"+name+"' is already used for another control.\n\n"
					+ "The control has a label of '"+other.getDisplayName()+"' and is linked to '"+other.getName()+"' and cannot be redefined.");
			return;
			
		}
		tree.setName(node, name);
		
		node.setDisplayName(name);
		getViewer().refresh();
	}
}
