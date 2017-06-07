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
package org.eclipse.scanning.test.event.queues.integration;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.event.queues.RealQueueTestUtils;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtomProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyBeanProcess;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class QueueServiceIntegrationPluginTest extends BrokerTest {
	
	protected static IEventService evServ;
	protected static IQueueService queueService;
	protected static IQueueControllerService queueControl;
	
	/*
	 * These three methods are called by OSGi to configure services during a
	 * plugin test - see OSGI-INF/queueServiceIntegrationPluginTest.xml
	 */
	public static void setEventService(IEventService eServ) {
		ServicesHolder.setEventService(eServ);
	}
	public static void setQueueService(IQueueService qServ) {
		ServicesHolder.setQueueService(qServ);
	}
	public static void setQueueControllerService(IQueueControllerService qcServ) {
		ServicesHolder.setQueueControllerService(qcServ);
	}
	
	@Before
	public void setup() throws Exception {
		RealQueueTestUtils.initialise(uri);
		//FOR TESTS ONLY
		QueueProcessFactory.registerProcess(DummyAtomProcess.class);
		QueueProcessFactory.registerProcess(DummyBeanProcess.class);
		
		//Get services service & start the QueueService
		evServ = ServicesHolder.getEventService();
		queueService = ServicesHolder.getQueueService();
		queueControl = ServicesHolder.getQueueControllerService();
		
		//Above here - spring will make the calls
		queueControl.startQueueService();
	}
	
	@After
	public void tearDown() throws EventException {
		QueueProcessFactory.initialize(); //Remove the registered processes
		queueControl.stopQueueService(false);
		queueService.disposeService();
		
		RealQueueTestUtils.reset();
	}
	
	@AfterClass
	public static void tearDownClass() throws EventException {
		RealQueueTestUtils.dispose();
	}
	
	@Test
	public void testRunningBean() throws EventException {
		DummyBean dummyBean = new DummyBean("Bob", 50);
		CountDownLatch waiter = RealQueueTestUtils.createFinalStateBeanWaitLatch(dummyBean, queueControl.getJobQueueID());
		
		queueControl.submit(dummyBean, queueService.getJobQueueID());
		try {
			RealQueueTestUtils.waitForEvent(waiter);
		} catch (Exception e) {
			// It's only a test...
			e.printStackTrace();
		}
		
		List<QueueBean> statusSet = queueService.getJobQueue().getConsumer().getStatusSet();
		assertEquals(1, statusSet.size());
		assertEquals(Status.COMPLETE, statusSet.get(0).getStatus());
		assertEquals(dummyBean.getUniqueId(), statusSet.get(0).getUniqueId());
	}
	
	@Test
	public void testTaskBean() throws EventException {
		TaskBean task = new TaskBean();
		task.setName("Test Task");
		
		SubTaskAtom subTask = new SubTaskAtom();
		subTask.setName("Test SubTask");
		
		DummyAtom dummyAtom = new DummyAtom("Gregor", 70);
		subTask.addAtom(dummyAtom);
		task.addAtom(subTask);
		
		CountDownLatch waiter = RealQueueTestUtils.createFinalStateBeanWaitLatch(task, queueControl.getJobQueueID());
		queueControl.submit(task, queueControl.getJobQueueID());
		try {
			RealQueueTestUtils.waitForEvent(waiter);
		} catch (Exception e) {
			// It's only a test...
			e.printStackTrace();
		}
		
		List<QueueBean> statusSet = queueService.getJobQueue().getConsumer().getStatusSet();
		assertEquals(1, statusSet.size());
		assertEquals(Status.COMPLETE, statusSet.get(0).getStatus());
		assertEquals(task.getUniqueId(), statusSet.get(0).getUniqueId());
	}

}
