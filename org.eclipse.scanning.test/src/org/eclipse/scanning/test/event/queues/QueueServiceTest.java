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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.QueueStatus;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.event.queues.QueueControllerService;
import org.eclipse.scanning.event.queues.QueueService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.mocks.MockConsumer;
import org.eclipse.scanning.test.event.queues.mocks.MockEventService;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueServiceTest {
	
	private MockConsumer<DummyBean> mockCons;
	private MockPublisher<ConsumerCommandBean> mockCmdPub;
	private MockEventService mockEvServ = new MockEventService();
	
	private IQueueService testQServ;
	private IQueueControllerService controllerServ;
	
	private String qRoot;
	private String uri;
	
	@Before
	public void setUp() throws Exception {
		mockCons = new MockConsumer<>();
		mockCmdPub = new MockPublisher<>(null, null);
		mockEvServ.setMockConsumer(mockCons);
		mockEvServ.setMockCmdPublisher(mockCmdPub);
		ServicesHolder.setEventService(mockEvServ);
		
		controllerServ = new QueueControllerService();
		ServicesHolder.setQueueControllerService(controllerServ);
		
		qRoot = "test-queue-root";
		uri = "file:///foo/bar";
	}
	
	@After
	public void tearDown() throws Exception {
		testQServ.disposeService();
		
		ServicesHolder.unsetQueueService(testQServ);
		ServicesHolder.unsetQueueControllerService(controllerServ);
	}
	
	/**
	 * Test initialisation & starting of the service
	 * @throws EventException 
	 */
	@Test
	public void testServiceInit() throws EventException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		ServicesHolder.setQueueService(testQServ);//For QueueControllerService
		controllerServ.init();
		/*
		 * init should:
		 * - check uriString set equal to uri
		 * - check heartbeatTopicName, commandSetName, commandTopicName correct
		 * - check job-queue exists & has name
		 * - not be runnable without qroot & uri
		 * - should initialize the active-queues map
		 */
		assertEquals("Configured uri & uriString of QueueService differ", uri.toString(), testQServ.getURIString());
		assertEquals("uriString & uri of QueueService differ", testQServ.getURIString(), testQServ.getURI().toString());
		assertEquals("Incorrect Heartbeat topic name", qRoot+IQueueService.HEARTBEAT_TOPIC_SUFFIX, testQServ.getHeartbeatTopicName());
		assertEquals("Incorrect Command set name", qRoot+IQueueService.COMMAND_SET_SUFFIX, testQServ.getCommandSetName());
		assertEquals("Incorrect Command topic name", qRoot+IQueueService.COMMAND_TOPIC_SUFFIX, testQServ.getCommandTopicName());
		assertTrue("Active-queue ID set should be an empty set", testQServ.getAllActiveQueueIDs().isEmpty());
		
		//Create an unconfigured QueueService
		testQServ.disposeService();
		testQServ = new QueueService();
		try {
			testQServ.init();
			fail("Should not be able to init without a qRoot or uri set.");
		} catch (IllegalStateException evEx) {
			//Expected
		}
	}
	
	/**
	 * Test clean-up of service
	 * @throws EventException 
	 */
	@Test
	public void testServiceDisposal() throws EventException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		testQServ.start();
		testQServ.stop(false);
		testQServ.disposeService();
		/*
		 * Disposal should:
		 * - call stop (marking inactive)
		 * - dispose job-queue
		 * - render service unstartable (without another init)
		 */
		assertFalse("QueueService is active", testQServ.isActive());
		assertEquals("Job-queue not disposed", null, testQServ.getJobQueue());
		assertEquals("JobQueueID not nullified", null, testQServ.getJobQueueID());
		
		//Test service is in an unstartable state
		try {
			testQServ.start();
			fail("Should not be able to start service immediately after disposal");
		} catch (IllegalStateException ex) {
			//Expected
		}
	}
	
	/**
	 * Testing starting of service
	 * @throws EventException 
	 */
	@Test
	public void testServiceStart() throws EventException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		ServicesHolder.setQueueService(testQServ);//For QueueControllerService
		controllerServ.init();
		testQServ.start();
		/*
		 * Start should:
		 * - start job-queue
		 * - mark service active
		 * - not be callable if init not run
		 */
		assertEquals("Job-queue not started", QueueStatus.STARTED, testQServ.getJobQueue().getStatus());
		assertTrue("QueueService not marked active", testQServ.isActive());	
	}
	
	/**
	 * Test stopping of service
	 * @throws EventException 
	 */
	@Test
	public void testServiceStop() throws EventException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		ServicesHolder.setQueueService(testQServ);//For QueueControllerService
		controllerServ.init();
		testQServ.start();
		testQServ.registerNewActiveQueue();
		testQServ.stop(false);
		/*
		 * Should:
		 * - deregister active-queue(s)
		 * - stop job-queue
		 * - mark service inactive
		 */
		
		//Check graceful stop works
		assertEquals("Active-queues remain after stopping", 0, testQServ.getAllActiveQueueIDs().size());
		assertEquals("Job-queue still active", QueueStatus.STOPPED, testQServ.getJobQueue().getStatus());
		assertFalse("Queue service still marked active", testQServ.isActive());
				
		//Check forceful stop works
		testQServ.start();
		String aqID = testQServ.registerNewActiveQueue();
		
		//Get IDs of consumers in preparation for analysis
		List<UUID> consIDs = new ArrayList<>();
		consIDs.add(testQServ.getActiveQueue(aqID).getConsumerID());
		consIDs.add(testQServ.getJobQueue().getConsumerID());
		
		//And stop the queues
		testQServ.startActiveQueue(aqID);
		testQServ.stop(true);
			
		List<ConsumerCommandBean> cmds = mockCmdPub.getCmdBeans();
		assertTrue("No command beans recorded", cmds.size() > 0);
		assertEquals("Expecting two Killbeans (one for each queue)", 2, cmds.size());
		for (int i = 0; i < 2; i++) {
			ConsumerCommandBean kb = (KillBean)cmds.get(i);
			assertTrue("Command bean "+i+"is not a KillBean", kb instanceof KillBean);
			assertEquals("KillBean has incorrect consumer ID", kb.getConsumerId(), consIDs.get(i));
		}
		
		assertFalse("Queue service still marked active", testQServ.isActive());
	}
	
	/**
	 * Test starting & stopping of a queue
	 * @throws EventException 
	 */
	@Test
	public void testQueueStartStop() throws EventException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		ServicesHolder.setQueueService(testQServ);//For QueueControllerService
		controllerServ.init();
		testQServ.start();
		/*
		 * Should:
		 * - start queue
		 * - stop queue nicely
		 * - start queue
		 * - stop queue forcefully
		 */
		String aqID = testQServ.registerNewActiveQueue();
		IQueue<QueueAtom> activeQ = testQServ.getActiveQueue(aqID);
		assertEquals("Active-queue not initialised", QueueStatus.INITIALISED, activeQ.getStatus());
		
		//Start queue & check it looks started
		testQServ.startActiveQueue(aqID);
		assertEquals("Active-queue not started", QueueStatus.STARTED, activeQ.getStatus());
		
		//Stop queue nicely & check it looks started
		testQServ.stopActiveQueue(aqID, false);
		assertEquals("Active-queue has wrong state", QueueStatus.STOPPED, activeQ.getStatus());
		
