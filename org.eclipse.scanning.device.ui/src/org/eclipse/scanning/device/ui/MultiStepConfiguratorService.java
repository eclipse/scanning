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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.richbeans.widgets.selector.BeanConfigurator;
import org.eclipse.scanning.api.points.models.StepModel;

public class MultiStepConfiguratorService implements IMultiStepConfiguratorService {

	private Map<String, MultiStepConfigurator> configs;

	protected MultiStepConfiguratorService() {
		configs = new HashMap<>();
	}

	@Override
	public void register(MultiStepConfigurator config) {
		configs.put(config.getName(), config);
	}

	@Override
	public BeanConfigurator<StepModel> getConfigurator(String name) {
		if (configs.containsKey(name)) {
			return configs.get(name);
		} else { // return default implementation
			return (bean, previous, context) ->  {
				bean.setStart(previous!=null ? previous.getStop() : 10.0);
				bean.setStop(bean.getStart() + 10);
				bean.setStep(1.0);
			};
		}
	}

}
