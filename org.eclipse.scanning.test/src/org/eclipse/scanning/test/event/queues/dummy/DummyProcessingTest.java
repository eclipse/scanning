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
package org.eclipse.scanning.test.event.queues.dummy;

import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.test.event.queues.processes.ProcessTestInfrastructure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests ability of DummyProcessor to handle all three Dummy bean types.
 * Also tests generic behaviour of processor in terms of changing its 
 * configuration and possible options for its configuration.
 * 
 * @author Michael Wharmby
 */
public class DummyProcessingTest {
	
	private ProcessTestInfrastructure pti;
	
	@Before
	public void setUp() {
		pti = new ProcessTestInfrastructure();
	}
	
	@After
	public void tearDown() {
		pti = null;
	}
	
	@Test
	public void testDummyAtomExecution() throws Exception {
		DummyAtom dAt = new DummyAtom("Clotho", 400);
		DummyAtomProcess<Queueable> dAtProc = new DummyAtomProcess<>(dAt, pti.getPublisher(), false);
		
		pti.executeProcess(dAtProc, dAt);
		pti.waitForExecutionEnd(10000l);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
	}
	
	@Test
	public void testDummyBeanExecution() throws Exception {
		DummyBean dBe = new DummyBean("Lachesis", 200);
		DummyBeanProcess<Queueable> dBeProc = new DummyBeanProcess<>(dBe, pti.getPublisher(), false);
		
		pti.executeProcess(dBeProc, dBe);
		pti.waitForExecutionEnd(10000L);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
	}
	
	@Test
	public void testDummyHasQueueExecution() throws Exception {
		DummyHasQueue dHQ = new DummyHasQueue("Atropos", 300);
		DummyHasQueueProcess<Queueable> dHQProc = new DummyHasQueueProcess<>(dHQ, pti.getPublisher(), false);
		
		pti.executeProcess(dHQProc, dHQ);
		pti.waitForExecutionEnd(10000L);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
	}
	
	/*
	 * Rest of the test is only for DummyAtoms since the process class is the same
	 */
	@Test
	public void testTermination() throws Exception {
		DummyAtom dAt = new DummyAtom("Hera", 400);
		DummyAtomProcess<Queueable> dAtProc = new DummyAtomProcess<>(dAt, pti.getPublisher(), false);
		
		pti.executeProcess(dAtProc, dAt);
		pti.waitToTerminate(10l);
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.TERMINATED, false);
	}
	
	
	
}
