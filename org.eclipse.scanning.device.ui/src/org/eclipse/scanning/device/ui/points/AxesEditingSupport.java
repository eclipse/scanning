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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AxesEditingSupport extends EditingSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(AxesEditingSupport.class);

	private IScannableDeviceService     cservice;
	private DelegatingSelectionProvider delegatingSelectionProvider;

	public AxesEditingSupport(ColumnViewer viewer, DelegatingSelectionProvider delegatingSelectionProvider, IScannableDeviceService cservice) {
		super(viewer);
		this.cservice = cservice;
		this.delegatingSelectionProvider = delegatingSelectionProvider;
	}
	
	private AxesCellEditor editor;

	@Override
	protected CellEditor getCellEditor(Object element) {
		if (editor==null) {
			try {
				editor = new AxesCellEditor((Composite)getViewer().getControl(), delegatingSelectionProvider, cservice);
			} catch (ScanningException e) {
				logger.error("Problem reading scannable names", e);
				return new TextCellEditor((Composite)getViewer().getControl()) {
	        	    @Override
	        		protected void doSetValue(Object value) {
	        	    	super.doSetValue("Problem reading scannable names");
	        	    }
				};
			}
		}
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
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
