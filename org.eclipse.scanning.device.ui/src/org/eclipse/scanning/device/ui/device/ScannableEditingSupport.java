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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.annotation.ui.DeviceType;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.device.ui.model.ModelFieldEditorFactory;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScannableEditingSupport extends EditingSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(ScannableEditingSupport.class);
	private ModelFieldEditorFactory factory;
	private IScannableDeviceService cservice;
	
	public ScannableEditingSupport(ColumnViewer viewer) {
		super(viewer);
		this.factory = new ModelFieldEditorFactory();
		this.cservice = factory.getScannableDeviceService();
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		try {
			return factory.getDeviceEditor(DeviceType.SCANNABLE, (Composite)getViewer().getControl());
		} catch (Exception ne) {
			logger.error("Cannot get a proper scannable editor!", ne);
		}
		return null;
	}

	@Override
	protected boolean canEdit(Object element) {
		IScannable<?> scannable = (IScannable<?>)element;
		return scannable.getName()==null || "".equals(scannable.getName());
	}

	@Override
	protected Object getValue(Object element) {
		IScannable<?> scannable = (IScannable<?>)element;
		return scannable.getName();
	}

	@Override
	protected void setValue(Object element, Object value) {
		
		String name = (String)value;
		IScannable<?> oscannable = (IScannable<?>)element;
		
		try {
			IScannable<?> nscannable = cservice.getScannable(name);
			ScannableContentProvider prov = (ScannableContentProvider)getViewer().getContentProvider();
			prov.replace(oscannable, nscannable);
			
		} catch (ScanningException e) {
			e.printStackTrace();
		}
		
	}
}
