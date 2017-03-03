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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.MoveAtom;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link MoveAtom} class. This class only create the POJO.
 * Actual tests in {@link AbstractBeanTest}. Additional test to determine atom
 * configuration.
 * 
 * @author Michael Wharmby
 *
 */
public class MoveAtomTest extends AbstractBeanTest<MoveAtom> {
	
	private String nameA = "testMoveA", nameB = "testMoveB";
	private String deviceA = "testDeviceA", deviceB = "testDeviceB"
			, deviceC = "testDeviceC", deviceD = "testDeviceD";
	private double targetA = 273.15, targetB = 957.845;
	private int targetC = 1;
	private String targetD = "barry";
	private long timeA = 26430, timeB = 4329;
	
	@Before
	public void buildBeans() throws Exception {
		Map<String, Object> beanBConf = new HashMap<>();
		beanBConf.put(deviceB, targetB);
		beanBConf.put(deviceC, targetC);
		beanBConf.put(deviceD, targetD);
				
		
		beanA = new MoveAtom(nameA, deviceA, targetA, timeA);
		beanB = new MoveAtom(nameB, beanBConf, timeB);
		
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
		
		assertEquals("Reported list and the expected list of names differ", expected, beanB.getMotorNames());
	}
}
