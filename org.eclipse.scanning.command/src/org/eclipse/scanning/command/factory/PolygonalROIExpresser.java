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

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PolygonalROI;

class PolygonalROIExpresser extends PyModelExpresser<PolygonalROI> {

	public String pyExpress(PolygonalROI proi, boolean verbose) {
		// TODO Use StringBuilder
		String fragment = "poly(";

		boolean pointListPartiallyWritten = false;
		for (IROI p : proi.getPoints()) {
			if (pointListPartiallyWritten) fragment += ", ";
			fragment += "("+((PointROI) p).getPointX()+", "+((PointROI) p).getPointY()+")";
		}

		fragment += ")";
		return fragment;
	}

}
