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
package org.eclipse.scanning.test.event.queues.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.points.models.CompoundModel;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Before;

/**
 * Test for the {@link ScanAtom} class. This class only create the POJO.
 * Actual tests in {@link AbstractBeanTest}.
 *
 * @author Michael Wharmby
 *
 */
public class ScanAtomTest extends AbstractBeanTest<ScanAtom> {

	private String shrtNmA = "Test scan 1", shrtNmB = "Test scan 2";

	@Before
	public void buildBeans() {
		//Config for first ScanAtom
		Map<String, Object> detectorsA = new HashMap<>();
		detectorsA.put("Test Detector A", new MockDetectorModel(30.0));
		detectorsA.put("Test Detector B", new MockDetectorModel(30.0));

		List<IScanPathModel> modelsA = new ArrayList<>();
		modelsA.add(new StaticModel());

		ScanRequest<?> scanReq = new ScanRequest<>();
		scanReq.setDetectors(detectorsA);
		scanReq.setCompoundModel(new CompoundModel<>(modelsA));


		//Config for second ScanAtom
		Map<String, Object> detectorsB = new HashMap<>();
		detectorsB.put("Test Detector C", new MockDetectorModel(50.0));

		List<IScanPathModel> modelsB = new ArrayList<>();
		modelsB.add(new StaticModel());
		modelsB.add(new StaticModel());

		List<String> monitors = new ArrayList<>();
		monitors.add("Fake monitor");

		beanA = new ScanAtom(shrtNmA, scanReq);
		beanB = new ScanAtom(shrtNmB, modelsB, detectorsB, monitors);
	}

}
