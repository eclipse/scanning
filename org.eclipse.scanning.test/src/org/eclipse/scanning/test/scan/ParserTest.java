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
package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertTrue;

import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.IParserResult;
import org.eclipse.scanning.api.scan.IParserService;
import org.eclipse.scanning.command.ParserServiceImpl;
import org.eclipse.scanning.test.util.DoubleUtils;
import org.junit.Before;
import org.junit.Test;

public class ParserTest {

	private static final double PRECISION = 1e-8;

	private IParserService parserService;

	@Before
	public void setUp() {
		parserService = new ParserServiceImpl();
		// TODO also have a Plugin test that uses OSGi-DS
	}

	@Test
	public void testParserService() {
		assertTrue(parserService!=null);
	}

	private void assertStepModel(StepModel stepModel, String name, double start, double stop, double step) {
		assertTrue(parserService!=null);
		assertTrue(stepModel.getName().equals(name));
		assertTrue(DoubleUtils.equalsWithinTolerance(stepModel.getStart(), start, PRECISION));
		assertTrue(DoubleUtils.equalsWithinTolerance(stepModel.getStop(), stop, PRECISION));
		assertTrue(DoubleUtils.equalsWithinTolerance(stepModel.getStep(), step, PRECISION));
	}

	@Test
	public void testCreateParser() throws Exception {
		final String scanCommand = "scan x 0 10 0.5 detector 0.1";
		// TODO how do we know what kind of parser this is?
		final IParserResult<?> parser = parserService.createParser(scanCommand);
		assertTrue(parser.getCommand().equals(scanCommand));
		assertTrue(parser.getScannableNames().contains("x"));
		final StepModel xModel = (StepModel) parser.getModel("x");
		assertStepModel(xModel, "x", 0, 10, 0.5);
		assertTrue(parser.getDetectorNames().contains("detector"));
		assertTrue(parser.getExposures().keySet().contains("detector"));
		assertTrue(DoubleUtils.equalsWithinTolerance((Double) parser.getExposures().get("detector"), 0.1, PRECISION));
	}

}
