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
package org.eclipse.scanning.test.event.queues.processes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueControllerService;
import org.eclipse.scanning.event.queues.QueueService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.processes.QueueProcess;
import org.eclipse.scanning.event.queues.processes.TaskBeanProcess;
import org.eclipse.scanning.test.event.queues.mocks.MockConsumer;
import org.eclipse.scanning.test.event.queues.mocks.MockEventService;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.eclipse.scanning.test.event.queues.mocks.MockSubmitter;
import org.eclipse.scanning.test.event.queues.util.TestAtomQueueBeanMaker;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TaskBeanProcessTest {
	
	private TaskBean tBe;
	private QueueProcess<TaskBean, Queueable> tBeProc;
	private ProcessTestInfrastructure pti;
	
	private static QueueService qServ;
	private static MockConsumer<Queueable> mockCons;
	private static MockPublisher<QueueAtom> mockPub;
	private static MockPublisher<ConsumerCommandBean> mockCmdPub;
	private static MockSubmitter<QueueAtom> mockSub;
	private static MockEventService mockEvServ;
	private static IQueueControllerService controller;
	
	@BeforeClass
	public static void setUpClass() throws EventException {
		//Configure the processor Mock queue infrastructure
		mockCons = new MockConsumer<>();
		mockPub = new MockPublisher<>(null, null);
		mockCmdPub = new MockPublisher<>(null, null);
		mockSub = new MockSubmitter<>();
		mockSub.setSendToConsumer(true);
		mockEvServ = new MockEventService();
		mockEvServ.setMockConsumer(mockCons);
		mockEvServ.setMockPublisher(mockPub);
		mockEvServ.setMockCmdPublisher(mockCmdPub);
		mockEvServ.setMockSubmitter(mockSub);
		ServicesHolder.setEventService(mockEvServ);
		
		//This is a real queue service, so we have to do some set up
		qServ = new QueueService("fake-qserv", "file:///foo/bar");
		qServ.init();
		qServ.start();
		
		ServicesHolder.setQueueService(qServ);
		
		//Once this lot is up, create a queue controller.
		controller = new QueueControllerService();
		controller.init();
		ServicesHolder.setQueueControllerService(controller);
	}
	
	@AfterClass
	public static void tearDownClass() {
		ServicesHolder.unsetQueueControllerService(controller);
		controller = null;
		
		ServicesHolder.unsetEventService(mockEvServ);
		mockEvServ = null;
		mockPub = null;
		
		ServicesHolder.unsetQueueService(qServ);
		qServ = null;
		mockSub = null;
	}
	
	@Before
	public void setUp() throws EventException {
		pti = new ProcessTestInfrastructure();
		
		//Create test atom & process
		tBe = new TaskBean("Test queue sub task bean");
		tBe.setBeamline("I15-1(test)");
		tBe.setHostName("afakeserver.diamond.ac.uk");
		tBe.setUserName(System.getProperty("user.name"));
		SubTaskAtom atomA = TestAtomQueueBeanMaker.makeDummySubTaskBeanA();
		SubTaskAtom atomB = TestAtomQueueBeanMaker.makeDummySubTaskBeanB();
		SubTaskAtom atomC = TestAtomQueueBeanMaker.makeDummySubTaskBeanC();
		tBe.addAtom(atomA);
		tBe.addAtom(atomB);
		tBe.addAtom(atomC);
		
		tBeProc = new TaskBeanProcess<>(tBe, pti.getPublisher(), false);
		
		//Reset queue architecture
		mockSub.resetSubmitter();
		mockPub.resetPublisher();
	}
	
	@After
	public void tearDown() {
		pti = null;
		mockEvServ.clearRegisteredConsumers();
	}
	
	/**
	 * After execution:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should be Status.COMPLETE and 100%
	 * - status publisher should have: 1 RUNNING bean and 1 COMPLETE bean
	 * - child active-queue should be deregistered from QueueService
	 * 
	 * N.B. This is *NOT* an integration test, so beans don't get run.
	 *      It only checks the processor behaves as expected
	 */
	@Test
	public void testExecution() throws Exception {
		pti.executeProcess(tBeProc, tBe, true);
		pti.waitForExecutionEnd(10000l);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
		
		//These are the statuses & percent completes reported by the processor as it sets up the run
		Status[] reportedStatuses = new Status[]{Status.RUNNING, Status.RUNNING,
				Status.RUNNING, Status.RUNNING};
		Double[] reportedPercent = new Double[]{0d, 1d, 
				4d, 5d};
		
		pti.checkFirstBroadcastBeanStatuses(reportedStatuses, reportedPercent);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, true);
		
		pti.checkSubmittedBeans(mockSub, ((TaskBeanProcess<Queueable>) tBeProc).getAtomQueueProcessor().getActiveQueueID());

		//Child queue should be removed after execution
		assertEquals("Active queues still registered after terminate", 0, qServ.getAllActiveQueueIDs().size());
	}
	
	/**
	 * On terminate:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should Status.TERMINATED and not be 100% complete
	 * - status publisher should have a TERMINATED bean
	 * - termination message should be set on the bean
	 * - child queue infrastructure should have received a stop message
	 * - child active-queue should be deregistered from QueueService
	 */
	@Test
	public void testTermination() throws Exception {
		pti.executeProcess(tBeProc, tBe);
		pti.waitToTerminate(100l, true);
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.TERMINATED, false);
		
		//TODO Should this be the message or the queue-message?
		assertEquals("Wrong message set after termination.", "Job-queue aborted before completion (requested)", pti.getLastBroadcastBean().getMessage());
		
		pti.checkConsumersStopped(mockEvServ, qServ);
		
		//Termination should remove the child queue
		assertEquals("Active queues still registered after terminate", 0, qServ.getAllActiveQueueIDs().size());
	}
	
