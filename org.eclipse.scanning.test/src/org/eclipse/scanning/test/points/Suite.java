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

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({

	ArrayTest.class,
	CompoundTest.class, 
	GridTest.class, 
	LinearTest.class, 
	// LissajousTest.class, FIXME Why does this not work?
	MultiStepTest.class, 
	PointServiceTest.class,
	RandomOffsetDecoratorTest.class,
	RandomOffsetGridTest.class,	
	RasterTest.class, 
	ScanPointGeneratorFactoryTest.class,
	ScanRankTest.class,
	ScanShapeTest.class,
	SpiralTest.class,
	StaticTest.class,
	StepTest.class,
	JythonGeneratorTest.class

	// TODO Smoke tests?
	//GridTestLarge.class, 
	//RasterTestLarge.class, 
	//CompoundTestLarge.class

})
public class Suite {
}
