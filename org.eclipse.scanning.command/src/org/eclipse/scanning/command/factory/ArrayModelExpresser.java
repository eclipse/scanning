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

import java.util.Collection;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.points.models.ArrayModel;
import org.eclipse.scanning.command.PyExpressionNotImplementedException;

class ArrayModelExpresser extends PyModelExpresser<ArrayModel> {

	
	String pyExpress(ArrayModel model, Collection<IROI> rois, boolean verbose) throws PyExpressionNotImplementedException {
		
		if (rois != null && rois.size() > 0) throw new IllegalStateException("ArrayModels cannot be associated with ROIs.");

		if (model.getPositions().length == 1 && !verbose)
			return "val('"+model.getName()+"', "+model.getPositions()[0]+")";
	
		String fragment =
				"array("
					+(verbose?"axis=":"")+"'"+model.getName()+"'"+", "
					+(verbose?"values=":"")+"[";
		boolean listPartiallyWritten = false;
	
		for (double position : model.getPositions()) {
			if (listPartiallyWritten) fragment += ", ";
			fragment += position;
			listPartiallyWritten |= true;
		}
	
		fragment += "])";
		return fragment;
	}

}
