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
package org.eclipse.scanning.api.event.dry;

import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.StatusBean;

public class DryRunCreator<T extends StatusBean> implements IProcessCreator<T> {
	
	private boolean blocking;
	
	public DryRunCreator() {
		this(true);
	}

	public DryRunCreator(boolean blocking) {
		this.blocking = blocking;
	}

	@Override
	public IConsumerProcess<T> createProcess(T bean, IPublisher<T> statusNotifier) {
		System.out.println("Creating process for name = "+bean.getName()+" id = "+bean.getUniqueId());
		return new DryRunProcess<T>(bean, statusNotifier, blocking);
	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

}
