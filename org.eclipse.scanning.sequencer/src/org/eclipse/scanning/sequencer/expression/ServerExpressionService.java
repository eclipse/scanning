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
package org.eclipse.scanning.sequencer.expression;

import org.eclipse.dawnsci.analysis.api.expressions.IExpressionEngine;
import org.eclipse.dawnsci.analysis.api.expressions.IExpressionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * 
 * Provides a simple expression service if the jexl one from DAWN is not available.
 * 
 * @author Matthew Gerring
 *
 */
public class ServerExpressionService implements IExpressionService {
	
	@Override
	public IExpressionEngine getExpressionEngine() {
		return new VanillaExpressionEngine();
	}

	public void start(ComponentContext context) {
		
		BundleContext bcontext = context.getBundleContext();
		ServiceReference<IExpressionService> ref = bcontext.getServiceReference(IExpressionService.class);
		if (ref == null) {
			System.out.println("Starting "+ServerExpressionService.class.getSimpleName());
			bcontext.registerService(IExpressionService.class, new ServerExpressionService(), null); 
		}
	}
}
