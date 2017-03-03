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

package org.eclipse.scanning.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.persistence.IClassRegistry;
import org.eclipse.scanning.api.malcolm.models.MapMalcolmModel;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueue;
import org.eclipse.scanning.test.scan.mock.AnnotatedMockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.eclipse.scanning.test.scan.real.TestScanBean;

public class ScanningTestClassRegistry implements IClassRegistry {

	private static final Map<String, Class<?>> idToClassMap;
	static {
		Map<String, Class<?>> tmp = new HashMap<String, Class<?>>();
		
		// scan.mock
		registerClass(tmp, AnnotatedMockDetectorModel.class);
		registerClass(tmp, MockDetectorModel.class);
		registerClass(tmp, MockWritingMandlebrotModel.class);	
		registerClass(tmp, MapMalcolmModel.class);
		
		// event.queues.dummy
		registerClass(tmp, DummyAtom.class);
		registerClass(tmp, DummyBean.class);
		registerClass(tmp, DummyHasQueue.class);
		registerClass(tmp, int[].class);

		// scan.real
		registerClass(tmp, TestScanBean.class);

		idToClassMap = tmp;
	}
	
	public ScanningTestClassRegistry() {
		
	}
	public ScanningTestClassRegistry(Class<?>... extras) {
		if (extras!=null) {
			for (Class<?> class1 : extras) {
				registerClass(idToClassMap, class1);
			}
		}
	}
	
	private static void registerClass(Map<String, Class<?>> map, Class<?> clazz) {
		map.put(clazz.getSimpleName(), clazz);
	}

	@Override
	public Map<String, Class<?>> getIdToClassMap() {
		return idToClassMap;
	}
}
