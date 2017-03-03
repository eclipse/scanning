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
package org.eclipse.scanning.test.malcolm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceOperationCancelledException;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(Parameterized.class)
public abstract class AbstractPausingMalcolmTest extends AbstractMalcolmTest {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractMalcolmTest.class);
	
	private static final int REPEAT_COUNT = 1;
	
	@Parameterized.Parameters
	public static List<Object[]> data() {
	    return Arrays.asList(new Object[REPEAT_COUNT][0]);
	}
	
	@Test
	public void testBasicPauseAndResume() throws Throwable {

		final List<Throwable> exceptions = new ArrayList<>(1);
		configure(device, 10);
		runDeviceInThread(device, exceptions);		
		checkPauseResume(device, -1, false);
		device.latch(-1, TimeUnit.SECONDS, DeviceState.RUNNING);  // Wait while finishes running
		
		if (exceptions.size()>0) throw exceptions.get(0);
		if (!device.getDeviceState().isBeforeRun()) throw new Exception("Problem with state at end of test! "+device.getDeviceState());
	}
	
	@Test
	public void testBasicPauseAndResumeWithEvent() throws Throwable {

		final List<Throwable> exceptions = new ArrayList<>(1);
		configure(device, 10);
		runDeviceInThread(device, exceptions);		
		
		// We add a listener to the live run, it's ok because we have not paused yet
		// and we are testing pause events.
		final List<MalcolmEventBean> beans = new ArrayList<MalcolmEventBean>(IMAGE_COUNT);
		createPauseEventListener(device, beans);
		
		checkPauseResume(device, -1, false);
		device.latch(-1, TimeUnit.SECONDS, DeviceState.RUNNING);  // Wait while running
		
		if (beans.size()!=1) throw new Exception("The pause event was not encountered!");
		if (exceptions.size()>0) throw exceptions.get(0);
		if (!device.getDeviceState().isBeforeRun()) throw new Exception("Problem with state at end of test! "+device.getDeviceState());
	}

	@Test
	public void testBasicPauseAndResumeWithTopic() throws Throwable {

		final List<Throwable> exceptions = new ArrayList<>(1);
		configure(device, 10);
		runDeviceInThread(device,exceptions);
		
        final List<MalcolmEventBean> beans = new ArrayList<MalcolmEventBean>(IMAGE_COUNT);
        createPauseEventListener(device, beans);	
        checkPauseResume(device, -1, false);
        device.latch(-1, TimeUnit.SECONDS, DeviceState.RUNNING);  // Wait while running
		
		if (beans.size()!=1) throw new Exception("The pause event was not encountered!");
		if (exceptions.size()>0) throw exceptions.get(0);
		if (!device.getDeviceState().isBeforeRun()) throw new Exception("Problem with state at end of test! "+device.getDeviceState());
	}


	@Test
	public void testBasicPauseAndResumeWithWait() throws Throwable {

		final List<Throwable> exceptions = new ArrayList<>(1);
		configure(device, 10);
		runDeviceInThread(device,exceptions);		
		checkPauseResume(device, 5000, false);
		device.latch(10, TimeUnit.SECONDS, DeviceState.RUNNING); // Wait while running, but not longer than 10-seconds
		
		if (exceptions.size()>0) throw exceptions.get(0);
		if (!device.getDeviceState().isBeforeRun()) throw new Exception("Problem with state at end of test! "+device.getDeviceState());
	}
	
	

	// We don't care that much about multi-threaded pause but it
	// helps find unexpected deadlocks with the system.
	@Test
	public void testPauseAndResume2Threads() throws Throwable {
		logger.debug("\ntestPauseAndResume2Threads");
		pause1000ResumeLoop(device, 5, 2, 2000, false);
	}
	// We don't care that much about multi-threaded pause but it
	// helps find unexpected deadlocks with the system.
//	@Test
//	public void testPauseAndResume2ThreadsJoined() throws Throwable {
//		logger.debug("\ntestPauseAndResume2ThreadsJoined");
//		pause1000ResumeLoop(device, 5, 2, -1, false);
//	}
	
	// We don't care that much about multi-threaded pause but it
	// helps find unexpected deadlocks with the system.
	@Test
	public void testPauseAndResume2ThreadsSeparateDevices() throws Throwable {
		logger.debug("\ntestPauseAndResume2ThreadsSeparateDevices");
		pause1000ResumeLoop(device, 5, 2, 2000, false, true, true);
	}
	// We don't care that much about multi-threaded pause but it
	// helps find unexpected deadlocks with the system.
	@Test
	public void testPauseAndResume2ThreadsJoinedSeparateDevices() throws Throwable {
		logger.debug("\ntestPauseAndResume2ThreadsJoinedSeparateDevices");
		pause1000ResumeLoop(device, 5, 2, -1, false, true, true);
	}

	
	// We don't care that much about multi-threaded pause but it
	// helps find unexpected deadlocks with the system.
	@Test
	public void testPauseAndResumeMultiThreadsStacked() throws Throwable {
		logger.debug("\ntestPauseAndResumeMultiThreadsStacked");
		pause1000ResumeLoop(device, 25, 10, 500, false); // Tweaking this to see how robust threading is...
	}
	
	// We don't care that much about multi-threaded pause but it
	// helps find unexpected deadlocks with the system.
	@Test
	public void testPauseAndResumeMultiThreadsJoined() throws Throwable {
		logger.debug("\ntestPauseAndResumeMultiThreadsJoined");
		pause1000ResumeLoop(device, 25, 10, -1, false); // Tweaking this to see how robust threading is...
	}
	
	@Test
	public void testPauseAndResumeMultiThreadsJoinedExceptionsExpected() throws Throwable {
		logger.debug("\ntestPauseAndResumeMultiThreadsJoinedExceptionsExpected");
		pause1000ResumeLoop(device, 5, 10, -1, true); // Tweaking this to see how robust threading is...
	}
}
