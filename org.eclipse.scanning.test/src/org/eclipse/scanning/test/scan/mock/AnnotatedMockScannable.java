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
package org.eclipse.scanning.test.scan.mock;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.example.scannable.MockScannable;

/**
 * Should probably use Mockito
 * @author fcp94556
 *
 */
public class AnnotatedMockScannable extends MockScannable implements AnnotationRecorder {
	

	public AnnotatedMockScannable(String name, double value) {
		super(name, value);
	}
	
	private Map<Class<? extends Annotation>, Integer> calls = new HashMap<>();
	public void record(Class<? extends Annotation> method) {
		if (!calls.containsKey(method)) calls.put(method, 0);
		calls.put(method, calls.get(method)+1);
	}
	
	public int getCount(Class<? extends Annotation> methodName) {
		Integer val = calls.get(methodName);
		if (val == null) return 0;
		return val;
	}

}
