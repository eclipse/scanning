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
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.command.PyExpressionNotImplementedException;

class GridModelExpresser extends PyModelExpresser<GridModel> {

	@Override
	public String pyExpress(GridModel model, Collection<IROI> rois, boolean verbose) throws Exception {
		
		// TODO Use StringBuilder
		return "grid("
			+(verbose?"axes=":"")+"("
				+"'"+model.getFastAxisName()+"'"+", "
				+"'"+model.getSlowAxisName()+"'"+"), "
			+(verbose?"start=":"")+"("
				+model.getBoundingBox().getFastAxisStart()+", "
				+model.getBoundingBox().getSlowAxisStart()+"), "
			+(verbose?"stop=":"")+"("
				+(model.getBoundingBox().getFastAxisStart()
					+model.getBoundingBox().getFastAxisLength())+", "
				+(model.getBoundingBox().getSlowAxisStart()
					+model.getBoundingBox().getSlowAxisLength())+"), "
			+"count=("
				+model.getFastAxisPoints()+", "
				+model.getSlowAxisPoints()+")"
			+(verbose
				? (", snake="+(model.isSnake()?"True":"False"))
				: (model.isSnake()?"":", snake=False"))
			+((rois == null)
				? ""
				: ((rois.size() == 0)
					? ""
					: (", roi="+factory.pyExpress(rois, verbose))))
			+")";
	}

}
