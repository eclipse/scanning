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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link PositionerAtom} class. This class only create the POJO.
 * Actual tests in {@link AbstractBeanTest}. Additional test to determine atom
 * configuration.
 * 
 * @author Michael Wharmby
 *
 */
public class PositionerAtomTest extends AbstractBeanTest<PositionerAtom> {
	
	private String shrtNmA = "tstMvA", shrtNmB = "tstMvB";
	private String nameA = "Set cryostream to 273.15", nameB = "Set samX to 0.0, samY to 10 and cryojet to 273.15";
	private String deviceA = "cryojet", deviceB = "samX", 
			deviceC = "samY", deviceD = "blower";
	private double targetA = 273.15, targetB = 957.845;
	private int targetC = 10;
	private String targetD = "out";
	
	@Before
	public void buildBeans() throws Exception {
		Map<String, Object> beanBConf = new LinkedHashMap<>();
		beanBConf.put(deviceB, targetB);
		beanBConf.put(deviceC, targetC);
		beanBConf.put(deviceD, targetD);
		
		beanA = new PositionerAtom(shrtNmA, deviceA, targetA);
		beanA.setName(nameA);
		beanB = new PositionerAtom(shrtNmB, beanBConf);
		beanB.setName(nameB);
		
	}
	
	/**
	 * Test of returning only the names from the stored map of motors.
	 */
	@Test
	public void testNameReturn() {
		List<String> expected = new ArrayList<>();
		expected.add(deviceB);
		expected.add(deviceC);
		expected.add(deviceD);
		
		assertEquals("Reported list and the expected list of names differ", expected, beanB.getPositionerNames());
	}

}
