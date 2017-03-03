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
package org.eclipse.scanning.points.mutators;

import org.eclipse.scanning.api.points.IMutator;
import org.eclipse.scanning.points.ScanPointGeneratorFactory;
import org.eclipse.scanning.points.ScanPointGeneratorFactory.JythonObjectFactory;

public class FixedDurationMutator implements IMutator {

	private double duration;

	public FixedDurationMutator(double duration) {
		this.duration = duration;
	}
	
	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	@Override
	public Object getMutatorAsJythonObject() {
		JythonObjectFactory fixedMutatorFactory = ScanPointGeneratorFactory.JFixedDurationMutatorFactory();
		return fixedMutatorFactory.createObject(duration);
	}
}