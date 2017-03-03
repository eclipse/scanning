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
package org.eclipse.scanning.api.event.bean;

import java.util.EventObject;

/**
 * General event which can notify of any bean change happening.
 * 
 * @author Matthew Gerring
 *
 */
public class BeanEvent<T> extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -164386744914837339L;


	public BeanEvent(T bean) {
		super(bean);
	}


	@SuppressWarnings("unchecked")
	public T getBean() {
		return (T)getSource();
	}
}
