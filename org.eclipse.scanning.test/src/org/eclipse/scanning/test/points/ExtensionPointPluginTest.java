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
package org.eclipse.scanning.test.points;

import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.points.PointGeneratorService;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

// Run as plugin test
public class ExtensionPointPluginTest {
	
	private IPointGeneratorService service;
	
	@Before
	public void setup() {
		service = new PointGeneratorService(); // Can't be bothered to set up OSGi for this test.
	}

	// Must be plugin test because uses extension points.
	@Test
	public void testExtendedGenerators() throws Exception {
		
		TestGeneratorModel model = new TestGeneratorModel();
		final IPointGenerator<TestGeneratorModel> gen = service.createGenerator(model);
		
		assertNotNull(gen); // Simple as that
	}
}
