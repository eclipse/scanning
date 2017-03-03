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

import java.util.EventListener;

import org.eclipse.scanning.api.event.scan.ScanBean;

public interface IBeanListener<T> extends EventListener {

	/**
	 * Called when any bean is changed and published	
	 * @param evt
	 */
	void beanChangePerformed(BeanEvent<T> evt);
	
	/**
	 * Optionally the bean class may be defined which provides
	 * a hint as to how to deserialize the string.
	 * 
	 * NOTE: In GDA9 and later most objects pass through a serialization
	 * layer which also adds the bundle and class information. Therefore
	 * it is not needed to provide an implementation of the bean class.
	 * 
	 * @return
	 */
	default Class<T> getBeanClass() {
		return null;
	}
}
