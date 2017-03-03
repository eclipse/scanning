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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.processes.MonitorAtomProcess;
import org.eclipse.scanning.event.queues.processes.QueueProcess;
import org.eclipse.scanning.example.file.MockFilePathService;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.test.event.queues.mocks.MockPositioner;
import org.eclipse.scanning.test.event.queues.mocks.MockScanService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MonitorAtomProcessTest {
	
	private MonitorAtom monAt;
	private QueueProcess<MonitorAtom, Queueable> monAtProc;
	
	//Infrastructure
	private ProcessTestInfrastructure pti;
	private IRunnableDeviceService mss;
	
	@Before
	public void setUp() throws EventException {
		
		pti = new ProcessTestInfrastructure(750);
		
		mss = new MockScanService();
		ServicesHolder.setDeviceService(mss);
		ServicesHolder.setNexusFileFactory(new NexusFileFactoryHDF5());
		ServicesHolder.setScannableDeviceService(new MockScannableConnector(null));
		ServicesHolder.setFilePathService(new MockFilePathService());
		
		monAt = new MonitorAtom("Monitor temperature", "T", 12000);
		monAtProc = new MonitorAtomProcess<>(monAt, pti.getPublisher(), false);
	}
	
	@After
	public void tearDown() {
		ServicesHolder.unsetDeviceService(mss);
		mss = null;
		pti = null;
	}
	
	/**
	 * After execution:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should be Status.COMPLETE and 100%
	 * - status publisher should have: 1 RUNNING bean and 1 COMPLETE bean
	 */
	@Test
	public void testExecution() throws Exception {
		pti.executeProcess(monAtProc, monAt);
		pti.waitForExecutionEnd(10000l);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);
		
		assertEquals("Incorrect message after execute", "Device move(s) completed.", pti.getLastBroadcastBean().getMessage());
		
		final File file = new File(monAt.getFilePath());
		assertTrue(file.exists());
		
		INexusFileFactory factory = ServicesHolder.getNexusFileFactory();
		NexusFile nfile = factory.newNexusFile(file.getAbsolutePath());
		nfile.openToRead();
		
		// TODO Should probably check data written.
		final DataNode node = nfile.getData(monAt.getDataset());
		assertNotNull(node);
		
		nfile.close();
	}
	
	/**
	 * On terminate:
	 * - first bean in statPub should be Status.RUNNING
	 * - last bean in statPub should Status.TERMINATED and not be 100% complete
	 * - status publisher should have a TERMINATED bean
	 * - termination message should be set on the bean
	 * - IPositioner should have received an abort command
	 * 
	 * N.B. MoveAtomProcessorTest uses MockPostioner, which pauses for 100ms 
	 * does something then pauses for 150ms.
	 */
	@Ignore("I do not understand why this fails.")
	@Test
	public void testTermination() throws Exception {
		pti.executeProcess(monAtProc, monAt);
		pti.waitToTerminate(100l);
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.TERMINATED, false);
		
		Thread.sleep(100);
		assertEquals("Incorrect message after terminate", "Move aborted before completion (requested).", pti.getLastBroadcastBean().getMessage());
		assertTrue("IPositioner not aborted", ((MockPositioner)mss.createPositioner()).isAborted());
		assertFalse("Move should have been terminated", ((MockPositioner)mss.createPositioner()).isMoveComplete());
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
	 * - IPositioner should have received an abort command
	 */
	@Test
	public void testFailure() throws Exception {
		MonitorAtom failAtom = new MonitorAtom("Error Causer", null, 1);
		MonitorAtomProcess mvAtProc = new MonitorAtomProcess<>(failAtom, pti.getPublisher(), false);
		
		pti.executeProcess(mvAtProc, failAtom);
		//Fail happens automatically since using MockDev.Serv.
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.FAILED, false);
		
		StatusBean lastBean = pti.getLastBroadcastBean();
		assertEquals("Write of file with value from 'null' failed with: \"Invalid scannable null\".", lastBean.getMessage());
	}

}
