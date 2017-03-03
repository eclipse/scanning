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
package org.eclipse.scanning.test.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.event.QueueReader;
import org.eclipse.scanning.event.queues.Queue;
import org.eclipse.scanning.event.queues.QueueControllerService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.remote.RemoteServiceFactory;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.mocks.MockQueueService;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

public class RemoteQueueControllerServiceTest extends BrokerTest {

	private static IQueueControllerService      qservice;
	private        IQueueControllerService      rservice;
	
	private MockQueueService mockQServ;
	private IQueue<QueueBean> mockJobQ;
	private IQueue<QueueAtom> mockActiveQ;
	private String jqID, aqID, jqSubmQ, aqSubmQ;
	
	private static IEventService                eservice;
	
	public RemoteQueueControllerServiceTest() {
		super(true);
	}

	@BeforeClass
	public static void createServices() throws Exception {
		
		setUpNonOSGIActivemqMarshaller(); // DO NOT COPY TESTING ONLY

		RemoteServiceFactory.setTimeout(1, TimeUnit.MINUTES); // Make test easier to debug.
		
		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!

		// Set up stuff because we are not in OSGi with a test
		// DO NOT COPY TESTING ONLY

		Services.setEventService(eservice);
		ServicesHolder.setEventService(eservice);		
	}

	
	@Before
	public void createService() throws EventException {
		
		this.jqID = "mock-job-queue";
		this.jqSubmQ = jqID+IQueue.SUBMISSION_QUEUE_SUFFIX;
		this.aqID = "mock-active-queue";
		this.aqSubmQ = aqID+IQueue.SUBMISSION_QUEUE_SUFFIX;
		this.mockJobQ = new Queue<>(jqID, uri);
		this.mockActiveQ = new Queue<>(aqID, uri);
		
		this.mockQServ = new MockQueueService(mockJobQ, mockActiveQ, uri);
		mockQServ.setCommandTopicName("mock-cmd-topic");
		//Clear queues to avoid class cast errors (a StatusBean is prepopulated for another test elsewhere...)
		mockQServ.getJobQueue().clearQueues();
		mockQServ.getActiveQueue(aqID).clearQueues();
		ServicesHolder.setQueueService(mockQServ);
		//Check that our job- and active-consumers do values which are different IDs
		assertFalse("job-queue consumer ID should not be null", mockQServ.getQueue(jqID).getConsumerID() == null);
		assertFalse("active-queue consumer ID should not be null", mockQServ.getQueue(aqID).getConsumerID() == null);
		assertFalse("job- & active-queue IDs identical", mockQServ.getQueue(jqID).getConsumerID() == mockQServ.getQueue(aqID).getConsumerID());
		
		//A bit of boilerplate to start the service under test
		qservice = new QueueControllerService();
		qservice.init();
		ServicesHolder.setQueueControllerService(qservice);
		
		rservice = eservice.createRemoteService(uri, IQueueControllerService.class);
		
	}
	
	@After
	public void disposeService() throws EventException {
		mockJobQ.disconnect();
		mockActiveQ.disconnect();
		mockQServ.stop(false);
		qservice.stopQueueService(false);
		((IDisconnectable)rservice).disconnect();
	}

	@Test
	public void checkNotNull() throws Exception {
		assertNotNull(rservice);
	}
	
	/**
	 * Test whether starting & stopping pushes the right buttons in the 
	 * QueueService
	 * @throws EventException 
	 */
	@Test
	public void testStartStopService() throws EventException {
		qservice.startQueueService();
		assertTrue("Start didn't push the start button.", mockQServ.isActive());
		
		qservice.stopQueueService(false);
		assertFalse("Stop didn't push the stop button.", mockQServ.isActive());
		assertFalse("Stop should not have been forced.", mockQServ.isForced());
		
		qservice.stopQueueService(true);
		assertTrue("Stop should have been forced.", mockQServ.isForced());
	}
	
	@Test
	public void testStartStopRemote() throws EventException {
		rservice.startQueueService();
		assertTrue("Start didn't push the start button.", mockQServ.isActive());
		
		rservice.stopQueueService(false);
		assertFalse("Stop didn't push the stop button.", mockQServ.isActive());
		assertFalse("Stop should not have been forced.", mockQServ.isForced());
		
		rservice.stopQueueService(true);
		assertTrue("Stop should have been forced.", mockQServ.isForced());
	}
	
	@Test
	public void submitRemoveService() throws Exception {
		testSubmitRemove(qservice); 
	}
	
	@Test
	public void submitRemoveRemote() throws Exception {
		testSubmitRemove(rservice); 
	}
	/**
	 * Test submission and removal of beans in queues. 
	 * @throws Exception 
	 */
	public void testSubmitRemove(IQueueControllerService test) throws Exception {
		
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
		test.submit(albert, jqID);
		QueueReader<DummyBean> beanReader = new QueueReader<>(eservice.getEventConnectorService());
		List<DummyBean> beans = beanReader.getBeans(uri, jqSubmQ, DummyBean.class);		
		assertEquals("No beans in job-queue after 1 submission", 1, beans.size());
		assertEquals("Bean has wrong name", "Albert", beans.get(0).getName());
		
		test.submit(bernard, jqID);
		beans = beanReader.getBeans(uri, jqSubmQ, DummyBean.class);	
		assertEquals("Bean has wrong name", "Bernard", beans.get(1).getName());
		
		//active-queue
		test.submit(carlos, aqID);
		QueueReader<DummyAtom> atomReader = new QueueReader<>(eservice.getEventConnectorService());
		List<DummyAtom> atoms = atomReader.getBeans(uri, aqSubmQ, DummyAtom.class);		
		assertEquals("No beans in active-queue after 1 submission", 1, atoms.size());
		assertEquals("Bean has wrong name", "Carlos", atoms.get(0).getName());

		test.submit(duncan, aqID);//Needed for remove test...
		
		/*
		 * Remove:
		 * - now only one bean left and it job-queue (1st)
		 * - now only one bean left in active-queue (2nd)
		 * - throw an exception if the removed bean is no-longer removeable 
		 *   (or not in the queue in the first place)
		 */
		//job-queue
		test.remove(bernard, jqID);
		beans = beanReader.getBeans(uri, jqSubmQ, DummyBean.class);		
		assertEquals("Should only be one bean left in active-queue", 1, beans.size());
		assertEquals("Wrong bean found in queue", "Albert", beans.get(0).getName());
		
		//active-queue
		test.remove(carlos, aqID);
		atoms = atomReader.getBeans(uri, aqSubmQ, DummyAtom.class);		
		assertEquals("Should only be one bean left in active-queue", 1, atoms.size());
		assertEquals("Wrong bean found in queue", "Duncan", atoms.get(0).getName());
		try {
			test.remove(carlos, aqID);
			fail("Expected EventException: Carlos has already been removed");
		} catch (EventException evEx) {
			//Expected
		}
		
		/*
		 * Prevent submission/removal of wrong bean type to wrong queue
		 */
		try {
			test.submit(carlos, jqID);
			fail("Expected EventException when wrong queueable type submitted (atom in job-queue)");
		} catch (EventException iaEx) {
			//Expected
		}
		try {
			test.remove(bernard, aqID);
			fail("Expected EventException when wrong queueable type submitted (bean in atom-queue)");
		} catch (EventException iaEx) {
			//Expected
		}
		try {
			test.remove(xavier, aqID);
			fail("Expected EventException: Xavier was never submitted = can't be removed");
		} catch (EventException evEx) {
			//Expected
		}

	}

}
