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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.richbeans.widgets.cell.CComboCellEditor;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeEditingSupport extends EditingSupport {
	
	private static final Logger logger = LoggerFactory.getLogger(TypeEditingSupport.class);

	public TypeEditingSupport(ColumnViewer viewer) {
		super(viewer);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		
		final MonitorRole[] values = MonitorRole.values();
	    final List<String> items  = Arrays.asList(values).stream().map(value -> value.getLabel()).collect(Collectors.toList());
		
		CComboCellEditor cellEd = new CComboCellEditor((Composite)getViewer().getControl(), items.toArray(new String[items.size()])) {
    	    protected void doSetValue(Object value) {
                if (value instanceof Enum) value = ((Enum) value).ordinal();
                super.doSetValue(value);
    	    }
    		protected Object doGetValue() {
    			Integer ordinal = (Integer)super.doGetValue();
    			try {
    			    return values[ordinal];
    			} catch (IndexOutOfBoundsException ne) {
    				return values[0];
    			}
    		}
		};
		
		return cellEd;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		return ((IScannable<?>)element).getMonitorRole();
	}

	@Override
	protected void setValue(Object element, Object value) {
		try {
			((IScannable<?>)element).setMonitorRole((MonitorRole)value);
			if (value == MonitorRole.NONE) {
				((IScannable<?>)element).setActivated(false);
			}
			getViewer().refresh(element);
		} catch (ScanningException e) {
			logger.error("Problem setting monitor role!", e);
		}
	}

}
