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
package org.eclipse.scanning.test.scan.nexus;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.annotation.scan.FileDeclared;
import org.eclipse.scanning.api.scan.IScanParticipant;
import org.eclipse.scanning.test.scan.mock.AnnotationRecorder;

/**
 * 
 * Class which counts annotations called.
 * 
 * @author Matthew Gerring
 *
 */
public class MockScanParticpiant implements AnnotationRecorder, IScanParticipant{

	
	private Map<Class<? extends Annotation>, Integer> calls = new HashMap<>();
	private List<String> paths = new ArrayList<>();
	
	public void record(Class<? extends Annotation> method) {
		if (!calls.containsKey(method)) calls.put(method, 0);
		calls.put(method, calls.get(method)+1);
	}
	
	public int getCount(Class<? extends Annotation> methodName) {
		Integer val = calls.get(methodName);
		if (val == null) return 0;
		return val;
	}

	public void clear() {
		calls.clear();
		paths.clear();
	}

	@FileDeclared
	public void addFile(String path) {
		paths.add(path);
	}

	public List<String> getPaths() {
		return paths;
	}
}
