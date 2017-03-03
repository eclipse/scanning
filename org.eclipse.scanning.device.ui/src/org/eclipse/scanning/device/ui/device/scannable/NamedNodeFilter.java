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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.scan.ui.ControlTree;
import org.eclipse.ui.dialogs.PatternFilter;

class NamedNodeFilter extends PatternFilter {

	@Override
    protected boolean isLeafMatch(Viewer viewer, Object element){
		try {
			INamedNode ob = (INamedNode)element;
			boolean matched = super.isLeafMatch(viewer, ob.getName());
			if (!matched && ob.getParentName()!=null) {
				final ControlTree tree = (ControlTree)viewer.getInput();
				return isLeafMatch(viewer, tree.getNode(ob.getParentName()));
			}
			return matched;
		} catch (Exception ne) {
			return super.isLeafMatch(viewer, element);
		}
	}
}
