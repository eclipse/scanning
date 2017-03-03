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
package org.eclipse.scanning.server.application;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.scanning.api.IServiceResolver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
	
	private static BundleContext context;

	@Override
	public void start(BundleContext c) throws Exception {
		context = c;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context = null;
	}
	
	static <S> void registerService(Class<S> interfaceClass, S serviceInstance) {
		if (context==null && Boolean.getBoolean("org.eclipse.scanning.test")) return;
		context.registerService(interfaceClass, serviceInstance, null);
	}

	public static BundleContext getContext() {
		return context;
	}
	
	public static IServiceResolver createResolver() {
		if (context == null) return null;
		return new IServiceResolver() {
			@Override
			public <T> T getService(Class<T> serviceClass) {
				ServiceReference<T> ref = context.getServiceReference(serviceClass);
				return context.getService(ref);
			}

			@Override
			public <T> Collection<T> getServices(Class<T> serviceClass) throws Exception {
				if (context==null) return null;
				Collection<ServiceReference<T>> refs = context.getServiceReferences(serviceClass, null);
				if (refs==null) return null;
				Collection<T> ret = new LinkedHashSet<T>(refs.size());
				for (ServiceReference<T> ref : refs) ret.add(context.getService(ref));
				return ret;
			}
		};
	}
	

}
