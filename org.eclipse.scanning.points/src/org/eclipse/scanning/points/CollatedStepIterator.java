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

package org.eclipse.scanning.points;

import static java.util.stream.Collectors.toMap;

import java.util.function.Function;

import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Scalar;
import org.eclipse.scanning.api.points.ScanPointIterator;
import org.eclipse.scanning.api.points.models.CollatedStepModel;

class CollatedStepIterator extends StepIterator {

	public CollatedStepIterator(CollatedStepModel model, ScanPointIterator pyIterator) {
		super(model, pyIterator);
	}

	@Override
	protected CollatedStepModel getModel() {
		return (CollatedStepModel) super.getModel();
	}

	@Override
	public IPosition next() {
		@SuppressWarnings("unchecked")
		Scalar<Double> point = (Scalar<Double>) super.next();
		double value = point.getValue();
		IPosition next = new MapPosition(getModel().getNames().stream().collect(toMap(
					Function.identity(), name -> value)));
		next.setStepIndex(point.getStepIndex());

		return next;
	}

}
