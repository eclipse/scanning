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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;

class ControlContentProvider implements ITreeContentProvider {

	
	private ControlTree factory;

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.factory = (ControlTree)newInput;
		if (factory!=null) factory.build();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object element) {
		INamedNode node = (INamedNode)element;
		return node.getChildren();
	}

	@Override
	public Object getParent(Object element) {
		INamedNode node = (INamedNode)element;
		return factory.getNode(node.getParentName());
	}

	@Override
	public boolean hasChildren(Object element) {
		INamedNode node = (INamedNode)element;
		return node.hasChildren();
	}

}
