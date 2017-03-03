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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.richbeans.widgets.internal.GridUtils;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.filter.IFilterService;
import org.eclipse.scanning.api.points.models.ScanRegion;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.device.ui.util.SortNatural;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class AxesCellEditor extends CellEditor {

	private ScanRegion<IROI>        region;
	private CCombo                  fast, slow;
	private DelegatingSelectionProvider prov;
	private List<String>            names;
	
	public AxesCellEditor(Composite control, DelegatingSelectionProvider prov, IScannableDeviceService cservice) throws ScanningException {
		super();
		this.prov   = prov;
		this.names = IFilterService.DEFAULT.filter("org.eclipse.scanning.scannableFilter", cservice.getScannableNames());

		Collections.sort(names, new SortNatural<>(false));
		create(control);
	}

	@Override
	protected Control createControl(Composite parent) {
		
        final Composite content = new Composite(parent, SWT.NONE);
        content.setBackground(content.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        content.setLayout(new GridLayout(4, false));
        GridUtils.removeMargins(content);
        
        this.fast = createLabelledCombo(content, "Fast Axis");
        fast.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		if (region!=null) region.getScannables().set(0, fast.getItem(fast.getSelectionIndex()));
        		fireWorkbenchSelection();
        	}
        });
        
        this.slow = createLabelledCombo(content, "Slow Axis");
        slow.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		if (region!=null) region.getScannables().set(1, slow.getItem(slow.getSelectionIndex()));
        		fireWorkbenchSelection();
        	}
        });
        
		return content;

	}

	protected void fireWorkbenchSelection() {
		if (prov!=null) prov.fireSelection(new StructuredSelection(region));
	}

	private CCombo createLabelledCombo(Composite content, String slabel) {
		
		Label label = new Label(content, SWT.NONE);
		label.setBackground(content.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		label.setText("    "+slabel+" ");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));

		CCombo ret = new CCombo(content, SWT.READ_ONLY|SWT.BORDER);
		ret.setItems(getNames());
		GridData fill = new GridData(SWT.FILL, SWT.CENTER, true, true);
		fill.widthHint=100;
		ret.setLayoutData(fill);
		
		return ret;
	}

	private String[] getNames() {
		try {
			return names.toArray(new String[names.size()]);
		} catch (Exception ne) {
			ne.printStackTrace();
			return new String[]{"Unable to get scannables", "Server may be down", ne.getMessage()!=null ? ne.getMessage() : ""};
		}
	}
	private int getIndex(String name) {
		try {
			return names.indexOf(name);
		} catch (Exception ne) {
			ne.printStackTrace();
			return 0;
		}
	}

	@Override
	protected Object doGetValue() {
		return region;
	}

	@Override
	protected void doSetFocus() {
		if (fast!=null) fast.setFocus();
	}
	
	@Override
	protected void focusLost() {
		super.focusLost();
	}

	@Override
	protected void doSetValue(Object value) {
		this.region = (ScanRegion<IROI>)value;
		if (region.getScannables()==null) {
			region.setScannables(new ArrayList<String>(Arrays.asList(new String[]{"stage_x","stage_y"})));
		}
		fast.select(getIndex(region.getScannables().get(0)));
		slow.select(getIndex(region.getScannables().get(1)));
		
	}

}
