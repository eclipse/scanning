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
package org.eclipse.scanning.api.event;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;


public class IdBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2967954413159475128L;
	
	private String    uniqueId;         // Unique id for each object.
	private boolean   explicitlySetId;
	
	public IdBean() {
		uniqueId = UUID.randomUUID().toString(); // Normally overridden
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId        = uniqueId;
		this.explicitlySetId = true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((uniqueId == null) ? 0 : uniqueId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdBean other = (IdBean) obj;
		if (explicitlySetId) {
			if (uniqueId == null) {
				if (other.uniqueId != null)
					return false;
			} else if (!uniqueId.equals(other.uniqueId))
				return false;
		}
		return true;
	}

	/**
	 * Subclasses should override this method calling super.merge(...)
	 * If they forget 
	 * @param with
	 */
	public <T extends IdBean> void merge(T with) {
		this.uniqueId = with.getUniqueId();
		
		// We this class does not have its own merge then we
		// try to do it with reflection
		
		final Method[] methods = getClass().getMethods();
		for (Method method : methods) {
			if (method.getName().equals("merge")) {
				if (method.getDeclaringClass().equals(getClass())) {
					return; // Merge is implemented in this class.
				}
			}
		}
		
		// We try to mush fields with reflection in case the 
		// class implementing this one forgets.
		Field[] wfields = with.getClass().getDeclaredFields();
		try {
			for (Field field : wfields)  {
				if (Modifier.isStatic(field.getModifiers())) continue;
				boolean isAccess = field.isAccessible();
				try {
					field.setAccessible(true);
					field.set(this, field.get(with));
				} finally {
					field.setAccessible(isAccess);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
