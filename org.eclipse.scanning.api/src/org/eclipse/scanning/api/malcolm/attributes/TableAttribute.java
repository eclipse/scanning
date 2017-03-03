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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.scanning.api.malcolm.MalcolmTable;

/**
 * 
 * Encapsulates a table array attribute as read from a malcolm device
 * 
 * @author Matt Taylor
 *
 */
public class TableAttribute extends MalcolmAttribute {
	public static final String TABLE_ID = "malcolm:core/TableMeta:";
	
	MalcolmTable tableValue;
	String[] headings;
	List<MalcolmAttribute> elements = new LinkedList<MalcolmAttribute>();
	
	@Override
	public MalcolmTable getValue() {
		return tableValue;
	}
	public void setValue(MalcolmTable tableValue) {
		this.tableValue = tableValue;
	}
	public String[] getHeadings() {
		return headings;
	}
	public void setHeadings(String[] headings) {
		this.headings = headings;
	}
}
