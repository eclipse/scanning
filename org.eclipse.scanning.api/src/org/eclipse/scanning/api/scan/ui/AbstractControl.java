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
package org.eclipse.scanning.api.scan.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.INamedNode;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;

public abstract class AbstractControl implements INamedNode {
	
	// Should not serialize parent.
	private String parentName;
	private String name;
	private String displayName;
	private INamedNode[] children;

	public void add() { // Called by spring
		ControlTree.getInstance().add(this);
	}

	@Override
	public String getParentName() {
		return parentName;
	}

	@Override
	public void setParentName(String pName) {
		this.parentName = pName;
	}
	

	@Override
	public boolean hasChildren() {
		return children!=null&&children.length>0;
	}

	public INamedNode[] getChildren() {
		return children;
	}

	public void setChildren(INamedNode[] children) {
		this.children = children;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDisplayName() {
		if (displayName==null) return getName();
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(children);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parentName == null) ? 0 : parentName.hashCode());
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
		AbstractControl other = (AbstractControl) obj;
		if (!Arrays.equals(children, other.children))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentName == null) {
			if (other.parentName != null)
				return false;
		} else if (!parentName.equals(other.parentName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+" [name=" + name + "]";
	}

	public void addChild(INamedNode child) {
		
		final INamedNode[] onodes = getChildren();
		final INamedNode[] nnodes = new INamedNode[onodes!=null ? onodes.length+1 : 1];
		if (onodes!=null) System.arraycopy(onodes, 0, nnodes, 0, onodes.length);
		nnodes[nnodes.length-1] = child;
		setChildren(nnodes);
	}

	public void removeChild(INamedNode node) {
		List<INamedNode> nodes = new ArrayList<INamedNode>(Arrays.asList(getChildren()));
		nodes.remove(node);
		setChildren(nodes.toArray(new INamedNode[nodes.size()]));
	}

	public boolean containsChild(INamedNode iNameable) {
		if (children==null) return false;
		return Arrays.asList(children).contains(iNameable);
	}

	/**
	 * 
	 * @return
	 */
	public IPosition toPosition() {
		
		MapPosition ret = new MapPosition();
		toPosition(null, ret);
		return ret;
	}
	/**
	 * @param If the value of any devices is null and they have a name, their values will be defaulted
	 * to the current IScannable.getPosition() value.
	 * @return
	 */
	public IPosition toPosition(IScannableDeviceService cservice) {
		
		MapPosition ret = new MapPosition();
		toPosition(cservice, ret);
		return ret;
	}
	
	
	private IPosition toPosition(IScannableDeviceService cservice, MapPosition toFill) {
		if (this instanceof ControlNode) {
			ControlNode cnode = (ControlNode)this;
			Object value = cnode.getValue();
			if (value == null && cservice!=null) {
				try {
					value = cservice.getScannable(cnode.getScannableName()).getPosition();
				} catch (Exception ignored) {
					// They do not have to use existing scannables, the tree is totally abstract.
					// Please implement checking if scannable names are real in a different layer
					// than the toPosition(...) call.
				}
			}
			if (value!=null) toFill.put(cnode.getScannableName(), value);
		} else {
			if (children!=null) for (INamedNode child : children) {
				if (child instanceof AbstractControl) ((AbstractControl)child).toPosition(cservice, toFill);
			}
		}
		return toFill;
	}


	public boolean contains(String name) {
		if (getName().equals(name)) return true;
		if (children!=null) for (INamedNode child : children) {
			if (child instanceof AbstractControl) {
				if (((AbstractControl)child).contains(name)) return true;
			} else {
			    if (child.getName().equals(name)) return true;
			}
		}
		return false;
	}

	
	public <T extends INamedNode> T findChild(String name) {
		if (children!=null) for (INamedNode child : children) {
			if (child.getName().equals(name)) return (T)child;
		}
		return null;
	}
}
