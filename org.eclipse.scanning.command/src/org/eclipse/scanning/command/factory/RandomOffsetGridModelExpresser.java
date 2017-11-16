/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.command.factory;

import java.util.Collection;

import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.scanning.api.points.models.RandomOffsetGridModel;

public class RandomOffsetGridModelExpresser extends PyModelExpresser<RandomOffsetGridModel> {

	@Override
	String pyExpress(RandomOffsetGridModel model, Collection<IROI> rois, boolean verbose) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("random_offset_grid(");

		// axes
		sb.append(verbose?"axes=":"");
		sb.append("('"+model.getFastAxisName()+"', '");
		sb.append(model.getSlowAxisName()+"'), ");

		// start
		sb.append(verbose?"start=":"");
		sb.append("("+model.getBoundingBox().getFastAxisStart()+", "+model.getBoundingBox().getSlowAxisStart()+"), ");

		// stop
		sb.append(verbose?"stop=":"");
		sb.append("("+model.getBoundingBox().getFastAxisEnd()+", "+model.getBoundingBox().getSlowAxisEnd()+"), ");

		// points
		sb.append(verbose?"count=":"");
		sb.append("("+model.getFastAxisPoints()+", ");
		sb.append(model.getSlowAxisPoints()+"), ");

		// snake
		sb.append(getBooleanPyExpression("snake", model.isSnake(), verbose));
		sb.append(", ");

		// continuous
		sb.append(getBooleanPyExpression("continuous", model.isContinuous(), verbose));

		// rois
		sb.append(getROIPyExpression(rois, verbose));
		sb.append(")");
		return sb.toString();
	}
}
