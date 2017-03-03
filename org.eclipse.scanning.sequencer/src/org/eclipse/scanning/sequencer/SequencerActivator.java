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
package org.eclipse.scanning.sequencer;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.scanning.api.IServiceResolver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class SequencerActivator implements BundleActivator, IServiceResolver {

	private static BundleContext      context;
	private static SequencerActivator instance;

	@Override
	public void start(BundleContext c) throws Exception {
		context = c;
		instance = this;
	}

	@Override
	public void stop(BundleContext c) throws Exception {
		context = null;
		instance = null;
	}

	@Override
	public <T> T getService(Class<T> serviceClass) {
		if (context==null) return null;
		ServiceReference<T> ref = context.getServiceReference(serviceClass);
		return context.getService(ref);
	}
	
	public Object getService(String serviceClass) {
		if (context==null) return null;
		ServiceReference<?> ref = context.getServiceReference(serviceClass);
		return context.getService(ref);
	}

	public static boolean isStarted() {
		return context!=null;
	}

	public static IServiceResolver getInstance() {
		return instance;
	}

	@Override
	public <T> Collection<T> getServices(Class<T> serviceClass) throws InvalidSyntaxException {
		if (context==null) return null;
		Collection<ServiceReference<T>> refs = context.getServiceReferences(serviceClass, null);
		if (refs==null) return null;
		Collection<T> ret = new LinkedHashSet<T>(refs.size());
		for (ServiceReference<T> ref : refs) ret.add(context.getService(ref));
		return ret;
	}

}
