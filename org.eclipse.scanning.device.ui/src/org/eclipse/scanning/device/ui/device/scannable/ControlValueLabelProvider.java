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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.ui.ControlEnumNode;
import org.eclipse.scanning.api.scan.ui.ControlFileNode;
import org.eclipse.scanning.api.scan.ui.ControlNode;
import org.eclipse.scanning.device.ui.Activator;
import org.eclipse.scanning.device.ui.DevicePreferenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ControlValueLabelProvider extends ColumnLabelProvider implements IStyledLabelProvider, IPositionListener {
	
	private static final Logger logger = LoggerFactory.getLogger(ControlValueLabelProvider.class);
	
	private IScannableDeviceService cservice;
	private ControlTreeViewer       viewer;
	
	public ControlValueLabelProvider(IScannableDeviceService cservice, ControlTreeViewer viewer) {
		Activator.getStore().setDefault(DevicePreferenceConstants.NUMBER_FORMAT, "##########0.0###");
		this.cservice = cservice;
		this.viewer   = viewer;
	}

	@Override
	public StyledString getStyledText(Object element) {

		if(!(element instanceof INamedNode)) return new StyledString();

		final String       text       = getText(element);
		final StyledString styledText = new StyledString(text!=null?text:"");
		
		INamedNode node = (INamedNode)element;
		try {
			if (node instanceof ControlNode) {
				final String scannableName = node.getName();
				if (scannableName != null && !scannableName.equals("")) {
					final IScannable<?> scannable = cservice.getScannable(node.getName());
					registerListener(scannable, node);
					if (scannable.getUnit() != null) {
						styledText.append("    ");
						styledText.append(scannable.getUnit(), StyledString.DECORATIONS_STYLER);
					}
				}				
			} else {
		
				// Intentionally do nothing!
			}
		} catch (Exception ne) {
			String message = ne.getMessage() == null ? "" : ne.getMessage();
			return styledText.append(message, StyledString.QUALIFIER_STYLER);
		}
		
		return styledText;
	}
	
	private Map<String, INamedNode>    scannableNodes;
	private Map<String, IScannable<?>> scannables;
	
	private void registerListener(IScannable<?> scannable, INamedNode node) {
		
		if (!(scannable instanceof IPositionListenable)) return;
		if (scannableNodes==null) scannableNodes = new HashMap<>();
		if (scannables==null)     scannables     = new ConcurrentHashMap<>();
		
		String name = scannable.getName();
		
		if (scannables.containsKey(name)) {
			((IPositionListenable)scannables.remove(name)).removePositionListener(this);
		}
		
		IPositionListenable pl = (IPositionListenable)scannable;
		pl.addPositionListener(this);
		scannableNodes.put(name, node);
		scannables.put(name, scannable);
	}

	/**
	 * Not the SWT thread, events come from the device.
	 */
	@Override
	public void positionChanged(PositionEvent evt) throws ScanningException {
		updatePosition(evt);
	}
	
	@Override
	public void positionPerformed(PositionEvent evt) throws ScanningException {
		updatePosition(evt);
	}
	
	private void updatePosition(PositionEvent evt) {
		if (viewer.getViewer().isCellEditorActive()) return;
		viewer.safeRefresh(scannableNodes.get(evt.getDevice().getName()));
	}

	@Override
	public void dispose() {
		super.dispose();
		for (String name : scannables.keySet()) {
			((IPositionListenable)scannables.remove(name)).removePositionListener(this);
		}
	}


	@Override
	public String getText(Object element) {
		
		if(!(element instanceof INamedNode)) return super.getText(element);
		
		INamedNode node = (INamedNode)element;
		
		if (cservice==null) return "Server Error";
		try {
			if (node instanceof ControlNode) {
				final ControlNode cnode = (ControlNode)node;
				final String scannableName = cnode.getName();
				if (scannableName == null || scannableName.equals("")) {
					return "";
				}

				Object value = cnode.getValue(viewer.getMode().isDirectlyConnected(), cservice);
				
				if (value == null) return "!VALUE";
				if (value instanceof Number) {
					try {
						final DecimalFormat format = new DecimalFormat(Activator.getStore().getString(DevicePreferenceConstants.NUMBER_FORMAT));
						return format.format(value); 
					} catch (Exception ne) {
						logger.warn("Could not format value as a decimal: ", value);
						return value.toString();
					}
				}
				if (value instanceof Object[]) {
					return Arrays.toString((Object[]) value);
				} else if (value instanceof double[]) {
					return Arrays.toString((double[]) value);
				}
				
				return value.toString();
				
			} else if (node instanceof ControlFileNode) {
				final ControlFileNode fnode = (ControlFileNode)node;
				return fnode.getFile();
			} else if (node instanceof ControlEnumNode) {
				final ControlEnumNode fnode = (ControlEnumNode)node;
				return fnode.getValue().toString();
				
			} else {
				return ""; // Only controls have values...
			}
		} catch (Exception ne) {
			logger.error("Error with value for "+node, ne);
		    return ne.toString();
		}
		
	}

}
