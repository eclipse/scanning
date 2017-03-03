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
package org.eclipse.scanning.api.points;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A position with no value. Can be used in a scan to expose detectors where we do not
 * wish to move any scannables.
 * 
 * @author Matthew Gerring
 *
 */
public class StaticPosition extends AbstractPosition {

	private static final long serialVersionUID = 8325962136123756800L;
	
	@Override
	public int size() {
		return 0;
	}

	@Override
	public List<String> getNames() {
		return Collections.emptyList();
	}

	@Override
	public Object get(String name) {
		return null;
	}

	@Override
	public int getIndex(String name) {
		return 0;
	}

	@Override
	public List<Collection<String>> getDimensionNames() {
		return Collections.emptyList();
	}

	@Override
	public int getScanRank() {
		return 0;
	}

}
