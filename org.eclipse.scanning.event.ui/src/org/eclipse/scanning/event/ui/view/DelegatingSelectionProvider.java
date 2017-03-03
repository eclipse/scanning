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
package org.eclipse.scanning.event.ui.view;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;

public class DelegatingSelectionProvider implements ISelectionProvider, ISelectionChangedListener {

	private Set<ISelectionChangedListener> listeners;
	private ISelectionProvider             wrapped;
	
	public DelegatingSelectionProvider(ISelectionProvider wrapped) {
		this.wrapped = wrapped;
		this.listeners = new HashSet<>();
		wrapped.addSelectionChangedListener(this);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		fireSelection(event.getSelection());
	}
	
	/**
	 * Call to programmatically fire a selection. This should be called directly from outside
	 * this class to set the workbench selection when the elements in the selection are not
	 * in the wrapped selection provider (e.g. a {@link TableViewer}).
	 * @param selection selection to fire
	 */
	public void fireSelection(ISelection selection) {
		if (listeners.isEmpty()) return;
		SelectionChangedEvent e = new SelectionChangedEvent(this, selection);
		ISelectionChangedListener[] sl = listeners.toArray(new ISelectionChangedListener[listeners.size()]);
		for (ISelectionChangedListener s : sl) s.selectionChanged(e);
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return wrapped.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Method calls listener in background thread mto make frequent updates possible.
	 */
	@Override
	public void setSelection(ISelection selection) {
		wrapped.setSelection(selection); // Causes listeners to fire
	}
	
	public void dispose() {
		wrapped.removeSelectionChangedListener(this);
		listeners.clear();
	}

}
