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
import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.hdf5.nexus.NexusFileFactoryHDF5;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.processes.MonitorAtomProcess;
import org.eclipse.scanning.example.file.MockFilePathService;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MonitorAtomProcessTest {

	//Infrastructure
	private ProcessTestInfrastructure pti;

	@Before
	public void setUp() throws EventException {

		pti = new ProcessTestInfrastructure(750);

		ServicesHolder.setNexusFileFactory(new NexusFileFactoryHDF5());
		ServicesHolder.setScannableDeviceService(new MockScannableConnector(null));
		ServicesHolder.setFilePathService(new MockFilePathService());
	}

	@After
	public void tearDown() {
		ServicesHolder.setNexusFileFactory(null);
		ServicesHolder.setScannableDeviceService(null);
		ServicesHolder.setFilePathService(null);

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
		MonitorAtom monAt = new MonitorAtom("getTC1", "thermocouple1");
		monAt.setName("Monitor thermocouple1");
		MonitorAtomProcess<Queueable> monAtProc = new MonitorAtomProcess<>(monAt, pti.getPublisher(), false);

		pti.executeProcess(monAtProc, monAt);
		pti.waitForExecutionEnd(10000l);
		pti.checkLastBroadcastBeanStatuses(Status.COMPLETE, false);

		assertEquals("Incorrect message after execute", "Successfully stored current value of 'thermocouple1'", pti.getLastBroadcastBean().getMessage());

		assertEquals(monAt.getRunDirectory(), new File(monAt.getFilePath()).getParent());

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
	 */
//	@Ignore("Intermittent failures on Travis.") //TODO 05.07.2017 Stability improved on local machine
	@Test
	public void testTermination() throws Exception {
		MonitorAtom monAt = new MonitorAtom("getTC1", "thermocouple1");
		monAt.setName("Monitor thermocouple1");
		MonitorAtomProcess<Queueable> monAtProc = new MonitorAtomProcess<>(monAt, pti.getPublisher(), false);

		pti.executeProcess(monAtProc, monAt,false, false);
		pti.waitToTerminate(2l);
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.TERMINATED, false);

		Thread.sleep(100);
//		TODO This is probably not a good way to test this as we can't guarantee what stage the terminate happens at
//		assertEquals("Incorrect message after terminate", "Get value of 'thermocouple1' aborted (requested)", pti.getLastBroadcastBean().getMessage());
		//Get the filepath set for the monitor output and check it does not exist
		MonitorAtom termAt = (MonitorAtom)pti.getLastBroadcastBean();
		if (termAt.getFilePath() != null) {
			assertFalse("Nexus file not deleted during cleanup", new File(termAt.getFilePath()).exists());
		}
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
		MonitorAtom failAtom = new MonitorAtom("error", null);
		failAtom.setName("Error Causer");
		MonitorAtomProcess<Queueable> monAtProc = new MonitorAtomProcess<>(failAtom, pti.getPublisher(), false);

		pti.executeProcess(monAtProc, failAtom);
		//Fail happens automatically since using MockDev.Serv.
		pti.waitForBeanFinalStatus(5000l);
		pti.checkLastBroadcastBeanStatuses(Status.FAILED, false);

		StatusBean lastBean = pti.getLastBroadcastBean();

//TODO Tried to do this elegantly. If doesn't work - throws a SecurityException due to incorrect signing of hamcrest matcher classes. Works fine locally; fails on travis
//		assertThat("Fail message is wrong", lastBean.getMessage(), anyOf(equalTo("Processing MonitorAtom 'Error Causer' failed with: 'Failed to get monitor with the name 'null''"),
//				equalTo("Failed to get monitor with the name 'null'")));
		if (!(lastBean.getMessage().equals("Processing MonitorAtom 'Error Causer' failed with: 'Failed to get monitor with the name 'null''") ||
				lastBean.getMessage().equals("Failed to get monitor with the name 'null'"))) {
			fail("Fail message is wrong");
		}

	}

}
