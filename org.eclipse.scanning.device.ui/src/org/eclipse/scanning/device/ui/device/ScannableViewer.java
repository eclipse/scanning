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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.ui.CommandConstants;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A viewer for editing a list of scannables.
 * 
 * @author Matthew Gerring
 *
 */
public class ScannableViewer {

	private static final Logger logger = LoggerFactory.getLogger(ScannableViewer.class);
	
	private TableViewer viewer;
	
	private IScannableDeviceService cservice;
	private Image ticked, unticked, defaultIcon;
	

	public ScannableViewer() {
		IEventService eservice = ServiceHolder.getEventService();
		try {
			if (Activator.getDefault()!=null) {
			    Activator.getDefault().getPreferenceStore().setDefault(DevicePreferenceConstants.SHOW_ACTIVATED_ONLY, true);
			}
			this.cservice = eservice.createRemoteService(new URI(CommandConstants.getScanningBrokerUri()), IScannableDeviceService.class);
		    this.defaultIcon = Activator.getImageDescriptor("icons/camera-lens.png").createImage();
		    this.ticked      = Activator.getImageDescriptor("icons/ticked.png").createImage();
		    this.unticked    = Activator.getImageDescriptor("icons/unticked.gif").createImage();
		} catch (EventException | URISyntaxException e) {
			logger.error("Problem getting remote "+IScannableDeviceService.class.getSimpleName(), e);
		}
	}

	/**
	 * Create contents of the view part.
	 * @param parent
	 */
	public void createPartControl(Composite parent) {
		
		viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
		
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createColumns(viewer, "Name", "Value", "Type");
		
		viewer.setContentProvider(new ScannableContentProvider(cservice));

		try {
			viewer.setInput(getMonitors());
		} catch (Exception e) {
			logger.error("Cannot find selected monitors");
		}

	}


	private Collection<String> getMonitors() throws Exception {
		
		boolean onlyActivated = Activator.getDefault()!=null
				              ? Activator.getDefault().getPreferenceStore().getBoolean(DevicePreferenceConstants.SHOW_ACTIVATED_ONLY)
				              : true;
		final Collection<DeviceInformation<?>> scannables = cservice.getDeviceInformation();
		final List<String> ret = new ArrayList<String>();
		for (DeviceInformation<?> info : scannables) {
			if (onlyActivated) {
			    if (info.isActivated()) ret.add(info.getName());
			} else{
				ret.add(info.getName());
			}
		}
		return ret;
	}

	private void createColumns(TableViewer tableViewer, String nameTitle, String valueTitle, String typeTitle) {

		TableViewerColumn tickedColumn = new TableViewerColumn(tableViewer, SWT.CENTER, 0);
		tickedColumn.getColumn().setWidth(24);
		tickedColumn.getColumn().setMoveable(false);
		tickedColumn.setLabelProvider(new ColumnLabelProvider() {
			
			public Image getImage(Object element) {

				if (!(element instanceof IScannable<?>)) return null;
				IScannable<?> scannable = (IScannable<?>)element;

				return scannable.isActivated() ? ticked : unticked;
			}
			
			public String getText(Object element) {
				return null;
			}
		});

		MouseAdapter mouseClick = new MouseAdapter() {
			
			public void mouseDown(MouseEvent e) {
				Point pt = new Point(e.x, e.y);
				TableItem item = viewer.getTable().getItem(pt);
				if (item == null) return;
				Rectangle rect = item.getBounds(0);
				if (rect.contains(pt)) {
					final IScannable<?> scannable = (IScannable<?>)item.getData();
					try {
						scannable.setActivated(!scannable.isActivated());
					} catch (ScanningException e1) {
						logger.error("Unable to set activated state!", e1);
					}
					tableViewer.refresh(scannable);
				}
			}
		};
		tableViewer.getTable().addMouseListener(mouseClick);


		TableViewerColumn nameColumn = new TableViewerColumn(tableViewer, SWT.LEFT, 1);
		nameColumn.getColumn().setWidth(100);
		nameColumn.getColumn().setMoveable(false);
		nameColumn.getColumn().setText(nameTitle);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (!(element instanceof IScannable<?>)) return null;
				IScannable<?> scannable = (IScannable<?>)element;
				String label = scannable.getName();
				if (label==null) label = "Unamed Device "+scannable;
				return label;
			}
		});
		nameColumn.setEditingSupport(new ScannableEditingSupport(tableViewer));
		
		TableViewerColumn valueColumn = new TableViewerColumn(tableViewer, SWT.LEFT, 2);
		valueColumn.getColumn().setWidth(150);
		valueColumn.getColumn().setMoveable(false);
		valueColumn.getColumn().setText(valueTitle);
		valueColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new ScannableValueLabelProvider()));

		TableViewerColumn typeColumn = new TableViewerColumn(tableViewer, SWT.LEFT, 3);
		typeColumn.getColumn().setWidth(100);
		typeColumn.getColumn().setMoveable(false);
		typeColumn.getColumn().setText(typeTitle);
		typeColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (!(element instanceof IScannable<?>)) return null;
				IScannable<?> scannable = (IScannable<?>)element;
				if (scannable==null) return MonitorRole.NONE.getLabel();
				if (scannable.getMonitorRole()==null) return MonitorRole.NONE.getLabel();
				return scannable.getMonitorRole().getLabel();
			}
		});
		typeColumn.setEditingSupport(new TypeEditingSupport(tableViewer));

	}
 
	public void refresh() throws Exception {
		final Collection<DeviceInformation<?>> scannables = cservice.getDeviceInformation();
		ScannableContentProvider prov = (ScannableContentProvider)viewer.getContentProvider();
        for (DeviceInformation<?> info : scannables) {
			if (info.isActivated()) prov.add(info.getName());
		}
        viewer.refresh();
	}

	public void addScannable() {
		
		ScannableContentProvider prov = (ScannableContentProvider)viewer.getContentProvider();
		IScannable<?> sscannable = getSelection();
		if (sscannable == null) sscannable = prov.last();
		IScannable<?> nscannable = AbstractScannable.empty();
		
		prov.insert(sscannable, nscannable);
		viewer.editElement(nscannable, 1);		
	}

	public void removeScannable() throws ScanningException {
		IScannable<?> sscannable = getSelection();
        if (sscannable==null) return;
        sscannable.setActivated(false);
		ScannableContentProvider prov = (ScannableContentProvider)viewer.getContentProvider();
        prov.remove(sscannable);
	}

	public IScannable<?> getSelection() {
		final ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			return (IScannable<?>)ssel.getFirstElement();
		}
		return null;
	}


	public void setScannableSelected(String scannableName) throws ScanningException {
		IScannable<?> remoteScannable = cservice.getScannable(scannableName);
		setSelection(remoteScannable);
	}

	public void setSelection(IScannable<?> scannable) {
		viewer.setSelection(new StructuredSelection(scannable));
	}

	
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	
	public void dispose() {
		if (ticked!=null)   ticked.dispose();
		if (unticked!=null) unticked.dispose();
		if (defaultIcon!=null) defaultIcon.dispose();
	}

	public ISelectionProvider getSelectionProvider() {
		return viewer;
	}

	public Control getControl() {
		return viewer.getControl();
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		viewer.addSelectionChangedListener(listener);
	}
	
	public void reset() {
		try {
			viewer.setInput(getMonitors());
		} catch (Exception e) {
			logger.error("Cannot find selected monitors");
		}		
	}
}
