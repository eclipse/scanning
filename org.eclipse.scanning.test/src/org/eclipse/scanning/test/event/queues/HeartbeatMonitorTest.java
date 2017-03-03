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
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.queues.IHeartbeatMonitor;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.QueueBean;
import org.eclipse.scanning.event.Constants;
import org.eclipse.scanning.event.queues.HeartbeatMonitor;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.test.event.queues.dummy.DummyAtom;
import org.eclipse.scanning.test.event.queues.dummy.DummyBean;
import org.eclipse.scanning.test.event.queues.mocks.MockQueue;
import org.eclipse.scanning.test.event.queues.mocks.MockQueueService;
import org.eclipse.scanning.test.event.queues.util.EventInfrastructureFactoryService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Deprecated
public class HeartbeatMonitorTest {
	
	private static EventInfrastructureFactoryService infrastructureServ;
	
	private IConsumer<QueueBean> consOne;
	private IConsumer<QueueAtom >consTwo;
	private IQueue<QueueBean> mockOne;
	private IQueue<QueueAtom> mockTwo;
	private MockQueueService mQServ;
	private UUID consOneID, consTwoID;
	private HeartbeatMonitor hbM;
	
	private URI uri;
	
	@BeforeClass
	public static void initialise() throws Exception {
		infrastructureServ = new EventInfrastructureFactoryService();
		infrastructureServ.start(true);
	}
	
	@AfterClass
	public static void shutdown() throws Exception {
		infrastructureServ.stop();
	}
	
	@Before
	public void setUp() throws Exception {
		Constants.setNotificationFrequency(200);
		
		consOne = infrastructureServ.makeConsumer(new DummyBean(), true);
		consOneID = consOne.getConsumerId();
		uri = infrastructureServ.getURI();
		
		//This is not a plugin-test - need to supply the EventService
		ServicesHolder.setEventService(infrastructureServ.getEventService());
	}
	
	@After
	public void tearDown() throws Exception {
		hbM = null;
		
		consOne.clearQueue(IEventService.SUBMISSION_QUEUE);
		consOne.clearQueue(IEventService.STATUS_SET);
		consOne.clearQueue(IEventService.CMD_SET);
		consOne.disconnect();
		
		if (consTwo != null) {
			consTwo.clearQueue(IEventService.SUBMISSION_QUEUE);
			consTwo.clearQueue(IEventService.STATUS_SET);
			consTwo.clearQueue(IEventService.CMD_SET);
			consTwo.disconnect();
		}
		
		mockOne = null;
		mockTwo = null;
		
		consOneID = null;
		consTwoID = null;
		
		Constants.setNotificationFrequency(2000);
	}
	
	@Test
	public void testLatestHeartbeats() throws Exception {
		hbM = new HeartbeatMonitor(uri, IEventService.HEARTBEAT_TOPIC, consOne.getConsumerId());
		consOne.start();
		Thread.sleep(600);
		
		List<HeartbeatBean> heartbeats = hbM.getLatestHeartbeats();
		assertEquals("Unexpected number of heartbeats", 2, heartbeats.size());
	}
	
	@Test
	public void testLastHeartbeat() throws Exception {
		hbM = new HeartbeatMonitor(uri, IEventService.HEARTBEAT_TOPIC, consOne.getConsumerId());
		consOne.start();
		waitForHeartbeat(600);
		
		final HeartbeatBean first = hbM.getLastHeartbeat();
		Thread.sleep(230); //Heartbeat every 200ms
		final HeartbeatBean second = hbM.getLastHeartbeat();
		consOne.start();
		List<HeartbeatBean> heartbeats = hbM.getLatestHeartbeats();
		
		assertFalse("First & second heartbeats are the same", first.equals(second));
		assertEquals("Last heartbeat in latest and lastBeat differ", second, heartbeats.get(heartbeats.size()-1));
	}
	
	@Test
	public void testChangingConsumerID() throws Exception {
		hbM = new HeartbeatMonitor(uri, IEventService.HEARTBEAT_TOPIC, consOneID);
		consOne.start();
		waitForHeartbeat(600);
		
		final HeartbeatBean first = hbM.getLastHeartbeat();
		assertEquals("Heartbeat from wrong consumer", consOneID, first.getConsumerId());
		
		createSecondConsumer();
		assertFalse("IDs of two consumers are identical", consTwoID.equals(consOne.getConsumerId()));
		
		hbM.setConsumerID(consTwoID);
		consTwo.start();
		waitForHeartbeat(600);
		
		final HeartbeatBean second = hbM.getLastHeartbeat();
		assertEquals("Heartbeat from wrong consumer", consTwoID, second.getConsumerId());
		assertFalse("HeartbeatBean consumerIDs identical", consOneID.equals(consTwoID));
	}

