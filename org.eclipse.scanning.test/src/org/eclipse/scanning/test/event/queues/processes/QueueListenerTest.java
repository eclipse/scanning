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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.queues.IQueueBroadcaster;
import org.eclipse.scanning.api.event.queues.beans.IHasChildQueue;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.processes.QueueListener;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueue;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueueProcess;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * QueueListener listens to the progress (status/percent complete) of one or 
 * more beans on a child queue & updates the progress of the parent bean.
 * 
 * @author Michael Wharmby
 *
 */
public class QueueListenerTest {
	
	private QueueListener<DummyHasQueue, DummyAtom> qList;
	private DummyHasQueue parent, originalParent;
	private double initPercent = 5d;
	private DummyAtom childA, childB;
	private List<DummyAtom> queue;
	
	private IPublisher<DummyHasQueue> statPub;
	private IQueueBroadcaster<DummyHasQueue> broadcaster;
	private CountDownLatch latch;
	
	@Before
	public void setUp() throws Exception {
		//Set initial state on parent
		parent = new DummyHasQueue("Big momma", 60);
		parent.setStatus(Status.RUNNING);
		parent.setMessage("");
		parent.setQueueMessage("");
		parent.setPercentComplete(initPercent);
		originalParent = new DummyHasQueue();
		originalParent.merge(parent);
		
		//Set initial state on child
		childA = new DummyAtom("Baby", 10);
		childA.setStatus(Status.SUBMITTED);
		childA.setPercentComplete(0d);
		childB = new DummyAtom("Bobby", 10);
		childB.setStatus(Status.SUBMITTED);
		childB.setPercentComplete(0d);
		
		queue = new ArrayList<>();
		queue.add(childA);
		queue.add(childB);
		
		statPub = new MockPublisher<>(null, "test.topic");
		broadcaster = new DummyHasQueueProcess<>(parent, statPub, false);
		
		latch = new CountDownLatch(1);
	}
	
	@After
	public void tearDown() {
		parent = null;
		originalParent = null;
		childA = null;
		childB = null;
		queue = null;

		statPub = null;
		broadcaster = null;
		latch = null;
	}
	
	@Test
	public void testNothingHappened() {
		//This is set before Listener creation so the bean status doesn't appear to change
		childA.setStatus(Status.RUNNING);
		childA.setPercentComplete(50d);
		qList = new QueueListener<>(broadcaster, parent, latch, childA);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertEquals("Parent has been broadcast, even though no changes happened on child", null, getLastBroadcast());
	}
	
	@Test
	public void testIgnoreNonChildren() {
		DummyAtom friend = new DummyAtom("Bilbo", 10);
		
		qList = new QueueListener<>(broadcaster, parent, latch, childA);
		
		friend.setPercentComplete(50d);
		friend.setStatus(Status.RUNNING);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(friend));
		
