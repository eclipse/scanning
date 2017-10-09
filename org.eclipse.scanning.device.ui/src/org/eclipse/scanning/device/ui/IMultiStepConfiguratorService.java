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


import org.eclipse.richbeans.widgets.selector.BeanConfigurator;
import org.eclipse.scanning.api.points.models.StepModel;

/**
 * IMultiStepConfiguratorService lets us register instances of {@link MultiStepConfigurator}
 * configured in Spring to adjust the default {@link StepModel} values of particular scannables.
 * <p>
 * In the same style of IFilterService, this is not an OSGi service.
 */
public interface IMultiStepConfiguratorService {

	/**
	 * The default service
	 */
	public static final IMultiStepConfiguratorService DEFAULT = new MultiStepConfiguratorService();

	/**
	 * Call to register a MultiStepConfigurator to this service
	 * @param multiStepConfigurator
	 */
	void register(MultiStepConfigurator multiStepConfigurator);

	/**
	 * Retrieve configurator registered with a particular scannable name
	 * @param scannableName
	 * @return MultiStepConfigurator registered with scannableName, or default implementation
	 */
	BeanConfigurator<StepModel> getConfigurator(String scannableName);

}
