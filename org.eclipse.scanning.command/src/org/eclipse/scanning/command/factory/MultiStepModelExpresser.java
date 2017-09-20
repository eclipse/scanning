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
import java.util.Iterator;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.points.models.StepModel;

class MultiStepModelExpresser extends PyModelExpresser<MultiStepModel> {

	@Override
	public String pyExpress(MultiStepModel mmodel, Collection<IROI> rois, boolean verbose) {

		if (rois != null && !rois.isEmpty()) throw new IllegalStateException("StepModels cannot be associated with ROIs.");

		StringBuilder buf = new StringBuilder();
		buf.append("mstep(");
		buf.append(verbose?"axis=":"");
		buf.append("'"+mmodel.getName()+"'");
		buf.append(verbose?", stepModels=[":", [");


		for (Iterator<StepModel> it = mmodel.getStepModels().iterator(); it.hasNext();) {
			String step = getString(it.next());
			buf.append(step);
            if (it.hasNext()) buf.append(", ");
		}
		buf.append("])");
		return buf.toString();
	}

	static final String getString(StepModel model) {
		StringBuilder ret = new StringBuilder("StepModel(");
		ret.append("'"+model.getName()+"'"+", ");
		ret.append(model.getStart()+", ");
		ret.append(model.getStop()+", ");
		ret.append(model.getStep());

		if (model.getExposureTime()>0d) {
			ret.append(", ");
			ret.append(model.getExposureTime());
		}

		ret.append(")");
		return ret.toString();
	}

}
