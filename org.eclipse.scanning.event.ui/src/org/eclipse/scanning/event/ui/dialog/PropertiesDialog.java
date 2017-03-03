/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.scanning.event.ui.dialog;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PropertiesDialog extends Dialog {

	private Map<Object,Object> props;

	public PropertiesDialog(Shell parentShell, Properties p) {
		super(parentShell);
		setShellStyle(SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		this.props = new TreeMap<Object,Object>();
		props.putAll(p);
	}

	protected Control createDialogArea(Composite parent) {
		
		// create a composite with standard margins and spacing
		Composite composite = (Composite)super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));
		
		final CLabel warning = new CLabel(composite, SWT.LEFT);
		warning.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		warning.setImage(Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/error.png").createImage());
		warning.setText("Expert queue configuration parameters, please use with caution.");
		
		TableViewer viewer   = new TableViewer(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setUseHashlookup(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createColumns(viewer);
		viewer.setContentProvider(createContentProvider());
		
		viewer.setInput(props);

		final Button adv = new Button(composite, SWT.PUSH);
		adv.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		adv.setText("Advanced...");
		
		adv.addSelectionListener(new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(getShell(), "org.dawnsci.commandserver.ui.activemqPage", null, null);
				if (pref != null) pref.open();
			}
		});

		return composite;
	}

	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			
			private Set<Entry<Object, Object>> entries;

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				@SuppressWarnings("unchecked")
				Map<Object,Object> tmp = (Map<Object,Object>)newInput;
				if (tmp==null) return;
				this.entries = tmp.entrySet();
			}
			
			@Override
			public void dispose() {
				
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return entries.toArray(new Entry[entries.size()]);
			}
		};
	}

	private void createColumns(final TableViewer viewer) {
		
        final TableViewerColumn name = new TableViewerColumn(viewer, SWT.LEFT);
		name.getColumn().setText("Name");
		name.getColumn().setWidth(200);
		name.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return humanReadable(((Entry<?, ?>)element).getKey().toString());
			}
		});

        final TableViewerColumn value = new TableViewerColumn(viewer, SWT.LEFT);
        value.getColumn().setText("Value");
        value.getColumn().setWidth(300);
        value.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				String val = ((Entry<?, ?>)element).getValue().toString();
				return val.replace("%3A", ":");
			}
		});
        value.setEditingSupport(new EditingSupport(viewer) {

			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(viewer.getTable(), SWT.NONE);
			}

			@Override
			protected boolean canEdit(Object element) {
				return element instanceof Entry<?, ?>;
			}

			@Override
			protected Object getValue(Object element) {
				Entry<Object, Object> e = (Entry<Object, Object>)element;
				return e.getValue();
			}

			@Override
			protected void setValue(Object element, Object value) {
				Entry<Object, Object> e = (Entry<Object, Object>)element;
				
				String val = (String)value;
				val = val.replace(":", "%3A");
				e.setValue(value);
				viewer.refresh(element);
			}
        	
        });
	}
	
	/**
	 * Convert Camel Case to human readable.
	 * @param s
	 * @return
	 */
	private static String humanReadable(String s) {
		String spaces = s.replaceAll(
				String.format("%s|%s|%s",
						"(?<=[A-Z])(?=[A-Z][a-z])",
						"(?<=[^A-Z])(?=[A-Z])",
						"(?<=[A-Za-z])(?=[^A-Za-z])"
						),
						" "
				);
		return spaces.substring(0,1).toUpperCase()+spaces.substring(1);
	}

	public Map<Object,Object> getProps() {
		return props;
	}

}