		if (getLastBroadcast() != null) {
			assertEquals("Parent percentage incremented even though non-child in event", getLastBroadcast().getPercentComplete(), originalParent.getPercentComplete(), 0d);
			assertEquals("Parent Status should not have changed as event was non-child", originalParent.getStatus(), getLastBroadcast().getStatus());
		}
	}
	
	@Test
	public void testNormalRun() {
		qList = new QueueListener<>(broadcaster, parent, latch, queue);
		//Change childA percentage only (not active)
		childA.setPercentComplete(50d);
		childA.setMessage("Mushroom");
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status changed even though child not active", originalParent.getStatus(), getLastBroadcast().getStatus());
		
		//Now with the status actually set...
		childA.setStatus(Status.RUNNING);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status should not have changed", originalParent.getStatus(), getLastBroadcast().getStatus());
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': Mushroom", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Running...", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		//Complete childA percentage & Status
		childA.setPercentComplete(100d);
		childA.setStatus(Status.COMPLETE);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertEquals("Parent percentage incorrectly incremented", 52.25d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status should not have changed", originalParent.getStatus(), getLastBroadcast().getStatus());
		assertEquals("Processor latch released before all children complete", 1, latch.getCount(), 0);
		
		//Complete childB percentage & Status
		childB.setStatus(Status.RUNNING);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childB));
		assertEquals("Parent has wrong queue message", "Running...", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		childB.setPercentComplete(100d);
		childB.setStatus(Status.COMPLETE);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childB));
		
		//All beans now complete, so latch should have been released
		assertEquals("Parent percentage incorrectly incremented", 99.5d, getLastBroadcast().getPercentComplete(), 0d);
		assertFalse("Parent Status should not be final", parent.getStatus().isFinal());
		assertFalse("Parent Status should not be COMPLETE", parent.getStatus() == Status.COMPLETE);
		assertEquals("Processor latch not released on completion of all children", 0, latch.getCount(), 0);
		assertEquals("Parent has wrong message", "Running finished.", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "All child processes complete.", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
	}

	/**
	 * Test running two beans together asynchronously
	 */
	@Test
	public void testAsyncNormalRun() {
		childA.setStatus(Status.RUNNING);
		childB.setStatus(Status.RUNNING);
		qList = new QueueListener<>(broadcaster, parent, latch, queue);
		childA.setPercentComplete(50d);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		childB.setPercentComplete(50d);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childB));
		
		assertEquals("Parent percentage incorrectly incremented", 52.25d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status changed even though child not active", originalParent.getStatus(), getLastBroadcast().getStatus());
		
		childA.setPercentComplete(100d);
		childA.setStatus(Status.COMPLETE);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertEquals("Parent percentage incorrectly incremented", 75.875d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status changed even though child not active", originalParent.getStatus(), getLastBroadcast().getStatus());
		assertEquals("Parent has wrong queue message", "'"+childA.getName()+"' completed successfully.", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		childB.setPercentComplete(100d);
		childB.setStatus(Status.COMPLETE);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childB));
		
		assertEquals("Parent percentage incorrectly incremented", 99.5d, getLastBroadcast().getPercentComplete(), 0d);
		assertFalse("Parent Status should not be final", parent.getStatus().isFinal());
		assertFalse("Parent Status should not be COMPLETE", parent.getStatus() == Status.COMPLETE);
		assertEquals("Parent has wrong queue message", "All child processes complete.", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		//All beans now complete, so latch should have been released
		assertEquals("Processor latch not released on completion of all children", 0, latch.getCount(), 0);
	}
	
	/**
	 * Tests REQUEST_TERMINATE and TERMINATED both lead to parent entering 
	 * REQUEST_TERMINATE state
	 */
	@Test
	public void testRunExternalTermination() {
		Status[] termCalls = new Status[]{Status.REQUEST_TERMINATE, Status.TERMINATED};
		String[] termMessages = new String[]{"Asking nicely to die", "I'm dead already"};
		for (int i = 0; i < 2; i++) {
			childA.setStatus(Status.RUNNING);
			childA.setMessage("Happy");
			qList = new QueueListener<>(broadcaster, parent, latch, queue);
			childA.setPercentComplete(50d);
			qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));

			assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
			assertEquals("Parent has wrong message", "'"+childA.getName()+"': Happy", parent.getMessage());

			childA.setStatus(termCalls[i]);
			childA.setMessage(termMessages[i]);
			qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));

			assertTrue("Child queue instruction not indicated", qList.isChildCommand());
			assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
			assertEquals("Parent Status not changed", Status.REQUEST_TERMINATE, getLastBroadcast().getStatus());
			assertEquals("Parent has wrong message", "'"+childA.getName()+"': "+termMessages[i], getLastBroadcast().getMessage());
			assertEquals("Parent has wrong queue message", "Termination requested from '"+childA.getName()+"'", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
			
			//Termination of the parent is handled within the parent processor NOT by the Listener
			assertEquals("Latch has been released when it shouldn't have been.", 1, latch.getCount(), 0);
		}
	}
	
	/**
	 * Tests that both pausing & resuming from the child lead to the parent 
	 * moving to a REQUEST_PAUSE & REQUEST_RESUME. Also ensures {ACTION} and 
	 * REQUEST_{ACTION} Statuses are handled the same.
	 */
	@Test
	public void testRunExternalPause() {
		childA.setStatus(Status.RUNNING);
		childA.setMessage("Badgers");
		qList = new QueueListener<>(broadcaster, parent, latch, queue);
		childA.setPercentComplete(50d);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));

		assertEquals("Percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);

		childA.setStatus(Status.REQUEST_PAUSE);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));

		assertTrue("Child queue instruction not indicated", qList.isChildCommand());
		assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status not changed", Status.REQUEST_PAUSE, getLastBroadcast().getStatus());
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': Badgers", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Pause requested from '"+childA.getName()+"'", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		assertEquals("Latch has been released when it shouldn't have been.", 1, latch.getCount(), 0);
		
		//Mimic effect of processor
		parent.setStatus(Status.PAUSED);
		
		childA.setStatus(Status.REQUEST_RESUME);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertTrue("Child queue instruction not indicated", qList.isChildCommand());
		assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status not changed", Status.REQUEST_RESUME, getLastBroadcast().getStatus());
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': Badgers", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Resume requested from '"+childA.getName()+"'", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		assertEquals("Latch has been released when it shouldn't have been.", 1, latch.getCount(), 0);
		
		//Mimic effect of processor
		parent.setStatus(Status.RESUMED);
		childA.setStatus(Status.RUNNING);
		childA.setPercentComplete(80d);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		assertEquals("Parent percentage incorrectly incremented", 42.8d, getLastBroadcast().getPercentComplete(), 0d);
		assertFalse("Child queue instruction set after RUNNING", qList.isChildCommand());//Child queue should be reset false after a normal pass
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': Badgers", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Running...", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		//Mimic effect of processor
		parent.setStatus(Status.RUNNING);
		
		childA.setStatus(Status.PAUSED);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));

		assertTrue("Child queue instruction not indicated", qList.isChildCommand());
		assertEquals("Parent percentage incorrectly incremented", 42.8d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status not changed", Status.REQUEST_PAUSE, getLastBroadcast().getStatus());
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': Badgers", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Pause requested from '"+childA.getName()+"'", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		assertEquals("Latch has been released when it shouldn't have been.", 1, latch.getCount(), 0);
		
		//Mimic effect of processor
		parent.setStatus(Status.PAUSED);
		childA.setStatus(Status.RESUMED);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertTrue("Child queue instruction not indicated", qList.isChildCommand());
		assertEquals("Parent percentage incorrectly incremented", 42.8d, getLastBroadcast().getPercentComplete(), 0d);
		assertEquals("Parent Status not changed", Status.REQUEST_RESUME, getLastBroadcast().getStatus());
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': Badgers", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Resume requested from '"+childA.getName()+"'", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		assertEquals("Latch has been released when it shouldn't have been.", 1, latch.getCount(), 0);
	}

	/**
	 * Tests that a failed report in a child queue leads to the parent being 
	 * returned to the processor (latch released) with a non-final state and 
	 * the percentage less than the completion percentage (99.5). Fail should 
	 * be marked as a child command for completeness. Message from
	 */
	@Test
	public void testRunExternalFail() {
		childA.setStatus(Status.RUNNING);
		qList = new QueueListener<>(broadcaster, parent, latch, queue);
		childA.setPercentComplete(50d);
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));

		assertEquals("Percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
		
		//Mimic a fail in the child queue
		childA.setStatus(Status.FAILED);
		childA.setMessage("My old man's a dustman.");
		qList.beanChangePerformed(new BeanEvent<DummyAtom>(childA));
		
		assertTrue("Child queue instruction not indicated", qList.isChildCommand());
		assertEquals("Parent percentage incorrectly incremented", 28.625d, getLastBroadcast().getPercentComplete(), 0d);
		assertFalse("Parent Status should not be final", parent.getStatus().isFinal());
		assertEquals("Parent has wrong message", "'"+childA.getName()+"': My old man's a dustman.", getLastBroadcast().getMessage());
		assertEquals("Parent has wrong queue message", "Failure caused by '"+childA.getName()+"'", ((IHasChildQueue)getLastBroadcast()).getQueueMessage());
		
		//Failure setting of the parent is handled within the parent processor NOT by the Listener
		assertEquals("Latch has not been released when it should have been.", 0, latch.getCount(), 0);
	}

	private StatusBean getLastBroadcast() {
		List<StatusBean> broadBeans = ((MockPublisher<?>)statPub).getBroadcastBeans();
		if (broadBeans.size() == 0) {
			return null;
		} else {
			return broadBeans.get(broadBeans.size()-1);
		}
	}
}
