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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.richbeans.widgets.file.FileDialogCellEditor;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.ui.ControlFileNode;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ControlEditingSupport extends EditingSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(ControlEditingSupport.class);
	
	private IScannableDeviceService cservice;
	private ControlViewerMode       mode;

	ControlEditingSupport(ColumnViewer viewer, IScannableDeviceService cservice, ControlViewerMode mode) {
		super(viewer);
		Activator.getStore().setDefault(DevicePreferenceConstants.NUMBER_FORMAT, "##########0.0###");
		this.cservice = cservice;
		this.mode = mode;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		if (element instanceof ControlNode)     return getControlNodeEditor((ControlNode)element);
		if (element instanceof ControlFileNode) return getFileEditor((ControlFileNode)element);
		return null;
	}
	
	private CellEditor getFileEditor(final ControlFileNode element) {
    	FileDialogCellEditor fe = new FileDialogCellEditor((Composite) getViewer().getControl()) {
    		@Override
    		protected Object doGetValue() {
    			String file = (String)super.doGetValue();
    			element.setFile(file);
    			return element;
    		}

    		@Override
    		protected void doSetValue(Object value) {
    			ControlFileNode node = (ControlFileNode)value;
    			super.doSetValue(node.getFile());
    		}
    	};
    	fe.setNewFile(false);
    	fe.setDirectory(false);
		return fe;
	}

	private CellEditor getControlNodeEditor(ControlNode element) {

		final Composite parent = (Composite) getViewer().getControl();
		
		final ControlNode controlNode = (ControlNode) element; 
		Object value = getValue(controlNode);
		if (value instanceof Number) {
			return new ControlValueCellEditor(parent, cservice, mode);
		} else if (value instanceof String) {
			String[] permittedValues = getPermittedValues(controlNode);
			if (permittedValues == null) {
				return new ControlStringCellEditor(parent, controlNode);
			} else {
				return new ControlStringComboCellEditor(parent, cservice, mode, permittedValues);
			}
		} else {
			throw new RuntimeException("Unsupported type: " + value.getClass().getName());
		}
	}

	private String[] getPermittedValues(ControlNode controlNode) {
		final String scannableName = controlNode.getScannableName();
		try {
			IScannable<String> scannable = cservice.getScannable(scannableName);
			return scannable.getPermittedValues();
		} catch (Exception e) {
			throw new RuntimeException("Could not get scannable " + scannableName, e);
		}
	}

	@Override
	protected boolean canEdit(Object element) {
		
		if (mode.isDirectlyConnected()) return true;
		if (element instanceof ControlFileNode) return true;
		// can only edit ControlNodes and only when the value is not null
		// (the value could be null if the scannable doesn't exist)
		if (element instanceof ControlNode) {
			ControlNode node = (ControlNode) element;
			Object value = getValue(node);
			
			// can only edit where value is a number or string
			// (in particular cannot edit where value is null, or where it is an array of any kind)
			return value instanceof Number || value instanceof String;
		}
		return false;
	}
	
	private Object getValue(ControlNode node) {
		
		if (mode.isDirectlyConnected()) {
			return getScannableValue(node);
		} else {
			Object value = node.getValue();
			if (value == null) value = getScannableValue(node);
			return value;
		}
	}

	private Object getScannableValue(ControlNode node) {
		try {
			return cservice.getScannable(node.getName()).getPosition();
		} catch (Exception e) {
			logger.warn("Problem getting scannable value for "+node.getName()+". This might not be a problem.", e);
			return null;
		}
	}

	@Override
	protected Object getValue(Object element) {
		return element;
	}

	@Override
	protected void setValue(Object element, Object value) {
		getViewer().refresh(element);
	}

}
