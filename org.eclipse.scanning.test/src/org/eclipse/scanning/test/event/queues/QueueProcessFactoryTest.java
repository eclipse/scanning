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
package org.eclipse.scanning.test.event.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.IQueueProcess;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtomProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyBeanProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueue;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueueProcess;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.junit.Before;
import org.junit.Test;

public class QueueProcessFactoryTest {
	
	@Before
	public void setUp() {
		QueueProcessFactory.initialize();
	}
	
	@Test
	public void testRegisterProcessor() throws EventException {
		int nRegistered = QueueProcessFactory.getProcessors().size();
		
		//Register a single processor
		QueueProcessFactory.registerProcess(DummyAtomProcess.class);
		assertEquals("No change in number of registered processors!", nRegistered+1, QueueProcessFactory.getProcessors().size());
		
		//Register multiple processors by varargs
		QueueProcessFactory.registerProcesses(DummyBeanProcess.class, DummyHasQueueProcess.class);
		assertEquals("Processors already registered!", nRegistered+3, QueueProcessFactory.getProcessors().size());
	}
	
	@Test
	public void testReturnProcessorForAtom() throws EventException {
		QueueProcessFactory.registerProcesses(DummyAtomProcess.class, DummyBeanProcess.class);
		
		MockPublisher<Queueable> mockPub = new MockPublisher<>(null, null);
		
		DummyAtom dAt = new DummyAtom("Bill", 750);
		IQueueProcess<?,?> processOne = QueueProcessFactory.getProcessor(dAt, mockPub, false);
		if(!(processOne instanceof DummyAtomProcess)) fail("Wrong IProcessor type returned for DummyAtom.");
		
		DummyBean dBe = new DummyBean("Ben", 750);
		IQueueProcess<?,?> processTwo = QueueProcessFactory.getProcessor(dBe, mockPub, false);
		if(!(processTwo instanceof DummyBeanProcess)) fail("Wrong IProcessor type returned for DummyBean.");
		
//		FIXME
//		MoveAtom mvAt = new MoveAtom("Sam1", "Sam1", 3, 20);
//		IQueueProcess<?,?> processThree = QueueProcessFactory.getProcessor(mvAt.getClass().getName());
//		if(!(processThree instanceof MoveAtomProcessor)) fail("Wrong IProcessor type returned for MoveAtom.");
		
		try {
			DummyHasQueue dHQ = new DummyHasQueue("Dilbert", 25);
			@SuppressWarnings("unused")
			IQueueProcess<?,?> processFour = QueueProcessFactory.getProcessor(dHQ, mockPub, false);
			fail("DummyHasQueue not registered, should not be able to get processor!");
		} catch (EventException evEx) {
			//Expected
		}
	}

}
