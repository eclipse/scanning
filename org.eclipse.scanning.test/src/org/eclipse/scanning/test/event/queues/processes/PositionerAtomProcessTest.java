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

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.processes.PositionerAtomProcess;
import org.eclipse.scanning.event.queues.processes.QueueProcess;
import org.eclipse.scanning.example.scannable.MockNeXusScannable;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.test.event.queues.mocks.MockPositioner;
import org.eclipse.scanning.test.event.queues.mocks.MockScanService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



public class PositionerAtomProcessTest {
	
	private PositionerAtom posAt;
	private QueueProcess<PositionerAtom, Queueable> posAtProc;
	
	//Infrastructure
	private ProcessTestInfrastructure pti;
	private static IRunnableDeviceService dService;
	private static IScannableDeviceService connector;
	private static MockScannable robotArm;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		connector = new MockScannableConnector(null);
		MockScannable fakeRobotArm = new MockNeXusScannable("robot_arm", 800d, 3, "mm");
		fakeRobotArm.setMoveRate(100);
		fakeRobotArm.setRealisticMove(true);
		fakeRobotArm.setRequireSleep(false);
		connector.register(fakeRobotArm);
		robotArm = fakeRobotArm;
		
		dService  = new RunnableDeviceServiceImpl(connector);
		ServicesHolder.setDeviceService(dService);
		ServicesHolder.setScannableDeviceService(connector);
	}
	
	@Before
	public void setUp() throws Exception {
		pti = new ProcessTestInfrastructure();
		
		posAt = new PositionerAtom("mvRbtRm", "robot_arm", 1250d);
		posAt.setName("Move robot arm");
		posAtProc = new PositionerAtomProcess<>(posAt, pti.getPublisher(), false);
		
		//Reset the IScannable
		robotArm.setRealisticMove(false);
		robotArm.setPosition(800d);
		robotArm.setRealisticMove(true);
	}
	
	@After
	public void tearDown() {
		pti = null;
	}
	
	@AfterClass
	public static void tearDownClass() {
		ServicesHolder.unsetDeviceService(dService);
		dService = null;
		
		connector = null;
	}
	
	@Test
	public void checkTestInfrastructUp() {
		assertTrue(dService != null);
		assertTrue(robotArm != null);
	}
	
	/**
	 * After execution:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should be Status.COMPLETE and 100%
	 * - status publisher should have: 1 RUNNING bean and 1 COMPLETE bean
	 */
	@Test
	public void testExecution() throws Exception {
		pti.executeProcess(posAtProc, posAt);
		pti.waitForExecutionEnd(10000l);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
		
		assertEquals("Incorrect message after execute", "Set position completed successfully", pti.getLastBroadcastBean().getMessage());
		
		//Check we moved the arm
		assertEquals("Robot arm not moved", 1250d, robotArm.getPosition());
	}
	
	/**
	 * On terminate:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should Status.TERMINATED and not be 100% complete
	 * - status publisher should have a TERMINATED bean
	 * - termination message should be set on the bean
	 * - IPositioner should have received an abort command
	 * 
	 * N.B. PositionerAtomProcessorTest uses MockPostioner, which pauses for 
	 * 100ms does something then pauses for 150ms.
	 */
	@Test
	public void testTermination() throws Exception {
		//Reduce the move distance so it happens faster
		posAt.addPositioner("robot_arm", 850d);
		
		pti.executeProcess(posAtProc, posAt, false, true);
		pti.waitToTerminate(100l);
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.TERMINATED, false);
		
		pti.waitForExecutionEnd(500);
		assertEquals("Incorrect message after terminate", "Position change aborted before completion (requested)", pti.getLastBroadcastBean().getMessage());
//		assertTrue("IPositioner not aborted", ((MockPositioner)dService.createPositioner()).isAborted());
//		assertFalse("Position setting should have been terminated", ((MockPositioner)dService.createPositioner()).isMoveComplete());
		
		//We moved the arm a bit
		Double robPos = (Double)robotArm.getPosition();
		assertTrue("Robot arm should have moved, but not all the way", robPos > 800 && robPos < 1250);
	}
	
////	@Test
//	public void testPauseResume() throws Exception {
//		//TODO!
//	}
//	
//	/**
//	 * On failure:
//	 * - first bean in statPub should be Status.RUNNING
//	 * - last bean in statPub should Status.FAILED and not be 100% complete
//	 * - message with details of failure should be set on bean
//	 * - IPositioner should have received an abort command
//	 */
//	@Test
//	public void testFailure() throws Exception {
//		PositionerAtom failAtom = new PositionerAtom("Error Causer", "BadgerApocalypseButton", "pushed");
//		failAtom.setName("Error Causer");
//		posAtProc = new PositionerAtomProcess<>(failAtom, pti.getPublisher(), false);
//		
//		pti.executeProcess(posAtProc, failAtom);
//		//Fail happens automatically since using MockDev.Serv.
//		pti.waitForBeanFinalStatus(5000l);
//		pti.checkLastBroadcastBeanStatuses(Status.FAILED, false);
//		
//		StatusBean lastBean = pti.getLastBroadcastBean();
//		
//		String req = "Moving device(s) in 'Error Causer' failed with: 'The badger apocalypse cometh! (EXPECTED - we pressed the button...)'";
//		assertEquals("Fail message from IPositioner incorrectly set", req, lastBean.getMessage());
//		assertTrue("IPositioner not aborted", ((MockPositioner)dService.createPositioner()).isAborted());
//	}

}
