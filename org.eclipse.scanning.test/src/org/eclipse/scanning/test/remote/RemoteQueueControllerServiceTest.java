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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.QueueService;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.remote.RemoteServiceFactory;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.event.queues.RealQueueTestUtils;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RemoteQueueControllerServiceTest extends BrokerTest {

	private static IQueueControllerService      qservice;
	private        IQueueControllerService      rservice;
	
	private IQueueService qServ;

	private String jqID, aqID;
	
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
		
		//A bit of boilerplate to start the service under test
		qServ =  new QueueService("remote-test-queue-root", uri.toString());
		qServ.init();
		ServicesHolder.setQueueService(qServ);
		qservice = (IQueueControllerService)qServ;
		ServicesHolder.setQueueControllerService(qservice);
				
		rservice = eservice.createRemoteService(uri, IQueueControllerService.class);
		
		//Last thing, reset the support utils.
		RealQueueTestUtils.initialise(uri);
		
		//In travis submitRemove test fails because bean is consumed; reset QueueProcessFactory to prevent this
		QueueProcessFactory.initialize();
		
	}
	
	@After
	public void disposeService() throws EventException {
		rservice.stopQueueService(true);
		((IDisconnectable)rservice).disconnect();
		rservice = null;
		
		qservice.stopQueueService(true);
		qservice = null;
		
		qServ.disposeService();
		qServ = null;
		ServicesHolder.setQueueService(null);
		ServicesHolder.setQueueControllerService(null);
		
		RealQueueTestUtils.reset();
	}
	
	@AfterClass
	public static void tearDownClass() throws EventException {
		RealQueueTestUtils.dispose();
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
	public void testStartStopService() throws Exception {
		//Create command listener
		CountDownLatch waiter = RealQueueTestUtils.createCommandBeanWaitLatch(qServ.getCommandTopicName(), 1);
		//listenerForCommandBeans(qServ.getCommandTopicName(), 1);
		
		//Start the service for test
		qservice.startQueueService();
		assertTrue("Start didn't push the start button.", qServ.isActive());
		
		qservice.stopQueueService(false);
		assertFalse("Stop didn't push the stop button.", qServ.isActive());
		assertFalse("Stop should not have been forced.", lastBeanWasKiller(waiter, true));
		
		//Re-start service to test force stop  
		qservice.startQueueService();
		qservice.stopQueueService(true);
		assertTrue("Stop should have been forced.", lastBeanWasKiller(waiter));
	}
	
	@Test
	public void testStartStopRemote() throws Exception {
		//Create command listener
		CountDownLatch waiter = RealQueueTestUtils.createCommandBeanWaitLatch(qServ.getCommandTopicName(), 1);
		
		//Start the service for test
		rservice.startQueueService();
		assertTrue("Start didn't push the start button.", qServ.isActive());
		
		rservice.stopQueueService(false);
		assertFalse("Stop didn't push the stop button.", qServ.isActive());
		assertFalse("Stop should not have been forced.", lastBeanWasKiller(waiter, true));
		
		//Re-start service to test force stop
		rservice.startQueueService();
		rservice.stopQueueService(true);
		assertTrue("Stop should have been forced.", lastBeanWasKiller(waiter));
	}
	
	private boolean lastBeanWasKiller(CountDownLatch latch) throws InterruptedException {
		return lastBeanWasKiller(latch, false);
	}
	
	private boolean lastBeanWasKiller(CountDownLatch latch, boolean noWait) throws InterruptedException {
		ConsumerCommandBean cmdBean;
		if (noWait) {
			System.out.println("Not waiting so expect timeout...");
			cmdBean = RealQueueTestUtils.waitToGetCmdBean(latch, 1000L, true);
		} else {
			cmdBean = RealQueueTestUtils.waitToGetCmdBean(latch, 3000L, false);
		}
		if (cmdBean == null) return false;
		if (cmdBean instanceof KillBean) return true;
		return false;
	}
	
	@Test
	public void submitRemoveService() throws Exception {
		System.out.println("\nService submitRemove test\n-------------------------");
		setQueueNames();
		testSubmitRemove(qservice); 
	}
	
	@Test
	public void submitRemoveRemote() throws Exception {
		System.out.println("\nRemote submitRemove test\n------------------------");
		setQueueNames();
		testSubmitRemove(rservice); 
	}
	/**
	 * Test submission and removal of beans in queues. 
	 * @throws Exception 
	 */
	public void testSubmitRemove(IQueueControllerService test) throws Exception {
		//For all submit/remove testing we don't want to process anything
		qservice.pauseQueue(jqID);
		qservice.pauseQueue(aqID);
		Thread.sleep(500); //PauseBean takes time to have an effect (increased time from 100)
		
		//Beans for submission
		DummyBean albert = new DummyBean("Albert", 10),bernard = new DummyBean("Bernard", 20), fred = new DummyBean("Fred", 60), geoff = new DummyBean("Geoff", 70);
		DummyAtom carlos = new DummyAtom("Carlos", 30), duncan = new DummyAtom("Duncan", 40), enrique = new DummyAtom("Enrique", 50);
		DummyAtom xavier = new DummyAtom("Xavier", 100);
		
		/*
		 * Submit:
		 * - new DummyBean in job-queue with a particular name
		 * - 2nd and we have two in submit queue (second has particular name)
		 * - two DummyBeans in active-queue with particular names 
		 */
		//job-queue
		test.submit(albert, jqID);
		List<? extends Queueable> beans = getSubmitQueue(jqID);
		assertEquals("Exactly one bean should be submitted", 1, beans.size());
		assertEquals("Bean has wrong name", "Albert", beans.get(0).getName());
		
		test.submit(bernard, jqID);
		beans = getSubmitQueue(jqID);
		assertEquals("Exactly two beans should be submitted", 2, beans.size());
		assertEquals("Bean has wrong name", "Bernard", beans.get(1).getName());
		
		//active-queue
		test.submit(carlos, aqID);
		beans = getSubmitQueue(aqID);
		RealQueueTestUtils.waitForSubmitQueueLength(qServ.getQueue(aqID).getConsumer(), 5000L, 1);
		assertEquals("Exactly one bean should be submitted", 1, beans.size());
		assertEquals("Bean has wrong name", "Carlos", beans.get(0).getName());

		/*
		 * Remove:
		 * - now only one bean left and it job-queue (1st)
		 * - now only one bean left in active-queue (2nd)
		 * - throw an exception if the removed bean is no-longer removeable 
		 *   (or not in the queue in the first place)
		 */
		//active-queue
		Thread.sleep(100); //PauseBean takes time to have an effect
		test.submit(duncan, aqID);//Needed for remove test...
		test.submit(enrique, aqID);
		RealQueueTestUtils.waitForSubmitQueueLength(qServ.getQueue(aqID).getConsumer(), 5000L, 2);
		
		beans = getSubmitQueue(aqID);
		assertEquals("Should be two beans in the active-queue submit queue", 3, beans.size());
		test.remove(enrique, aqID);
		beans = getSubmitQueue(aqID);
		assertEquals("Should only be one bean left in active-queue", 2, beans.size());
		assertEquals("Wrong bean found in queue", "Duncan", beans.get(1).getName());
		
		//job-queue
		test.submit(fred, jqID);
		test.submit(geoff, jqID);
		RealQueueTestUtils.waitForSubmitQueueLength(qServ.getJobQueue().getConsumer(), 3000L, 2);
		
		beans = getSubmitQueue(jqID);
		assertEquals("Should be two beans in the job-queue submit queue", 4, beans.size());
		test.remove(fred, jqID);
		beans = getSubmitQueue(jqID);
		assertEquals("Should only be one bean left in active-queue", 3, beans.size());
		assertEquals("Wrong bean found in queue", "Geoff", beans.get(2).getName());
		
		try {
			test.remove(enrique, aqID);
			fail("Expected EventException: Enrique has already been removed");
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

		System.out.println("\nsubmitRemove test done\n----------------------\n");
	}
	
	private void setQueueNames() throws EventException {
		qServ.start();
		jqID = qServ.getJobQueueID();
		aqID = qServ.registerNewActiveQueue();
		qServ.startActiveQueue(aqID);
	}
	
	private List<? extends Queueable> getSubmitQueue(String queueID) throws EventException {
		return qServ.getQueue(queueID).getConsumer().getSubmissionQueue();
	}

}
