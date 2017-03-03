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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.device.ui.util.SortNatural;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScannableContentProvider implements IStructuredContentProvider, IPositionListener {
	
	private static final Logger logger = LoggerFactory.getLogger(ScannableContentProvider.class);

	private final IScannableDeviceService cservice;
	private final Map<String, IScannable<?>> content;

	private TableViewer viewer;
	
	public ScannableContentProvider(IScannableDeviceService cservice) {
		this.cservice = cservice;
		this.content = new TreeMap<>(new SortNatural<>(true));
	}

	@Override
	public void dispose() {
		for (IScannable<?> scannable : content.values()) {
			if (scannable instanceof IPositionListenable) {
				((IPositionListenable)scannable).removePositionListener(this);
			}
		}
		content.clear();
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		this.viewer = (TableViewer)viewer;
        content.clear();
        
        if (newInput!=null) {
			for (String name : (List<String>)newInput) {
				try {
					register(name, cservice.getScannable(name));
				} catch (ScanningException e) {
					logger.error("Cannot add scannable to monitor view", e);
				}
			}
        }

	}
	@Override
	public void positionChanged(PositionEvent evt) throws ScanningException {
		update(evt);
	}
	
	@Override
	public void positionPerformed(PositionEvent evt) throws ScanningException {
		update(evt);
	}

	private void update(PositionEvent evt) {
		viewer.getControl().getDisplay().syncExec(()->viewer.refresh(evt.getDevice()));
	}

	private void register(String name, IScannable<?> scannable) {
		content.put(name, scannable);
		if (scannable instanceof IPositionListenable) {
			try {
				((IPositionListenable)scannable).addPositionListener(this);
			} catch (Exception ne) {
				logger.trace("Could not listen to value of "+scannable+". This might not be fatal.", ne);
			}
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return content.values().toArray(new IScannable[content.size()]);
	}

	public void replace(IScannable<?> oscannable, IScannable<?> nscannable) {
		
		Map<String, IScannable<?>> copy = new LinkedHashMap<>(content);
		content.clear();
		for (String name : copy.keySet()) {
			if (oscannable.getName()==name || name.equals(oscannable.getName())) {
				register(nscannable.getName(), nscannable);
			} else {
				content.put(name, copy.get(name));
			}
		}
		viewer.refresh();
	}

	public void insert(IScannable<?> sscannable, IScannable<?> nscannable) {
		if (content.isEmpty()) {
			register(nscannable.getName(), nscannable);
		} else {
			Map<String, IScannable<?>> copy = new LinkedHashMap<>(content);
			content.clear();
			for (String name : copy.keySet()) {
				if (sscannable.getName()==name || name.equals(sscannable.getName())) {
					content.put(sscannable.getName(), sscannable);
					register(nscannable.getName(), nscannable);
				} else {
					content.put(name, copy.get(name));
				}
			}
		}
		viewer.refresh();
	}

	public IScannable<?> last() {
		IScannable<?> ret = null;
		for (IScannable<?> s : content.values()) {
			ret = s;
		}
		return ret;
	}

	public Collection<String> getActivatedScannables() {
		List<String> scannables = new ArrayList<>();
		for (String name : content.keySet()) {
			if (content.get(name).isActivated()) scannables.add(name);
		}
		return scannables;
	}

	public void remove(IScannable<?> sscannable) {
		content.remove(sscannable.getName());
		if (sscannable instanceof IPositionListenable) {
			((IPositionListenable)sscannable).removePositionListener(this);
		}
		viewer.refresh();
	}

	public void add(String name) throws ScanningException {
		if (!content.containsKey(name)) register(name, cservice.getScannable(name));
	}

}
