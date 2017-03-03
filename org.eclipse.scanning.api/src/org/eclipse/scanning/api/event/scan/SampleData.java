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
package org.eclipse.scanning.api.event.scan;

import java.io.Serializable;

import org.eclipse.scanning.api.annotation.ui.EditType;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

public class SampleData implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1110342376693211593L;

	@FieldDescriptor(label="Sample Name", hint="The name of the sample.\nMay be used in the file name written by acqusition.", fieldPosition=1, regex="[a-zA-Z0-9_]+")
	private String name;
	
	@FieldDescriptor(label="Description", hint="The description of the sample.\nWill be entered in the nexus file during scanning.", fieldPosition=2, edit=EditType.LONG)
	private String description;
	
	public SampleData() {
		// no-arg constructor for spring initialization
	}
	
	public SampleData(final String name, final String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String decription) {
		this.description = decription;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
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
		SampleData other = (SampleData) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
