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
package org.eclipse.scanning.api.scan;

import java.util.List;

public class LevelInformation {

	private int level;
	private List<?> objects;
	public LevelInformation(int level, List<?> objects) {
		super();
		this.level = level;
		this.objects = objects;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public List<?> getObjects() {
		return objects;
	}
	public void setObjects(List<?> objects) {
		this.objects = objects;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
		result = prime * result + ((objects == null) ? 0 : objects.hashCode());
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
		LevelInformation other = (LevelInformation) obj;
		if (level != other.level)
			return false;
		if (objects == null) {
			if (other.objects != null)
				return false;
		} else if (!objects.equals(other.objects))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(hashCode()) +" [level=" + level + ", objects=" + objects + "]";
	}
}
