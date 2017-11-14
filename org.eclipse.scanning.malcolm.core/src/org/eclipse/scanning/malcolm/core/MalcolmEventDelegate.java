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
package org.eclipse.scanning.malcolm.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;
import org.eclipse.scanning.api.malcolm.event.MalcolmEvent;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;

public class MalcolmEventDelegate {

	// listeners
	private Collection<IMalcolmListener<MalcolmEventBean>> listeners;

	// Bean to contain all the settings for a given
	// scan and to hold data for scan events
	private MalcolmEventBean templateBean;

    /**
     * Call to publish an event. If the topic is not opened, this
     * call prompts the delegate to open a connection. After this
     * the close method *must* be called.
     *
     * @param event
     * @throws Exception
     */
	public void sendEvent(MalcolmEventBean event)  throws Exception {

		if (templateBean!=null) BeanMerge.merge(templateBean, event);
		fireMalcolmListeners(event);
	}


	public void addMalcolmListener(IMalcolmListener<MalcolmEventBean> l) {
		if (listeners==null) listeners = Collections.synchronizedCollection(new LinkedHashSet<IMalcolmListener<MalcolmEventBean>>());
		listeners.add(l);
	}

	public void removeMalcolmListener(IMalcolmListener<MalcolmEventBean> l) {
		if (listeners==null) return;
		listeners.remove(l);
	}

	private void fireMalcolmListeners(MalcolmEventBean message) {

		if (listeners==null) return;

		// Make array, avoid multi-threading issues.
		@SuppressWarnings("unchecked")
		final IMalcolmListener<MalcolmEventBean>[] la = listeners.toArray(new IMalcolmListener[listeners.size()]);
		final MalcolmEvent<MalcolmEventBean> evt = new MalcolmEvent<MalcolmEventBean>(message);
		for (IMalcolmListener<MalcolmEventBean> l : la) l.eventPerformed(evt);
	}

	public void sendStateChanged(DeviceState state, DeviceState old, String message) throws Exception {
		final MalcolmEventBean evt = new MalcolmEventBean();
		evt.setPreviousState(old);
		evt.setDeviceState(state);
		evt.setMessage(message);
		sendEvent(evt);
	}

	public void setTemplateBean(MalcolmEventBean bean) {
		templateBean = bean;
	}

	public void close() {
		if (listeners!=null) listeners.clear();
	}

}
