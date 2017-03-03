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
package org.eclipse.scanning.api.scan.event;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.ILevel;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.PositionEvent;

/**
 * 
 * A location is a json object for notifying over JMS
 * the value of an IScannable. We do not use IPosition 
 * directly because this does not have all the information
 * required. A location may be a transient state which a 
 * motor is moving through or a notifcation of an intent
 * to move. It can be thought of as a PositionEvent 
 * encapsulated in a single JSON object and with context
 * information.
 * 
 * @author Matthew Gerring
 *
 */
public class Location implements INameable {
	
	/**
	 * Must match method names in IPositionListener as reflection used on the value to 
	 * call the correct method on the listener.
	 */
	public enum LocationType {
		positionWillPerform, levelPerformed, positionChanged, positionPerformed;
	}

	
	private LocationType type;
	private String       name;
	private IPosition    position;
	private int          level;
	private List<String> levelNames;

	public Location() {
		
	}
	public Location(LocationType t, PositionEvent evnt) {
		this.type       = t;
		this.position   = evnt.getPosition();
		if (position.getNames()!=null&&position.getNames().size()==1) this.name = position.getNames().iterator().next(); 
		this.level      = evnt.getLevel();
		this.levelNames = readLevelNames(evnt.getLevelObjects());
	}
	
	private List<String> readLevelNames(List<? extends ILevel> levelObjects) {
		
		if (levelObjects==null || levelObjects.isEmpty()) return null;
		List<String> ret = new ArrayList<>();
		for (ILevel level : levelObjects) {
			ret.add(level.getName());
		}
		return ret;
	}
	public LocationType getType() {
		return type;
	}
	public void setType(LocationType type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public IPosition getPosition() {
		return position;
	}
	public void setPosition(IPosition position) {
		this.position = position;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public List<String> getLevelNames() {
		return levelNames;
	}
	public void setLevelNames(List<String> levelNames) {
		this.levelNames = levelNames;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
		result = prime * result + ((levelNames == null) ? 0 : levelNames.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((position == null) ? 0 : position.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Location other = (Location) obj;
		if (level != other.level)
			return false;
		if (levelNames == null) {
			if (other.levelNames != null)
				return false;
		} else if (!levelNames.equals(other.levelNames))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Location [type=" + type + ", name=" + name + ", position=" + position + ", level=" + level + "]";
	}
}
