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
package org.eclipse.scanning.api.annotation.scan;

import java.util.Comparator;

import org.eclipse.scanning.api.ILevel;

public class LevelComparitor implements Comparator<Object> {

	@Override
	public int compare(Object o1, Object o2) {
		int l1 = getLevel(o1);
		int l2 = getLevel(o2);
		return l1-l2;
	}

	private int getLevel(Object o) {
		if (!(o instanceof ILevel)) return ILevel.MAXIMUM;
		return ((ILevel)o).getLevel();
	}

}