//	@Test
	public void testPauseResume() throws Exception {
		//TODO!
	}
	
	/**
	 * On failure:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should Status.FAILED and not be 100% complete
	 * - message with details of failure should be set on bean
	 * - the consumer we're running the TaskBean on should have received a 
	 *   REQUEST_PAUSE command
	 * - child active-queue should be deregistered from QueueService
	 */
	@Test
	public void testChildFailure() throws Exception {
		pti.executeProcess(tBeProc, tBe);
		//Set some arbitrary percent complete
		tBeProc.broadcast(Status.RUNNING, 20d);
		tBeProc.getProcessLatch().countDown();
		//Need to give the post-match analysis time to run
		Thread.sleep(10);
		
		/*
		 * FAILED is always going to happen underneath- i.e. process will be 
		 * running & suddenly latch will be counted down.
		 * 
		 * QueueListener sets the message and queueMessage.
		 * We need to set this bean's status to FAILED and pause the consumer 
		 * to stop running any more beans until the user is happy.
		 */
		pti.checkLastBroadcastBeanStatuses(Status.FAILED, false);
		
		//Check we sent a pause instruction to the job-queue consumer
		List<ConsumerCommandBean> cmdBeans = mockCmdPub.getCmdBeans();
		long timeout = 1000;
		while (cmdBeans.size() < 1) {
			//Sit here waiting until a cmd bean lands...
			Thread.sleep(50);
			timeout = timeout-50;
			if (timeout == 0) fail("No cmd bean's heard before timeout");
		}
		///...then check it's the right one.
		if (cmdBeans.get(cmdBeans.size()-1) instanceof PauseBean) {
			PauseBean lastBean = (PauseBean)cmdBeans.get(cmdBeans.size()-1);
			assertEquals("PauseBean does not pause the job-queue consumer", mockCons.getConsumerId(), lastBean.getConsumerId());
		} else {
			fail("Last published bean was not a PauseBean");
		}
		
		//After fail child queue should be deregistered
		assertEquals("Active queues still registered after terminate", 0, qServ.getAllActiveQueueIDs().size());
	}

}
