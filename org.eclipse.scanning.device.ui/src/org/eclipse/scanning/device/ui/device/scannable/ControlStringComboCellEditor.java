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

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ControlStringComboCellEditor extends ComboBoxCellEditor {
	
	private static final Logger logger = LoggerFactory.getLogger(ControlStringComboCellEditor.class);
		
	private ControlNode controlNode;
	private ControlViewerMode cmode;
	private IScannableDeviceService cservice;
	private ControlValueJob<String> job;
	
	public ControlStringComboCellEditor(Composite parent, IScannableDeviceService cservice, ControlViewerMode mode, String... permittedValues) {
		super();
		setItems(permittedValues);
		setStyle(SWT.READ_ONLY);
		this.cservice = cservice;
		this.cmode = mode;
	    if (cmode.isDirectlyConnected())	job = new ControlValueJob<>();
		create(parent);
	}
	@Override
	protected Control createControl(Composite parent) {
		CCombo combo = (CCombo)super.createControl(parent);
		if (cmode.isDirectlyConnected()) {
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setValue();
				}
			});
		}
		return combo;
	}
	
	protected void setValue() {
		try {
			CCombo combo = (CCombo)getControl();
			if (controlNode!=null && cservice.getScannable(controlNode.getName())!=null) {
				String value = combo.getText();
				job.setPosition(cservice.getScannable(controlNode.getName()), value);
			}
		} catch (Exception ne) {
			logger.error("Cannot send value to scannable "+controlNode.getName(), ne);
		}
	}
	
	protected void doSetValue(final Object value) {
		controlNode = (ControlNode)value;
		// Need to set the index of the selected item in the superclass method
		final String stringValue = (String) controlNode.getValue();
		final String[] items = getItems();
		int itemIndex = 0;
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(stringValue)) {
				itemIndex = i;
				break;
			}
		}
		super.doSetValue(itemIndex);
	}
	
	protected Object doGetValue() {
		final int selectionIndex = ((Integer) super.doGetValue()).intValue();
		final String stringValue = getItems()[selectionIndex];
		controlNode.setValue(stringValue);
		
		return controlNode;
	}
	
}