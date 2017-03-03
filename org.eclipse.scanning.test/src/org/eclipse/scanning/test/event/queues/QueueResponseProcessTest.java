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
import org.eclipse.scanning.api.event.core.IResponseCreator;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.remote.QueueRequest;
import org.eclipse.scanning.api.event.queues.remote.QueueRequestType;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueControllerService;
import org.eclipse.scanning.event.queues.QueueService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.remote.QueueResponseCreator;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.mocks.MockConsumer;
import org.eclipse.scanning.test.event.queues.mocks.MockEventService;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueResponseProcessTest {
	
	private DummyBean submDummy, statDummy;
	private MockPublisher<QueueRequest> mockPub;
	private MockConsumer<Queueable> mockCons = new MockConsumer<>();
	private MockEventService mockEvServ;
	
	private IQueueService qServ;
	private IQueueControllerService qControl;
	
	private QueueRequest qReq, qAns;
	private IResponseCreator<QueueRequest> qResponseCreator;
	private IResponseProcess<QueueRequest> responseProc;
	
	@Before
	public void setUp() throws EventException {
		//Make sure we have clear queues before we start
		mockCons.clearQueue(mockCons.getStatusSetName());
		mockCons.clearQueue(mockCons.getSubmitQueueName());
		
		//A bean to interrogate
		submDummy = new DummyBean(); //Should have a uID & Status.NONE
		mockCons.addToSubmitQueue(submDummy);
		statDummy = new DummyBean();
		mockCons.addToStatusSet(statDummy);
		
		//Set up all the underlying queue service infrastructure
		mockPub = new MockPublisher<>(null, null);
		mockEvServ = new MockEventService();
		mockEvServ.setMockConsumer(mockCons);
		ServicesHolder.setEventService(mockEvServ);
		
		//This is the REAL queue service, because the Mock is too complex
		qServ = new QueueService();
		qServ.setQueueRoot("fake-q-root");
		qServ.setUri("file:///foo/bar");
		qServ.init();
		ServicesHolder.setQueueService(qServ);
		qControl = new QueueControllerService();
		qControl.init();
		ServicesHolder.setQueueControllerService(qControl);
		
		//Create the QueueResponseProcess creator
		qResponseCreator = new QueueResponseCreator();
	}
	
	@After
	public void tearDown() {
		qReq = null;
		qAns = null;
	}
	
	@Test
	public void testResponseGetStringConfig() throws EventException {
		//Expected values
		String realCommandSetName = "fake-q-root"+IQueueService.COMMAND_SET_SUFFIX;
		String realCommandTopicName = "fake-q-root"+IQueueService.COMMAND_TOPIC_SUFFIX;
		String realHeartbeatTopicName = "fake-q-root"+IQueueService.HEARTBEAT_TOPIC_SUFFIX;
		String realJobQueueID = "fake-q-root"+IQueueService.JOB_QUEUE_SUFFIX;
		
		/*
		 * Get command set
		 */
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.COMMAND_SET);
		
		//Create the response & process the request; check answer is correct
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		assertEquals("Response command set is incorrect", realCommandSetName, qAns.getCommandSetName());
		
		/*
		 * Get command topic
		 */
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.COMMAND_TOPIC);
		
		//Create the response & process the request; check answer is correct
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		assertEquals("Response command topic is incorrect", realCommandTopicName, qAns.getCommandTopicName());
		
		/*
		 * Get heartbeat topic
		 */
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.HEARTBEAT_TOPIC);
		
		//Create the response & process the request; check answer is correct
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		assertEquals("Response heartbeat topic is incorrect", realHeartbeatTopicName, qAns.getHeartbeatTopicName());
		
		/*
		 * Get job-queue ID
		 */
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.JOB_QUEUE_ID);
		
		//Create the response & process the request; check answer is correct
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		assertEquals("Response job-queue ID is incorrect", realJobQueueID, qAns.getJobQueueID());
	}
	
	@Test
	public void testResponseGetBeanStatus() throws EventException {
		/*
		 * Beans are already in the job-queue.
		 */
		submDummy.setStatus(Status.SUBMITTED);
		statDummy.setStatus(Status.RUNNING);
		
		/*
		 * Try getting a bean which is in the submit queue
		 */
		//Create bean status request & post
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.BEAN_STATUS);
		qReq.setQueueID("fake-q-root"+IQueueService.JOB_QUEUE_SUFFIX);
		qReq.setBeanID(submDummy.getUniqueId());
		
		//Create the response & process the request
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
	
		//Check response from server
		assertEquals("Response has wrong bean status", Status.SUBMITTED, qAns.getBeanStatus());
		
		/*
		 * Same as above, but get a bean which is in the status queue
		 */
		//Create bean status request & post
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.BEAN_STATUS);
		qReq.setQueueID("fake-q-root"+IQueueService.JOB_QUEUE_SUFFIX);
		qReq.setBeanID(statDummy.getUniqueId());
		
		//Create the response & process the request
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);

		//Check response from server
		assertEquals("Response has wrong bean status", Status.RUNNING, qAns.getBeanStatus());
		
		/*
		 * Same as above, but with a non-existent bean
		 */
		try {
			DummyBean bilbo = new DummyBean();
			qReq = new QueueRequest();
			qReq.setRequestType(QueueRequestType.BEAN_STATUS);
			qReq.setQueueID("fake-q-root"+IQueueService.JOB_QUEUE_SUFFIX);
			qReq.setBeanID(bilbo.getUniqueId());
			
			//Create the response & process the request
			responseProc = qResponseCreator.createResponder(qReq, mockPub);
			qAns = responseProc.process(qReq);
			
			fail("Bean 'bilbo' shouldn't be findable in the consumer");
		} catch (EventException evEx) {
			//Expected
		}
	}
	
	@Test
	public void testResponseQueueServiceStartStop() throws EventException {
		//Initially QueueService should not be running
		assertFalse("QueueService should not initially be active", qServ.isActive());
		
		//Create a request to start the queue service...
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.SERVICE_START_STOP);
		qReq.setStartQueueService(true);
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		
		//...has it started?
		assertTrue("QueueService should be active", qServ.isActive());
		assertEquals("Request & answer should not have changed", qAns, qReq);
		
		//Create a request to restart the queue service...
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.SERVICE_START_STOP);
		qReq.setStartQueueService(true);
		qReq.setStopQueueService(true);
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		
		//...is it still started?
		assertTrue("QueueService should be active", qServ.isActive());
		assertEquals("Request & answer should not have changed", qAns, qReq);
		
		//Create a request to stop the queue service...
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.SERVICE_START_STOP);
		qReq.setStopQueueService(true);
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);
		
		//...has it stopped?
		assertFalse("QueueService should not be active", qServ.isActive());
		assertEquals("Request & answer should not have changed", qAns, qReq);
	}
	
	@Test
	public void testResponseGetQueue() throws EventException {
		//Create bean status request & post
		qReq = new QueueRequest();
		qReq.setRequestType(QueueRequestType.QUEUE);
		qReq.setQueueID("fake-q-root"+IQueueService.JOB_QUEUE_SUFFIX);

		//Create the response & process the request
		responseProc = qResponseCreator.createResponder(qReq, mockPub);
		qAns = responseProc.process(qReq);

		//Get the job-queue & compare it's config
		IQueue<QueueBean> jobQueue = qServ.getJobQueue();
		IQueue<? extends Queueable> remoteJobQueue = qControl.getQueue(qAns.getQueueID());
		
		assertEquals("CommandSetName different", jobQueue.getCommandSetName(), remoteJobQueue.getCommandSetName());
		assertEquals("CommandTopicName different", jobQueue.getCommandTopicName(), remoteJobQueue.getCommandTopicName());
		assertEquals("ConsumerID different", jobQueue.getConsumerID(), remoteJobQueue.getConsumerID());
		assertEquals("HeartbeatTopic different", jobQueue.getHeartbeatTopicName(), remoteJobQueue.getHeartbeatTopicName());
		assertEquals("queueID different", jobQueue.getQueueID(), remoteJobQueue.getQueueID());
		assertEquals("Queue status different", jobQueue.getStatus(), remoteJobQueue.getStatus());
		assertEquals("StatusSetName different", jobQueue.getStatusSetName(), remoteJobQueue.getStatusSetName());
		assertEquals("StatusTopicName different", jobQueue.getStatusTopicName(), remoteJobQueue.getStatusTopicName());
		assertEquals("SubmissionTopicName different", jobQueue.getSubmissionQueueName(), remoteJobQueue.getSubmissionQueueName());
		assertEquals("URI different", jobQueue.getURI(), remoteJobQueue.getURI());
		
		//Check that we can't access the consumer.
		try {
			remoteJobQueue.getConsumer();
		} catch (IllegalArgumentException iaEx) {
			//Expected
		}
	}

}
