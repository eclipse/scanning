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
package org.eclipse.scanning.test.event.queues.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.scanning.api.event.queues.SizeLimitedRecorder;
import org.junit.Before;
import org.junit.Test;

/**
 * Test functionality of the SizeLimitedRecorder class.
 * 
 * @author Michael Wharmby
 *
 */
public class SizeLimitedRecorderTest {
	
	private SizeLimitedRecorder<Integer> slr;
	private int cap = 10;
	
	@Before
	public void createSLR() {
		slr = new SizeLimitedRecorder<Integer>(cap);
	}
	
	@Test
	public void testGenericNature() {
		SizeLimitedRecorder<Object> local = new SizeLimitedRecorder<Object>(cap);
		Object badger = new Object();
		local.add(badger);
		
		assertEquals("Returned object and added object differ!", badger, local.get(0));
	}
	
	@Test
	public void testAddingElements() {
		//Check the initial size is zero
		assertTrue("Elements already present in recorder", slr.isEmpty());
		assertEquals("Elements already present in recorder", 0, slr.size(), 0);

		slr.add(new Integer(2));

		//Check adding elements did something
		assertFalse("Elements not added to recorder", slr.isEmpty());
		assertEquals("Incorrect number of elements in recorder", 1, slr.size(), 0);
		
	}
	
	public void testOldestLatest() {
		slr.add(new Integer(8));
		slr.add(new Integer(12));
		assertEquals("Unexpected oldest value", 8, slr.oldest(), 0);
		assertEquals("Unexpected latest value", 12, slr.latest(), 0);
	}
	
	@Test
	public void testOverFillingRecorder() {
		int recorderSize = slr.getCapacity();
		
		//Populate the recorder to capacity
		for (int i = 0; i < recorderSize; i++) {
			slr.add(new Integer(5));
		}
		assertEquals("Too many or too few objects in the recorder", recorderSize, slr.size());
		
		//Add 5 more elements and check the recorder doesn't grow
		for (int i = 0; i < 5; i++) {
			slr.add(new Integer(9));
		}
		assertEquals("Too many or too few objects in the recorder given the capacity", recorderSize, slr.size());
	}
	
	@Test
	public void testChangingRecorderLimit() {
		final int recorderSize = slr.getCapacity();
		Integer serialNumber = new Integer(0);
		
		//Populate the recorder to capacity
		for (int i = 0; i < recorderSize; i++) {
			slr.add(serialNumber);
			serialNumber++;
		}
		assertEquals("Too many or too few objects in the recorder given the capacity", recorderSize, slr.size());
		
		//Increase the recorder capacity
		slr.setCapacity(recorderSize+10);
		assertEquals("Number of objects in recorder changed, without adding any", recorderSize, slr.size());
		for (int i = 0; i < 15; i++) {
			if (i == 5) assertEquals("Five more objects added, but a different number in the recorder", recorderSize+5, slr.size());
			slr.add(serialNumber);
			serialNumber++;
		}
		assertEquals("Too many or too few objects in the recorder given the capacity", recorderSize+10, slr.size());
		
		//Decrease the recorder capacity
		final Integer oldest = slr.oldest();
		slr.setCapacity(recorderSize);
		assertEquals("Latest record is not the last one added", serialNumber-1, slr.latest(), 0);
		assertTrue("Oldest element is not the oldest!", oldest!=slr.oldest());
		assertEquals("Too many or too few objects in the recorder given the capacity", recorderSize, slr.size());
		for (int i = 0; i < 5; i++) {
			slr.add(serialNumber);
			serialNumber++;
		}
		assertEquals("Oldest element is not the oldest!", serialNumber-slr.size(), slr.oldest(), 0);
		assertEquals("Latest element is not the latest!", serialNumber-1, slr.latest(), 0);
	}

}
