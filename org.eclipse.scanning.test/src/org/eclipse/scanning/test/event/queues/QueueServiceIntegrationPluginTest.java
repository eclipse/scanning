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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.event.queues.QueueProcessFactory;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtomProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.dummy.DummyBeanProcess;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueServiceIntegrationPluginTest extends BrokerTest {
	
	protected static IQueueService queueService;
	protected static IQueueControllerService queueControl;
	private String qRoot = "fake-queue-root";
	
	@Before
	public void setup() throws Exception {
		//FOR TESTS ONLY
		QueueProcessFactory.registerProcess(DummyAtomProcess.class);
		QueueProcessFactory.registerProcess(DummyBeanProcess.class);
		
		//Configure the queue service
		queueService = ServicesHolder.getQueueService();
		queueService.setUri(uri);
		queueService.setQueueRoot(qRoot);
		queueService.init();
		
		//Configure the queue controller service
		queueControl = ServicesHolder.getQueueControllerService();
		queueControl.init();
		
		//Above here - spring will make the calls
		queueControl.startQueueService();
	}
	
	@After
	public void tearDown() throws EventException {
		queueControl.stopQueueService(false);
		queueService.disposeService();
	}
	
	@Test
	public void testRunningBean() throws EventException {
		DummyBean dummyBean = new DummyBean("Bob", 50);
		
		queueControl.submit(dummyBean, queueService.getJobQueueID());
		try {
			waitForBeanFinalStatus(dummyBean, queueService.getJobQueueID());//FIXME Put this on the QueueController
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
		
		queueControl.submit(task, queueControl.getJobQueueID());
		try {
//			Thread.sleep(1000000);
			waitForBeanFinalStatus(task, queueService.getJobQueueID());//FIXME Put this on the QueueController
		} catch (Exception e) {
			// It's only a test...
			e.printStackTrace();
		}
		
		List<QueueBean> statusSet = queueService.getJobQueue().getConsumer().getStatusSet();
		assertEquals(1, statusSet.size());
		assertEquals(Status.COMPLETE, statusSet.get(0).getStatus());
		assertEquals(task.getUniqueId(), statusSet.get(0).getUniqueId());
	}
	
	/**
	 * Same as below, but does not check for final state and waits for 10s
	 */
	private void waitForBeanStatus(Queueable bean, Status state, String queueID) throws EventException, InterruptedException {
		waitForBeanStatus(bean, state, queueID, false, 10000);
	}
	
	/**
	 * Same as below, but checks for isFinal and waits 10s
	 */
	private void waitForBeanFinalStatus(Queueable bean, String queueID) throws EventException, InterruptedException {
		waitForBeanStatus(bean, null, queueID, true, 10000);
	}
	
	/**
	 * Timeout is in ms
	 */
	private void waitForBeanStatus(Queueable bean, Status state, String queueID, boolean isFinal, long timeout) 
			throws EventException, InterruptedException {
		final CountDownLatch statusLatch = new CountDownLatch(1);
		
		//Get the queue we're interested in
		IQueue<Queueable> queue = queueService.getQueue(queueID);
		
		//Create a subscriber configured to listen for our bean
		IEventService evServ = ServicesHolder.getEventService();
		ISubscriber<IBeanListener<Queueable>> statusSubsc = evServ.createSubscriber(uri, queue.getStatusTopicName());
		statusSubsc.addListener(new IBeanListener<Queueable>() {

			@Override
			public void beanChangePerformed(BeanEvent<Queueable> evt) {
				Queueable evtBean = evt.getBean();
				if (evtBean.getUniqueId().equals(bean.getUniqueId())) {
					if ((evtBean.getStatus() == state) || (evtBean.getStatus().isFinal() && isFinal)) {
						statusLatch.countDown();
					}
				}
			}
			
		});
		//We may get stuck if the consumer finishes processing faster than the test works through
		//If so, we need to test for a non-empty status set with last bean status equal to our expectation
		
		//Once finished, check whether the latch was released or timedout
		boolean released = statusLatch.await(timeout, TimeUnit.MILLISECONDS);
		if (released) {
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~\n Final state reached\n~~~~~~~~~~~~~~~~~~~~~~~~~");
		} else {
			System.out.println("#########################\n No final state reported\n#########################");
		}
		statusSubsc.disconnect();
	}

}
