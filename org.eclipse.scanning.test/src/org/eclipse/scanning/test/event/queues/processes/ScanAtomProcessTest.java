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
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanRequest;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.api.points.models.IScanPathModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.processes.QueueProcess;
import org.eclipse.scanning.event.queues.processes.ScanAtomProcess;
import org.eclipse.scanning.test.event.queues.mocks.MockEventService;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.eclipse.scanning.test.event.queues.mocks.MockSubmitter;
import org.eclipse.scanning.test.event.queues.mocks.MockSubscriber;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ScanAtomProcessTest {
	
	private ScanAtom scAt;
	private QueueProcess<ScanAtom, Queueable> scAtProc;
	private ProcessTestInfrastructure pti;
	
	private static MockPublisher<? extends StatusBean> mockPub;
	private static MockSubscriber<? extends EventListener> mockSubsc;
	private static MockSubmitter<? extends StatusBean> mockSub;
	private static MockEventService mockEvServ;
	
	@BeforeClass
	public static void setUpClass() {
		mockPub = new MockPublisher<>(null, null);
		mockSub = new MockSubmitter<>();
		mockSubsc = new MockSubscriber<>(null, null);
		mockEvServ = new MockEventService();
		mockEvServ.setMockPublisher(mockPub);
		mockEvServ.setMockSubmitter(mockSub);
		mockEvServ.setMockSubscriber(mockSubsc);
		ServicesHolder.setEventService(mockEvServ);
	}
	
	@Before
	public void setUp() throws EventException {
		pti = new ProcessTestInfrastructure();
		
		//Create test atom & process
		List<IScanPathModel> scanAxes = new ArrayList<>();
		scanAxes.add(new StepModel("ocs", 290, 80, 10));
		scanAxes.add(new StepModel("xMotor", 150, 100, 5));

		Map<String, Object> detectors = new HashMap<>();
		detectors.put("pe", new MockDetectorModel(30d));

		List<String> monitors = new ArrayList<>();
		monitors.add("bpm3");
		monitors.add("i0");

		scAt = new ScanAtom("VT scan across sample", scanAxes, detectors); 

		try {
			scAt.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException ex) {
			System.out.println("WARNING: Failed to set hostname on bean. Continuing...");
		}
		scAt.setUserName("abc12345");
		scAt.setBeamline("I15-1");
		try {
			scAt.setScanBrokerURI("file:///fake.uri");
		} catch (Exception ex) {
			System.out.println("Failed to set broker URI"+ex.getMessage());
		}
		scAt.setScanSubmitQueueName("fake.test.submit"+IQueue.SUBMISSION_QUEUE_SUFFIX);
		
		scAtProc = new ScanAtomProcess<Queueable>(scAt, pti.getPublisher(), false);
		
		//Reset queue architecture
		mockSub.resetSubmitter();
		mockPub.resetPublisher();
		mockSubsc.resetSubscriber();
	}
	
	@After
	public void tearDown() {
		pti = null;
	}
	
	/**
	 * After execution:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should be Status.COMPLETE and 100%
	 * - status publisher should have: 1 RUNNING bean and 1 COMPLETE bean
	 * - ScanBean in child queue should have ScanRequest with configuration of ScanAtom
	 * - child queue infrastructure should be disconnected
	 * 
	 * N.B. This is *NOT* an integration test, so beans don't get run.
	 *      It only checks the processor behaves as expected
	 */
	@Test
	public void testExecution() throws Exception {
		pti.executeProcess(scAtProc, scAt, true);
		pti.waitForExecutionEnd(100000l);//FIXME
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
		
		//These are the statuses & percent completes reported by the processor as it sets up the run
		Status[] reportedStatuses = new Status[]{Status.RUNNING, Status.RUNNING,
				Status.RUNNING, Status.RUNNING, Status.RUNNING, Status.RUNNING};
		Double[] reportedPercent = new Double[]{0d, 1d, 
				2d, 3d, 4d, 5d};

		pti.checkFirstBroadcastBeanStatuses(reportedStatuses, reportedPercent);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, true);
		
		assertEquals("Wrong scan submit queue on bean", "fake.test.submit"+IQueue.SUBMISSION_QUEUE_SUFFIX, ((ScanAtom)pti.getLastBroadcastBean()).getScanSubmitQueueName());

		pti.checkSubmittedBeans(mockSub, "fake.test.submit");
		ScanBean submitted = (ScanBean)pti.getSubmittedBeans(mockSub, "fake.test.submit").get(0);
		ScanRequest<?> submScanReq = submitted.getScanRequest();
		assertEquals("Scan axis descriptions are wrong", scAt.getPathModels(), submScanReq.getCompoundModel().getModels());
		assertEquals("Scan detector models are wrong", scAt.getDetectorModels(), submScanReq.getDetectors());
		assertEquals("Scan monitors are wrong", scAt.getMonitors(), submScanReq.getMonitorNames());
		
		checkScanInfrastructureDisconnected();
	}
	
	/**
	 * On terminate:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should Status.TERMINATED and not be 100% complete
	 * - status publisher should have a TERMINATED bean
	 * - termination message should be set on the bean
	 * - child bean should have received a terminate message
	 * - child queue infrastructure should be disconnected
	 */
	@Test
	public void testTermination() throws Exception {
		pti.executeProcess(scAtProc, scAt);
		pti.waitToTerminate(100l, true);
		pti.waitForBeanFinalStatus(500000l);//FIXME
		pti.checkLastBroadcastBeanStatuses(Status.TERMINATED, false);
		
		assertEquals("Wrong message set after termination.", "Scan aborted before completion (requested).", pti.getLastBroadcastBean().getMessage());
		
		assertEquals("Unexpected number of messages in publisher", 1, mockPub.getBroadcastBeans().size());
		StatusBean pubBean = mockPub.getBroadcastBeans().get(0);
		assertEquals("Wrong status on published bean", Status.REQUEST_TERMINATE, pubBean.getStatus());
		
		checkScanInfrastructureDisconnected();
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
	 * - child active-queue should be deregistered from QueueService
	 */
	@Test
	public void testChildFailure() throws Exception {
		pti.executeProcess(scAtProc, scAt);
		//Set some arbitrary percent complete
		scAtProc.broadcast(Status.RUNNING, 20d);
		scAtProc.getProcessLatch().countDown();
		//Need to give the post-match analysis time to run
		Thread.sleep(10);
		
		/*
		 * FAILED is always going to happen underneath - i.e. process will be 
		 * running & suddenly latch will be counted down.
		 */
		
		pti.checkLastBroadcastBeanStatuses(Status.FAILED, false);
		
		checkScanInfrastructureDisconnected();
	}

	private void checkScanInfrastructureDisconnected() {
		//Scan infrastructure created should be disconnected afterwards
		assertTrue("Submitter not disconnected", mockSub.isDisconnected());
		assertTrue("Subscriber not disconnected", mockSubsc.isDisconnected());
		assertTrue("Publisher not disconnected", mockPub.isDisconnected());
	}
	
}
