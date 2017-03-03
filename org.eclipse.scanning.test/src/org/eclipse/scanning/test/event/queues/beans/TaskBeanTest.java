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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.test.event.queues.util.TestAtomQueueBeanMaker;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link TaskBean} class, which contains a queue of QueueAtoms, which
 * will form an active-queue when processed. This class only create the POJO.
 * Tests themselves in {@link AbstractAtomQueueTest}.
 * 
 * @author Michael Wharmby
 *
 */
public class TaskBeanTest extends AbstractBeanTest<TaskBean> { //extends AbstractAtomQueueTest<TaskBean, SubTaskAtom> {

	private String nameA = "Test TaskBean A", nameB = "Test TaskBean B";

	protected SubTaskAtom atomA, atomB, atomC, atomD, atomE;
	protected long timeA = 300, timeB = 1534, timeC = 654, timeD = 20, timeE = 520;

	@Before
	public void buildBeans() throws Exception {
		beanA = new TaskBean(nameA);
		beanB = new TaskBean(nameB);

		//Create atoms to be queued
		atomA = TestAtomQueueBeanMaker.makeDummySubTaskBeanA();
		atomB = TestAtomQueueBeanMaker.makeDummySubTaskBeanB();
		atomC = TestAtomQueueBeanMaker.makeDummySubTaskBeanC();
		atomD = TestAtomQueueBeanMaker.makeDummySubTaskBeanD();
		atomE = TestAtomQueueBeanMaker.makeDummySubTaskBeanE();

		//Populate the beans with the atoms
		beanA.addAtom(atomA);
		beanA.addAtom(atomB);
		beanA.addAtom(atomC);

		beanB.addAtom(atomA);
		beanB.addAtom(atomB);
		beanB.addAtom(atomC);
		beanB.addAtom(atomD);
		beanB.addAtom(atomE);

	}

	/**
	 * Test basic queue operations.
	 */
	@Test
	public void testAddAtoms() {
		List<SubTaskAtom> atomQueue = beanA.getAtomQueue();
		final int queueSize = atomQueue.size();

		//Check adding atoms to queue works
		assertTrue("atomD addition failed - already present?", beanA.addAtom(atomD));
		assertEquals("Queue size did not change after addition", queueSize+1, beanA.atomQueueSize());
		
		//Check adding nulls/identical atoms throws an expected error
		try {
			beanA.addAtom(null);
			fail("Expected NullPointerException not thrown");
		} catch (NullPointerException ex) {
			//expected
		}
		try {
			beanA.addAtom(atomD);
			fail("Expected IllegalArgumentException on adding an identical atom.");
		} catch (IllegalArgumentException iaEx) {
			//Expected
		}
	}

	/**
	 * Test viewing of queue and the sequence of stored {@link QueueAtom}s.
	 */
	@Test
	public void testNextLastViewing() {
		List<SubTaskAtom> atomQueue = beanB.getAtomQueue();
		final int queueSize = atomQueue.size();

		//Call nextAtom & viewNextAtom and check that queue changes/does not change
		assertEquals("atomA not in the next position", atomA, beanB.viewNextAtom());
		assertEquals("atomA not returned from the next position", atomA, beanB.nextAtom());
		assertEquals("Queue has not reduced in size on removal", queueSize-1, beanB.atomQueueSize());
		assertEquals("atomB not in the next position", atomB, beanB.viewNextAtom());
		assertEquals("Queue has not reduced in size on removal", queueSize-1, beanB.atomQueueSize());
	}
	
	/**
	 * Test behaviour of queue in case {@link QueueAtom}s with UIDs (see 
	 * {@link IdBean}) known not to be present are removed/otherwise requested.
	 */
	@Test
	public void testUsingNonExistentUID() {
		try {
			beanA.getIndex("fish");
			fail("Expected IllegalArgumentException with nonsense UID");
		} catch (IllegalArgumentException ex) {
			//Expected
		}
	}
	
	/**
	 * Test recovery of {@link QueueAtom} index from a given UID.
	 */
	@Test
	public void testGettingIndexFromUID() {
		assertEquals("atomC expected at index 2 of beanB", 2, beanB.getIndex(atomC.getUniqueId()));
		assertEquals("atomE expected at index 4 of beanB", 4, beanB.getIndex(atomE.getUniqueId()));
		assertEquals("atomB expected at index 1 of beanA", 1, beanA.getIndex(atomB.getUniqueId()));
	}
	
	/**
	 * Test adding of {@link QueueAtoms} with duplicate UIDs fails.
	 */
	@Test
	public void testAtomDuplication() {
		assertTrue("beanA should contain atomA, but doesn't", beanA.isAtomPresent(atomA));
		assertFalse("beanA shouldn't contain atomD, but does", beanA.isAtomPresent(atomD));
	}
	
	/**
	 * Test calculation of runtime of queue from the {@link QueueAtom}s present.
	 */
	@Test
	public void testRunTimeCalculation() {
		long beanARunTime = 0, beanBRunTime = 0;
		beanARunTime = beanARunTime + atomA.getRunTime();
		beanARunTime = beanARunTime + atomB.getRunTime();
		beanARunTime = beanARunTime + atomC.getRunTime();

		beanBRunTime = beanBRunTime + atomA.getRunTime();
		beanBRunTime = beanBRunTime + atomB.getRunTime();
		beanBRunTime = beanBRunTime + atomC.getRunTime();
		beanBRunTime = beanBRunTime + atomD.getRunTime();
		beanBRunTime = beanBRunTime + atomE.getRunTime();

		if (beanARunTime == 0 || beanBRunTime == 0) fail("Runtime is zero, cannot test this.");

		//Tests calculation method
		assertEquals("Calculated & expected beanA runTimes differ", beanARunTime, beanA.calculateRunTime());
		assertEquals("Calculated & expected beanB runTimes differ", beanBRunTime, beanB.calculateRunTime());

		//Tests setting of runTime on the queue bean
		assertEquals("Reported & expected beanA runtimes differ", beanARunTime, beanA.getRunTime());
		assertEquals("Reported & expected beanB runtimes differ", beanBRunTime, beanB.getRunTime());

		//Test updating runtime
		long newRunTime;
		beanA.addAtom(atomD);
		newRunTime = beanARunTime + atomD.getRunTime();
		assertEquals("runTime not updated on adding atom", newRunTime, beanA.getRunTime());

		//Replace the entire list
		List<SubTaskAtom> tmpList = new ArrayList<>();
		tmpList.add(atomA);
		tmpList.add(atomC);
		beanA.setAtomQueue(tmpList);
		newRunTime = atomA.getRunTime() + atomC.getRunTime();
		assertEquals("runTime not updated on adding list", newRunTime, beanA.getRunTime());

		//Next should not update the runTime!
		beanA.nextAtom();
		assertEquals("runTime changed getting next atom", newRunTime, beanA.getRunTime());
	}

}
