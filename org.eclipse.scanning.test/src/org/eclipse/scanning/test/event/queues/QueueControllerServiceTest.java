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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.mocks.MockConsumer;
import org.eclipse.scanning.test.event.queues.mocks.MockEventService;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.eclipse.scanning.test.event.queues.mocks.MockSubmitter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueControllerServiceTest {
	
	private String qRoot;
	private String uri;
	
	private MockPublisher<ConsumerCommandBean> mockCmdPub;
	private MockPublisher<Queueable> mockPub;
	private MockSubmitter<Queueable> mockSub;
	private MockEventService mockEvServ;
	private String jqID, jqSubmQ, aqID, aqSubmQ;
	
	private IQueueControllerService testController; 
	
	@Before
	public void setUp() throws EventException {
		//Configure the MockEventService
		mockEvServ = new MockEventService();
		mockEvServ.setConsumerNoFill(true);
		mockPub = new MockPublisher<>(null, null);
		mockEvServ.setMockPublisher(mockPub);
		mockCmdPub = new MockPublisher<>(null, null);
		mockEvServ.setMockCmdPublisher(mockCmdPub);
		mockSub = new MockSubmitter<>();
		mockEvServ.setMockSubmitter(mockSub);
		ServicesHolder.setEventService(mockEvServ);
		
		//A bit of boilerplate to start the service under test
		qRoot = "test-queue-root";
		uri = "file:///foo/bar";
		testController = new QueueService(qRoot, uri);
		ServicesHolder.setQueueService((IQueueService) testController);
		testController.init();
		testController.startQueueService();
		
		//Get our queue names
		jqID = testController.getJobQueueID();
		jqSubmQ = testController.getJobQueue().getSubmissionQueueName();
		aqID = ((IQueueService)testController).registerNewActiveQueue();
		aqSubmQ = testController.getQueue(aqID).getSubmissionQueueName();
	}
	
	@After
	public void tearDown() throws EventException {
		//Clear destination names
		qRoot = null;
		uri = null;
		jqID = null;
		jqSubmQ = null;
		aqID = null;
		aqSubmQ = null;
		
		testController.stopQueueService(true);
		testController = null;
		
		ServicesHolder.setEventService(null);
		
		//Set mock services null
		mockPub = null;
		mockCmdPub = null;
		mockSub = null;
		mockEvServ = null;
	}
	
	/**
	 * Test submission and removal of beans in queues. 
	 * @throws Exception 
	 */
	@Test
	public void testSubmitRemove() throws Exception {
		//Beans for submission
		DummyBean albert = new DummyBean("Albert", 10),bernard = new DummyBean("Bernard", 20);
		DummyAtom carlos = new DummyAtom("Carlos", 30), duncan = new DummyAtom("Duncan", 40);
		DummyAtom xavier = new DummyAtom("Xavier", 100);
		
		/*
		 * Submit:
		 * - new DummyBean in job-queue with a particular name
		 * - 2nd and we have two in submit queue (second has particular name)
		 * - two DummyBeans in active-queue with particular names 
		 */
		//job-queue
		testController.submit(albert, jqID);
		assertEquals("No beans in job-queue after 1 submission", 1, mockSub.getQueueSize(jqSubmQ));
		assertEquals("Bean has wrong name", "Albert", mockSub.getLastSubmitted(jqSubmQ).getName());
		
		testController.submit(bernard, jqID);
		assertEquals("Bean has wrong name", "Bernard", mockSub.getLastSubmitted(jqSubmQ).getName());
		
		//active-queue
		testController.submit(carlos, aqID);
		assertEquals("No beans in active-queue after 1 submission", 1, mockSub.getQueueSize(aqSubmQ));
		assertEquals("Bean has wrong name", "Carlos", mockSub.getLastSubmitted(aqSubmQ).getName());

		testController.submit(duncan, aqID);//Needed for remove test...
		
		/*
		 * Remove:
		 * - now only one bean left and it job-queue (1st)
		 * - now only one bean left in active-queue (2nd)
		 * - throw an exception if the removed bean is no-longer removeable 
		 *   (or not in the queue in the first place)
		 */
		//job-queue
		testController.remove(bernard, jqID);
		assertEquals("Should only be one bean left in active-queue", 1, mockSub.getQueueSize(jqSubmQ));
		assertEquals("Wrong bean found in queue", "Albert", mockSub.getLastSubmitted(jqSubmQ).getName());
		
		//active-queue
		testController.remove(carlos, aqID);
		assertEquals("Should only be one bean left in active-queue", 1, mockSub.getQueueSize(aqSubmQ));
		assertEquals("Wrong bean found in queue", "Duncan", mockSub.getLastSubmitted(aqSubmQ).getName());
		try {
			testController.remove(carlos, aqID);
			fail("Expected EventException: Carlos has already been removed");
		} catch (EventException evEx) {
			System.out.println("^---- Expected exception");
		}
		
		/*
		 * Prevent submission/removal of wrong bean type to wrong queue
		 */
		try {
			testController.submit(carlos, jqID);
			fail("Expected EventException when wrong queueable type submitted (atom in job-queue)");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.remove(bernard, aqID);
			fail("Expected EventException when wrong queueable type submitted (bean in atom-queue)");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.remove(xavier, aqID);
			fail("Expected EventException: Xavier was never submitted = can't be removed");
		} catch (EventException evEx) {
			System.out.println("^---- Expected exception");
		}
	}
	
	/**
	 * Tests request of reordering processes in a queue
	 * @throws EventException
	 */
	@Test
	public void testReorder() throws EventException {
		//Beans for submission
		DummyBean albert = new DummyBean("Albert", 10);
		DummyAtom carlos = new DummyAtom("Carlos", 30);
		DummyAtom xavier = new DummyAtom("Xavier", 100);
		
		//Add two beans to the queues
		testController.submit(albert, jqID);
		testController.submit(carlos, aqID);
		/*
		 * Submit beans & reorder
		 * - check number of moves correct for given bean
		 */
		assertFalse("Albert indicated reordered, but no reordering done", mockSub.isBeanReordered(albert));
		assertFalse("Carlos indicated reordered, but no reordering done", mockSub.isBeanReordered(carlos));
		
		testController.reorder(albert, 4, jqID);
		assertTrue("Albert not reordered after reordering done", mockSub.isBeanReordered(albert));
		assertEquals("Incorrect number of moves after reordering", 4, mockSub.getReorderedBeanMove(albert));
		assertFalse("Carlos indicated reordered, but no reordering done", mockSub.isBeanReordered(carlos));
		
		testController.reorder(carlos, 3, aqID);
		assertTrue("Carlos not reordered after reordering done", mockSub.isBeanReordered(carlos));
		assertEquals("Incorrect number of moves after reordering", 3, mockSub.getReorderedBeanMove(carlos));
		
		/*
		 * Check EventException thrown when bean wrong type or not present.
		 */
		try {
			testController.reorder(carlos, 2, jqID);
			fail("Expected EventException when wrong queueable type reordered (atom in job-queue)");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.reorder(xavier, 3, aqID);
			fail("Expected EventException: Xavier not submitted, should not be able to reorder");
		} catch (EventException evEx) {
			System.out.println("^---- Expected exception");
		}
	}
	
	/**
	 * Tests requests for pausing/resumption of processes within a queue.
	 * @throws EventException
	 */
	@Test
	public void testPauseResume() throws EventException {
		//Beans for submission
		DummyBean albert = new DummyBean("Albert", 10);
		DummyAtom carlos = new DummyAtom("Carlos", 30);
		DummyAtom xavier = new DummyAtom("Xavier", 100);
		
		//Set up beans in right queues
		setUpTwoBeanStatusSet(albert, carlos);
		
		/*
		 * Pause process in job-queue
		 * - check published bean has REQUEST_PAUSE status
		 * - check EventException thrown when bean with isPaused status is paused   
		 */
		testController.pause(albert, jqID);
		assertEquals("Published bean has wrong Status", Status.REQUEST_PAUSE, mockPub.getLastQueueable().getStatus());
		
		//Try pausing the bean again (should throw a runtime exception)
		try {
			testController.pause(albert, jqID);
			fail("Expected IllegalStateException on repeated pause");
		} catch (IllegalStateException isEx) {
			System.out.println("^---- Expected exception");
		}
		
		/*
		 * Resume process in job-queue
		 * - check published bean has REQUEST_RESUME status
		 * - check EventException thrown when bean with isResumed/isRunning status is resumed
		 */
		testController.resume(albert, jqID);
		assertEquals("Published bean has wrong Status", Status.REQUEST_RESUME, mockPub.getLastQueueable().getStatus());
		
		//Try resuming the bean again (should throw a runtime exception)
		try {
			testController.resume(albert, jqID);
			fail("Expected IllegalStateException on repeated resume");
		} catch (IllegalStateException isEx) {
			System.out.println("^---- Expected exception");
		}
		
		/*
		 * We'll assume that everything works fine for the active-queue, since
		 * the same code calls the terminate requests and that is tested.
		 */
		
		/*
		 * Test wrong bean type to wrong queue & non-existent paused/resumed bean
		 */
		try {
			testController.pause(carlos, jqID);
			fail("Expected EventException when wrong queueable type paused (atom in job-queue)");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.resume(albert, aqID);
			fail("Expected EventException when wrong queueable type resumed (bean in active-queue)");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.pause(xavier, aqID);
			fail("Expected EventException: Xavier not submitted, should not be able to pause");
		} catch (EventException evEx) {
			System.out.println("^---- Expected exception");
		}
		//Testing bean type test comes before bean exists/state test fails
		try {
			testController.resume(xavier, jqID);
			fail("Expected EventException: Xavier not right bean type, should not be able to resume");
		} catch (EventException evEx) {
			System.out.println("^---- Expected exception");
			assertTrue("Got unexpected exception - should be an EventException containing message '", 
					(evEx.getMessage().contains("Bean type") && evEx.getMessage().contains("not supported by queue")));
		}
	}
	
	/**
	 * Tests requesting termination of processes within a queue.
	 * @throws EventException
	 */
	@Test
	public void testTerminate() throws EventException {
		//Beans for submission
		DummyBean albert = new DummyBean("Albert", 10);
		DummyAtom carlos = new DummyAtom("Carlos", 30);
		DummyAtom xavier = new DummyAtom("Xavier", 100);
		
		//Set up beans in right queues
		setUpTwoBeanStatusSet(albert, carlos);
		
		/*
		 * Pause process in job-queue
		 * - check published bean has REQUEST_PAUSE status
		 * - check EventException thrown when bean with isPaused status is paused   
		 */
		testController.terminate(albert, jqID);
		assertEquals("Published bean has wrong Status", Status.REQUEST_TERMINATE, mockPub.getLastQueueable().getStatus());
		try {
			testController.terminate(albert, jqID);
			fail("Expected IllegalStateException on repeated terminate");
		} catch (IllegalStateException isEx) {
			System.out.println("^---- Expected exception");
		}

		/*
		 * Terminate process in active-queue
		 * - check published bean has REQUEST_PAUSE status 
		 */
		testController.terminate(carlos, aqID);
		assertEquals("Published bean has wrong Status", Status.REQUEST_TERMINATE, mockPub.getLastQueueable().getStatus());

		/*
		 * Test wrong bean type to wrong queue & non-existent terminated bean
		 */
		try {
			testController.terminate(carlos, jqID);
			fail("Expected EventException when wrong queueable type terminated (atom in job-queue)");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.terminate(xavier, aqID);
			fail("Expected EventException: Xavier not submitted, should not be able to terminate");
		} catch (EventException iaEx) {
			System.out.println("^---- Expected exception");
		}
		try {
			testController.terminate(xavier, jqID);
			fail("Expected EventException: Xavier not right bean type, should not be able to terminate");
		} catch (EventException evEx) {
			System.out.println("^---- Expected exception");
			assertTrue("Got unexpected exception - should be an EventException containing message '", 
					(evEx.getMessage().contains("Bean type") && evEx.getMessage().contains("not supported by queue")));
		}		
	}
	
	private void setUpTwoBeanStatusSet(QueueBean albert, QueueAtom carlos) throws EventException {		
		//Set up beans in right queues
		MockConsumer<QueueBean> jCons = (MockConsumer<QueueBean>) testController.getJobQueue().getConsumer();
		jCons.addToStatusSet(albert);
		@SuppressWarnings("unchecked")
		MockConsumer<QueueAtom> aCons = (MockConsumer<QueueAtom>) testController.getQueue(aqID).getConsumer();
		aCons.addToStatusSet(carlos);
	}
	
	/**
	 * Test configuration & publishing of PauseBeans to pause/resume whole queues.
	 * @throws EventException
	 */
	@Test
	public void testPauseResumeQueue() throws EventException {
		/*
		 * Pause the job-queue
		 * - check PauseBean sent
		 * - check has correct consumerID
		 * Unpause the job-queue
		 * - check PauseBean sent
		 * - check has correct consumerID
		 */
		testController.pauseQueue(jqID);
		analysePauser(jqID, true);
		testController.resumeQueue(jqID);
		analysePauser(jqID, false);
		
		/*
		 * Pause the active-queue
		 * - check PauseBean sent
		 * - check has correct consumerID
		 * Unpause the active-queue
		 * - check PauseBean sent
		 * - check has correct consumerID
		 */
		testController.pauseQueue(aqID);
		analysePauser(aqID, true);
		testController.resumeQueue(aqID);
		analysePauser(aqID, false);
	}
	
	private void analysePauser(String queueID, boolean pause) throws EventException {
		if (mockCmdPub.getLastCmdBean() instanceof PauseBean) {
			PauseBean pauser = (PauseBean)mockCmdPub.getLastCmdBean();
			assertEquals("Kill bean has wrong consumer ID", testController.getQueue(queueID).getConsumerID(), pauser.getConsumerId());
			assertEquals("Wrong boolean for pause field", pause, pauser.isPause());
		} else {
			fail("Last command bean was not a PauseBean.");
		}
	}
	
	/**
	 * Test configuration & publishing of KillBeans to kill queues.
	 * @throws EventException
	 */
	@Test	
	public void testKillQueue() throws EventException {
		/*
		 * Kill the job-queue
		 * - check KillBean sent
		 * - check KillBean has correct consumerID
		 * - check options passed through correctly
		 */
		testController.killQueue(jqID, true, false, false);
		analyseKiller(jqID, true, false);
		
		/*
		 * Kill active-queue
		 * - check KillBean sent
		 * - check KillBean has correct consumerID
		 * - check options passed through correctly
		 */
		testController.killQueue(aqID, false, false, true);
		analyseKiller(aqID, false, true);
	}
	
	/**
	 * Checks KillBean configured as expected
	 * @param queueID
	 * @throws EventException 
	 */
	private void analyseKiller(String queueID, boolean disconnect, boolean exitProc) throws EventException {
		if (mockCmdPub.getLastCmdBean() instanceof KillBean) {
			KillBean killer = (KillBean)mockCmdPub.getLastCmdBean();
			assertEquals("Kill bean has wrong consumer ID", testController.getQueue(queueID).getConsumerID(), killer.getConsumerId());
			assertEquals("Disconnect should be true (was false)", disconnect, killer.isDisconnect());
			assertEquals("ExitProcess should be false (was true)", exitProc, killer.isExitProcess());
		} else {
			fail("Last command bean was not a KillBean.");
		}
	}

}
