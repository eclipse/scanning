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
package org.eclipse.scanning.api;

/**
 * 
 * An interface for declaring parents and possibly
 * one day more distant ancestors.
 * 
 * This is designed to be used as a node in an RCP tree content provider.
 * 
 * @author Matthew Gerring
 *
 */
public interface INamedNode extends INameable {
	
	/**
	 * Used to provide a label in the UI
	 */
	default String getDisplayName() {
		return getName();
	}
	
	void setDisplayName(String name);

	/**
	 * 
	 * @return
	 */
	String getParentName();
	
	/**
	 * 
	 * @param parent
	 */
	void setParentName(String parentName);

	/**
	 * 
	 * @return the children or null if there are none.
	 */
	INamedNode[] getChildren();
	
	/**
	 * Set the children.
	 * @param children
	 */
	void setChildren(INamedNode[] children);

	/**
	 * 
	 * @return true if there are children.
	 */
	boolean hasChildren();
	
}
