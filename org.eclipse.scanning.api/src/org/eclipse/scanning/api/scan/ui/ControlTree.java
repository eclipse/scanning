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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.INamedNode;

/**
 * This is the top node for a control tree. It is both
 * created from spring and serialized to json at different
 * points. Spring calls add(...) on the children and globalize(...)
 * on this class to force the static instance to be a given value.
 * 
 * There is one static instance but the object is not a singleton
 * because it is created from json files as well. The design is
 * therefore a multiple instance, single static value rather than
 * singleton. This is intentional.
 * 
 * @author Matthew Gerring
 *
 */
public class ControlTree extends AbstractControl {

	private static ControlTree instance;
	
	private Map<String, INamedNode> content;
	private boolean treeEditable = true;

	public ControlTree() {
		this.content = new LinkedHashMap<>(37); // Ordered by insertion
		setName("Control Tree");
	}
	public ControlTree(String name) {
		this.content = new LinkedHashMap<>(37); // Ordered by insertion
		setName(name);
	}

	
	public static ControlTree getInstance() {
		return instance;
	}
	
	public void globalize() { // Called by spring
		ControlTree.instance = this;
	}
	
	/**
	 * Used to register a control such that a subsequent call
	 * to 'build' will create a legal tree.
	 * @param object
	 * @return Returns true if this collection changed as a result of the call.
	 */
	public INamedNode add(INamedNode object) {
		if (content.containsKey(object.getName())) throw new RuntimeException("The name '"+object.getName()+"' is already used!");
		return content.put(object.getName(), object);
	}
	
	public INamedNode insert(INamedNode parent, INamedNode child) {
		
		child.setParentName(parent.getName());
		((AbstractControl)parent).addChild(child);
		
		return child;
	}

	public void delete(INamedNode node) {
		if (node.getParentName()==null) return;
		INamedNode parent = getNode(node.getParentName());
		((AbstractControl)parent).removeChild(node);

		content.remove(node.getName());
	}
	
	public boolean isEmpty() {
		return content.isEmpty();
	}
	public Iterator<INamedNode> iterator() {
		return content.values().iterator();
	}

	/**
	 * A call to this method tells the factory to validate by reading
	 * the groups and controls created in spring. 
	 * 
	 * It is done this way because at the point where spring creates the 
	 * objects, they might not yet have their names and other details set.
	 */
	public boolean build() {
		
		final List<INamedNode> children = new ArrayList<>();
		
		for (String name : content.keySet()) {
			
			INamedNode iNameable = content.get(name);
			
			if (iNameable instanceof ControlGroup) {
				children.add(iNameable);
				iNameable.setParentName(getName());
			}
		}
		setChildren(children.toArray(new INamedNode[children.size()])); // All the groups.

		for (String name : content.keySet()) {
			INamedNode iNameable = content.get(name);

			if (!(iNameable instanceof ControlGroup)) {
				final String parName = iNameable.getParentName();
				final INamedNode par = getNode(parName);
				if (par!=null && !((AbstractControl)par).containsChild(iNameable)) {
					AbstractControl gpar = (AbstractControl)par;
					gpar.addChild(iNameable);
				}
			}
		}
	
		return true;
	}
	

	public Map<String, INamedNode> getContent() {
		return content;
	}

	public void setContent(Map<String, INamedNode> content) {
		this.content = content;
	}

	public INamedNode getNode(String name) {
		if (getName().equals(name)) return this;
		return content.get(name);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ControlTree other = (ControlTree) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

	public void setName(INamedNode node, String name) {
		node.setName(name);
		content.put(name, node);
	}

	public boolean isTreeEditable() {
		return treeEditable;
	}

	public void setTreeEditable(boolean treeEditable) {
		this.treeEditable = treeEditable;
	}


}
