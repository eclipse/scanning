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
package org.eclipse.scanning.api.points.models;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.annotation.UiHidden;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

/**
 * Abstract base class for scan models, which provides property change support for the convenience of subclasses.
 *
 * @author Matthew Gerring
 *
 */
public abstract class AbstractPointsModel implements IScanPathModel {


	protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.addPropertyChangeListener(listener);
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.pcs.removePropertyChangeListener(listener);
	}
	
	@FieldDescriptor(visible=false)
	private String name;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public List<String> getScannableNames() {
		return Arrays.asList(getName());
	}
	
	@UiHidden
	public String getSummary() {
		StringBuilder buf = new StringBuilder();
		String sname = getClass().getSimpleName();
		if (sname.toLowerCase().endsWith("model")) sname = sname.substring(0, sname.length()-5);
		buf.append(sname);
		String names = getScannableNames().toString();
		names = names.replace('[', '(');
		names = names.replace(']', ')');
		buf.append(names);
		return buf.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		AbstractPointsModel other = (AbstractPointsModel) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
	public static List<String> getScannableNames(Object model) {
		if (model instanceof IScanPathModel) return ((IScanPathModel)model).getScannableNames();
		try {
			Method method = model.getClass().getMethod("getScannableNames");
			Object ret    = method.invoke(model);
			if (ret instanceof List) return (List<String>)ret;
			return null;
		} catch (Exception ne) {
			return null;
		}
		
	}
}
