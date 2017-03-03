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
package org.eclipse.scanning.test.points;

import java.util.Iterator;

import org.eclipse.scanning.api.points.AbstractGenerator;
import org.eclipse.scanning.api.points.IPosition;

public class TestGenerator extends AbstractGenerator<TestGeneratorModel> {

	@Override
	protected Iterator<IPosition> iteratorFromValidModel() {
		throw new IllegalArgumentException("Not designed to be run, just to test extension point for when people want to load by extension!");
	}

	@Override
	protected void validateModel() { }

}