	@Test
	public void testChangingQueue() throws Exception {
		createTwoQueues();
		
		hbM = new HeartbeatMonitor(uri, mockOne);
		assertEquals("Wrong queueID set on monitor", "MockQueueOne", hbM.getQueueID());
		assertEquals("Wrong consumerID set on monitor", consOneID, hbM.getConsumerID());
		consOne.start();
		waitForHeartbeat(600);
		
		final HeartbeatBean first = hbM.getLastHeartbeat();
		assertEquals("Heartbeat from wrong consumer", consOneID, first.getConsumerId());
		
		hbM.setQueue(mockTwo);
		assertEquals("Wrong queueID set on monitor", "MockQueueTwo", hbM.getQueueID());
		assertEquals("Wrong consumerID set on monitor", consTwoID, hbM.getConsumerID());
		consTwo.start();
		waitForHeartbeat(600);
		
		final HeartbeatBean second = hbM.getLastHeartbeat();
		assertEquals("Heartbeat from wrong consumer", consTwoID, second.getConsumerId());
		assertFalse("HeartbeatBean consumerIDs identical", consOneID.equals(consTwoID));
	}
	
	@Test
	public void testChangingQueueID() throws Exception {
		createQueueService();
		mQServ.start();
		
		hbM = new HeartbeatMonitor(uri, IEventService.HEARTBEAT_TOPIC, "MockQueueOne", mQServ);
		assertEquals("Wrong queueID set on monitor", "MockQueueOne", hbM.getQueueID());
		assertEquals("Wrong consumerID set on monitor", consOneID, hbM.getConsumerID());
		waitForHeartbeat(600);
		
		final HeartbeatBean first = hbM.getLastHeartbeat();
		assertEquals("Heartbeat from wrong consumer", consOneID, first.getConsumerId());
		
		hbM.setQueueID("MockQueueTwo", mQServ);
		assertEquals("Wrong queueID set on monitor", "MockQueueTwo", hbM.getQueueID());
		assertEquals("Wrong consumerID set on monitor", consTwoID, hbM.getConsumerID());
		waitForHeartbeat(600);
		
		final HeartbeatBean second = hbM.getLastHeartbeat();
		assertEquals("Heartbeat from wrong consumer", consTwoID, second.getConsumerId());
		assertFalse("HeartbeatBean consumerIDs identical", consOneID.equals(consTwoID));
	}
	
	@Test
	public void testFixedQueueMonitor() throws Exception {
		createQueueService();
		
		IHeartbeatMonitor[] hbms = new IHeartbeatMonitor[]{
				new HeartbeatMonitor(uri, IEventService.HEARTBEAT_TOPIC, consOneID, true),
				new HeartbeatMonitor(uri, mockOne, true),
				new HeartbeatMonitor(uri, IEventService.HEARTBEAT_TOPIC, "MockQueueOne", mQServ, true)
		};
		
		for (IHeartbeatMonitor hbm : hbms) {
			try {
				hbm.setConsumerID(consTwoID);
				fail("Able to change consumerID when monitor locked");
			} catch (EventException ex) {
				//expected
			}
			try {
				hbm.setQueue(mockTwo);
				fail("Able to change Queue when monitor locked");
			} catch (EventException ex) {
				//expected
			}
			try {
				hbm.setQueueID(mockTwo.getQueueID(), mQServ);
				fail("Able to change queueID when monitor locked");
			} catch (EventException ex) {
				//expected
			}
		}
		
		
	}
	
	private void createSecondConsumer() throws Exception {
		consTwo = infrastructureServ.makeConsumer(new DummyAtom(), true);
		consTwoID = consTwo.getConsumerId();
		assertFalse("IDs of two consumers are identical", consTwoID.equals(consOneID));
	}
	
	private void createTwoQueues() throws Exception {
		createSecondConsumer();
		mockOne = new MockQueue<>("MockQueueOne", consOne);
		mockTwo = new MockQueue<>("MockQueueTwo", consTwo);
	}
	
	private void createQueueService() throws Exception {
		createTwoQueues();
		
		mQServ = new MockQueueService(mockOne);
		mQServ.addActiveQueue(mockTwo);
		mQServ.start();
	}
	
	private void waitForHeartbeat(long timeout) throws Exception {
		while (timeout > 0) {
			if (hbM.getLatestHeartbeats().size() > 0) break;
			timeout -= 100;
			Thread.sleep(100);
		}
	}

}
