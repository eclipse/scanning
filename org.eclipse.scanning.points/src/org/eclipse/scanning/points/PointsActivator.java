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
package org.eclipse.scanning.points;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class PointsActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		if (Boolean.getBoolean("org.eclipse.scanning.points.initJython")) {
			ScanPointGeneratorFactory.init();  // This is only needed for speed and might break GDA.
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
