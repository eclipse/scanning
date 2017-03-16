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
package org.eclipse.scanning.api.malcolm.attributes;

import java.util.Arrays;

/**
 * 
 * Encapsulates an attribute as read from a malcolm device
 * 
 * @author Matt Taylor
 * @param <T> the type of the attribute's value
 */
public abstract class MalcolmAttribute<T> implements IDeviceAttribute<T> {
	
	private String name;
	private String description;
	private String[] tags;
	private boolean writeable;
	private String label;
	
	/* (non-Javadoc)
	 * @see org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute#getTags()
	 */
	@Override
	public String[] getTags() {
		return tags;
	}
	public void setTags(String[] tags) {
		this.tags = tags;
	}
	public boolean isWriteable() {
		return writeable;
	}
	public void setWriteable(boolean writeable) {
		this.writeable = writeable;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.scanning.api.malcolm.attributes.IDeviceAttribute#getValue()
	 */
	@Override
	public abstract T getValue();
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(tags);
		result = prime * result + (writeable ? 1231 : 1237);
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
		MalcolmAttribute<?> other = (MalcolmAttribute<?>) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(tags, other.tags))
			return false;
		if (writeable != other.writeable)
			return false;
		return true;
	}
	
	
}
