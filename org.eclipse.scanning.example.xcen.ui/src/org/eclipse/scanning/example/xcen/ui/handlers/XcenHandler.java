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
package org.eclipse.scanning.example.xcen.ui.handlers;

import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ConsumerConfiguration;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.ui.IHandler;
import org.eclipse.scanning.example.xcen.beans.XcenBean;

public class XcenHandler implements IHandler<XcenBean> {

	protected IEventService eventService;
	protected ConsumerConfiguration conf;

	@Override
	public void init(IEventService eventService, ConsumerConfiguration conf) {
		this.eventService = eventService;
		this.conf = conf;
	}

	@Override
	public boolean isHandled(StatusBean bean) {
		return bean instanceof XcenBean;
	}

}
