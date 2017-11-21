/*-
 * Copyright © 2017 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package org.eclipse.scanning.api.device.models;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.ITimeoutable;
import org.eclipse.scanning.api.annotation.ui.FieldDescriptor;

public class GenericModel implements ITimeoutable, INameable, IReflectedModel {

	private long timeout;

	/**
	 * The name of the device
	 */
	@FieldDescriptor(label="Name", editable=false)
	private String name;


	// Auto generated methods

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (timeout ^ (timeout >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof GenericModel)) {
			return false;
		}
		GenericModel other = (GenericModel) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (timeout != other.timeout) {
			return false;
		}
		return true;
	}

	// Class functions

	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(hashCode())
				+ " [timeout=" + timeout + ", name=" + name + "]";
	}

//		return "GenericModel [timeout=" + timeout + ", name=" + name + "]";

	// interface INameable methods

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	// interface INameable methods

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
