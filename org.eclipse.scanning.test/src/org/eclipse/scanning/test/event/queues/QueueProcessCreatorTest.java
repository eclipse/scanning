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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueueProcess;
import org.eclipse.scanning.api.event.queues.beans.MoveAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.points.models.IScanPathModel;
//import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.event.queues.QueueProcessCreator;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtomProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyBeanProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueue;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueueProcess;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.eclipse.scanning.test.event.queues.util.TestAtomQueueBeanMaker;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link QueueProcessCreator} class, which selects the process class to 
 * create based on atom/bean type.
 * This test also tests the {@link AllBeanQueueProcessCreator} class, which 
 * also has Dummy types.
 * 
 * @author Michael Wharmby
 *
 */
public class QueueProcessCreatorTest {
	
	private IProcessCreator<Queueable> qpc;
	private IQueueProcess<?, Queueable> qProc;
	
	//Infrastructure
	private IPublisher<Queueable> statPub;
	
	/**
	 * Populate map similar to that in the process creator
	 */
	@Before
	public void setUp() throws Exception {
		
		qpc = new QueueProcessCreator<>(true);
		
		statPub = new MockPublisher<Queueable>(null, "test.topic");
		QueueProcessFactory.initialize();
	}

	/**
	 * Test of the standard registered Process classes
	 * 
	 * Check correct Process is created.
	 * 
	 * @throws Exception
	 */
	@Test //TODO FIXME
	@SuppressWarnings("unchecked")
	public void testProcessTypeCreation() throws Exception {
		List<Queueable> testAtoms = new ArrayList<>();
//		testAtoms.add(new MonitorAtom("Read bpm3", "bpm3", 10000)); FIXME
		testAtoms.add(new MoveAtom("Move robot arm", "robot_arm", "1250", 12000));
		testAtoms.add(makeScanAtom());
//		testAtoms.add(new ProcessAtom(...) TODO
		testAtoms.add(TestAtomQueueBeanMaker.makeDummySubTaskBeanA());
		testAtoms.add(TestAtomQueueBeanMaker.makeDummyTaskBeanA());
		
		for (Queueable atom : testAtoms) {
			qProc = (IQueueProcess<?, Queueable>) qpc.createProcess(atom, statPub);
			assertEquals("Class of test atom & processor do not match", atom.getClass(), qProc.getBeanClass());
			assertEquals("Process has different publisher associated", statPub, qProc.getPublisher());
		}
	}
	
	/**
	 * Test of the Dummy*Process creation only. These are not normally 
	 * registered.
	 * 
	 * Check correct Process is created.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDummyProcessTypeCreation() throws Exception {
		QueueProcessFactory.registerProcesses(DummyAtomProcess.class, DummyBeanProcess.class, DummyHasQueueProcess.class);
		
		List<Queueable> testAtoms = new ArrayList<>();
		testAtoms.add(new DummyAtom("Marvin", 10));
		testAtoms.add(new DummyBean("Doris", 20));
		testAtoms.add(new DummyHasQueue("Delores", 30));
		
		for (Queueable atom : testAtoms) {
			qProc = (IQueueProcess<?,Queueable>) qpc.createProcess(atom, statPub);
			
			assertEquals("Class of test atom & processor do not match", atom.getClass(), qProc.getBeanClass());
			assertEquals("The atom to be processed differs from the input", atom, qProc.getQueueBean());			
			assertEquals("Publisher differs from the expect configuration", statPub, qProc.getPublisher());
		}
	}
	
//	@Test
//	public void testNoRegisteredProcessorForBean() throws Exception {
//		DummyAtom dAt = new DummyAtom("Marvin", 10);
//		
//		try {
//			qProc = (IQueueProcess<Queueable>) qpc.createProcess(dAt, statPub);
//			fail("Should not be able to create QueueProcess for unregistered bean class");
//		} catch (EventException evEx) {
//			//Expected
//		}
//	}
	
	//TODO Check blocking persisted
	//TODO Setting blocking on a per-processor basis.
	
	private ScanAtom makeScanAtom() {
		//ScanAtomProcessor needs a ScanAtom with queue names etc.
		List<IScanPathModel> scanAxes = new ArrayList<>();
		scanAxes.add(new StepModel("ocs", 290, 80, 10));
		scanAxes.add(new StepModel("xMotor", 150, 100, 5));
		
		Map<String, Object> detectors = new HashMap<>();
		detectors.put("pe", new MockDetectorModel(30d));
		
		List<String> monitors = new ArrayList<>();
		monitors.add("bpm3");
		monitors.add("i0");
		
		ScanAtom scAt = new ScanAtom("VT scan across sample", scanAxes, detectors, monitors);

		scAt.setScanBrokerURI("tcp://localhost:8624");
		scAt.setScanSubmitQueueName(IEventService.SUBMISSION_QUEUE);
		scAt.setScanStatusTopicName(IEventService.STATUS_TOPIC);
		return scAt;
	}

}
