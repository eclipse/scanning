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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

public class BeanMerge {

	/**
	 * Puts the template values in the bean, makes it easy
	 * to send events without having to set all properties
	 * in each event manually.
	 * @param event
	 * @throws IntrospectionException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public static <M> void merge(M template, M destination) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		BeanInfo beanInfo = Introspector.getBeanInfo(template.getClass());

		// Iterate over all the attributes
		for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {

			// Only copy writable attributes
			if (descriptor.getWriteMethod() != null) {
				Object originalValue = descriptor.getReadMethod().invoke(destination);

				// Only copy values values where the destination values is null
				if (originalValue == null) {
					Object defaultValue = descriptor.getReadMethod().invoke(template);
					descriptor.getWriteMethod().invoke(destination, defaultValue);
				}

			}
		}
	}


}
