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
package org.eclipse.scanning.test.annot;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.ILevel;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;

public class OrderedDevice implements ILevel {
	
	private static List<String> calledNames = new ArrayList<>();

	private int    level;
	private String name;
	
	public OrderedDevice(String name) {
		this.name = name;
	}

	@PointStart
	public void pointWillRun() throws Exception {
		calledNames.add(getName());
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ScanEnd
	public void dispose() {
		calledNames.clear();
	}

	public static List<String> getCalledNames() {
		return calledNames;
	}

	@Override
	public String toString() {
		return name+"(level=" + level+")";
	}
}
