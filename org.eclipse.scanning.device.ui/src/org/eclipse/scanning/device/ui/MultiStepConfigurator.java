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

package org.eclipse.scanning.device.ui;

import java.util.List;

import org.eclipse.richbeans.widgets.selector.BeanConfigurator;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.device.ui.composites.MultiStepComposite;

/**
 * This class can be configured in Spring for a given scannable
 * to change the default {@link StepModel} values created with {@link MultiStepComposite}.
 * <p>
 *
 * The name field must match the name of the scannable as configured in Spring.
 */
public class MultiStepConfigurator implements BeanConfigurator<StepModel>, INameable{

	private String name;

	private double start;
	private double width;
	private double step;

	public void register() {
		IMultiStepConfiguratorService.DEFAULT.register(this);
	}

	public void setStart(double start) {
		this.start = start;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public void setStep(double step) {
		this.step = step;
	}

	@Override
	public void configure(StepModel bean, StepModel previous, List<StepModel> context) {
		bean.setStart(previous!=null ? previous.getStop() : start);
		bean.setStop(bean.getStart() + width);
		bean.setStep(step);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;

	}

}