//TODO		
		//Restart queue & stop is forcefully
		testQServ.startActiveQueue(aqID);
		testQServ.stopActiveQueue(aqID, true);
		List<ConsumerCommandBean> cmds = mockCmdPub.getCmdBeans();
		assertTrue("No command beans recorded", cmds.size() > 0);
		assertTrue("Last command bean is not a KillBean", cmds.get(cmds.size()-1) instanceof KillBean);
		assertEquals("KillBean not killing the active-queue consumer", testQServ.getActiveQueue(aqID).getConsumerID(), cmds.get(cmds.size()-1).getConsumerId());
	}
	
	/**
	 * Test registration & deregistration of active-queues
	 * @throws EventException 
	 */
	@Test
	public void testRegistration() throws EventException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		ServicesHolder.setQueueService(testQServ);//For QueueControllerService
		controllerServ.init();
		testQServ.start();
		/*
		 * Should:
		 * - register active-queue (not possible without queue service start)
		 * - register 5 active-queues (test names all different)
		 * - start 1 queue, deregister all
		 * - force deregister remaining queue
		 */
		//Register active queues
		int i = 0;
		while (i < 5) {
			testQServ.registerNewActiveQueue();
			i++;
		}
		//Check names different & related to queueRoot
		List<String> activeQIDs = new ArrayList<>(testQServ.getAllActiveQueueIDs());
		assertTrue("Not enough active-queues registered!", activeQIDs.size() > 4);
		for (i = 0; i < activeQIDs.size(); i++) {
			String[] idParts = activeQIDs.get(i).split("\\.");
			assertEquals("ID should be in three parts", 3, idParts.length);
			assertEquals("First part of active-queue ID should be queueRoot", qRoot, idParts[0]);
			assertTrue("Middle part of active-queue ID should be of the form \"aq-1-111\" (was: "+idParts[1]+")", idParts[1].matches("aq-\\d-\\d\\d\\d"));
			assertEquals("Third part of active-queue ID should be suffix", "active-queue", idParts[2]);
			for (int j = i+1; j < activeQIDs.size(); j++) {
				assertFalse("Two active queues with the same name", activeQIDs.get(i).equals(activeQIDs.get(j)));
			}
		}
		
		//Check deregistration works
		for (i = 0; i < activeQIDs.size()-1; i++) {
			assertTrue("Active-queue "+activeQIDs.get(i)+" should be registered", testQServ.isActiveQueueRegistered(activeQIDs.get(i)));
			testQServ.deRegisterActiveQueue(activeQIDs.get(i));
			assertFalse("Active-queue "+activeQIDs.get(i)+" should not be registered", testQServ.isActiveQueueRegistered(activeQIDs.get(i)));
			try {
				testQServ.getQueue(activeQIDs.get(i));
				fail("Queue should no longer exist in registry");
			} catch (EventException evEx) {
				//Expected
			}
		}
		activeQIDs = new ArrayList<>(testQServ.getAllActiveQueueIDs());
		assertEquals("Should only be one queue left in registry", 1, activeQIDs.size());

		//Check we can't deregister running queues
		testQServ.startActiveQueue(activeQIDs.get(0));
		try {
			testQServ.deRegisterActiveQueue(activeQIDs.get(0));
			fail("Should not be able to deregister a running active-queue");
		} catch (EventException evEx) {
			//Expected
		}
		
		//Check queue registration not possible without start
		testQServ.disposeService();
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		try {
			testQServ.registerNewActiveQueue();
			fail("QueueService should be started before active queue can be registered");
		} catch (IllegalStateException isEx) {
			//Expected
		}
	}
	
	/**
	 * Test changing config 
	 * @throws URISyntaxException 
	 */
	@Test
	public void testConfigChange() throws EventException, URISyntaxException {
		testQServ = new QueueService(qRoot, uri);
		testQServ.init();
		testQServ.start();
		
		/*
		 * Should check not possible whilst active:
		 * - changing of URI by URI or String
		 * - changing qRoot
		 * 
		 * Should check
		 * - changing qRoot changes Command/Heartbeat destinations
		 * - creates new job-queue
		 */
		try {
			testQServ.setQueueRoot("new-test-queue-root");
			fail("Should not be able to change queueRoot whilst active");
		} catch (UnsupportedOperationException evEx) {
			//Expected
		}
		try {
			testQServ.setUri("file:///foo/bar/baz");
			fail("Should not be able to change uri whilst active");
		} catch (UnsupportedOperationException evEx) {
			//Expected
		}
		try {
			testQServ.setUri(new URI("file:///foo/bar/baz"));
			fail("Should not be able to change uri whilst active");
		} catch (UnsupportedOperationException evEx) {
			//Expected
		}
		
		try {
			testQServ.stop(false);
			
			//Test changing qRoot
			testQServ.setQueueRoot("new-test-queue-root");
			assertEquals("Incorrect Heartbeat topic name", "new-test-queue-root"+IQueueService.HEARTBEAT_TOPIC_SUFFIX, testQServ.getHeartbeatTopicName());
			assertEquals("Incorrect Command set name", "new-test-queue-root"+IQueueService.COMMAND_SET_SUFFIX, testQServ.getCommandSetName());
			assertEquals("Incorrect Command topic name", "new-test-queue-root"+IQueueService.COMMAND_TOPIC_SUFFIX, testQServ.getCommandTopicName());
			assertEquals("Incorrect jobQueueID", "new-test-queue-root"+IQueueService.JOB_QUEUE_SUFFIX, testQServ.getJobQueueID());
			IQueue<QueueBean> jobQueue = testQServ.getJobQueue();
			assertEquals("Job queue name in service & on job-queue do not match", testQServ.getJobQueueID(), jobQueue.getQueueID());
			
			//Test changing URI
			testQServ.setUri("file:///foo/bar/baz");
			assertEquals("Unexpected URI after changing by string", new URI("file:///foo/bar/baz"), testQServ.getURI());
			jobQueue = testQServ.getJobQueue();
			assertEquals("Incorrect URI configured in job-queue", "file:///foo/bar/baz", jobQueue.getURI().toString());
			testQServ.setUri(new URI("file:///baz/foo/bar"));
			assertEquals("Unexpected uriString after changing by URI", "file:///baz/foo/bar", testQServ.getURIString());
			jobQueue = testQServ.getJobQueue();
			assertEquals("Incorrect URI configured in job-queue", "file:///baz/foo/bar", jobQueue.getURI().toString());
			
		} catch (EventException evEx) {
			//This is a error, we have to handle since we're checking for errors above 
			evEx.printStackTrace();
			return;
		}
		
	}

}
