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
package org.eclipse.scanning.command.factory;

import org.eclipse.dawnsci.analysis.dataset.roi.CircularROI;

class CircularROIExpresser extends PyModelExpresser<CircularROI> {

	public String pyExpress(CircularROI croi, boolean verbose) {
		// TODO Use StringBuilder
		return "circ("
				+(verbose?"origin=":"")
					+"("+croi.getCentre()[0]+", "+croi.getCentre()[1]+"), "
				+(verbose?"radius=":"")+croi.getRadius()
			+")";
	}
}
