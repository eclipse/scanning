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
package org.eclipse.scanning.example.xcen.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.event.ui.Activator;
import org.eclipse.scanning.event.ui.view.StatusQueueView;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.component.ComponentContext;

public class XcenServices {

	private ILoaderService       loaderService;
	private IEventService        eventService;

	public ILoaderService getLoaderService() {
		return loaderService;
	}

	public void setLoaderService(ILoaderService loaderService) {
		this.loaderService = loaderService;
	}
	
	private static XcenServices current;

	public void start(ComponentContext context) {
		current = this;
	}
	
	public void stop() {
		current = null;
	}

	public static XcenServices getCurrent() {
		return current;
	}

	public int hashCode() {
		return eventService.hashCode();
	}

	public boolean equals(Object obj) {
		return eventService.equals(obj);
	}

	public String toString() {
		return eventService.toString();
	}

	public IEventService getEventService() {
		return eventService;
	}

	public void setEventService(IEventService eventService) {
		this.eventService = eventService;
	}

	public final static String getQueueViewSecondaryId() {
		String uri = Activator.getJmsUri();
		String queueViewId = StatusQueueView.createId(uri, "org.eclipse.scanning.example.xcen", "org.eclipse.scanning.example.xcen.beans.XcenBean", "dataacq.xcen.STATUS_QUEUE", "dataacq.xcen.STATUS_TOPIC", "dataacq.xcen.SUBMISSION_QUEUE");
		queueViewId = queueViewId+"partName=Centering";
        return queueViewId;
	}

}
