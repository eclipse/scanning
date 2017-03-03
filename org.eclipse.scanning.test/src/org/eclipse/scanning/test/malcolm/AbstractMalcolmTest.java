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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.IMalcolmService;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceOperationCancelledException;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
import org.eclipse.scanning.api.malcolm.event.IMalcolmListener;
import org.eclipse.scanning.api.malcolm.event.MalcolmEvent;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.api.malcolm.models.MapMalcolmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMalcolmTest {
	
	// In Mock mode, these come from Java
	// In Real mode they come from the connection to the python server.
	protected IMalcolmService    service;
	protected IMalcolmDevice     device;
	protected IMalcolmConnectorService<MalcolmMessage> connectorService;

	/**
	 * Create the devices and add an @before annotiation
	 * @throws Exception
	 */
	public abstract void create() throws Exception;
	
	/**
	 * Create the devices and add an @before annotiation
	 * @throws Exception
	 */
	public abstract void dispose() throws Exception;
	
	
	
	
	protected static final Logger logger = LoggerFactory.getLogger(AbstractMalcolmTest.class);
	protected final static int IMAGE_COUNT = 5;

		
	protected static final URI PAUSABLE = URI.create("tcp://pausable"); 
 
	
	/**
	 * Create some parameters for configuring the mock connection.
	 * 
	 * You may override this method in case it creates attributes that are not supported for a given
	 * device. The default implementation is to set everything ready for a mock HDF5 write run.
	 * 
	 * @param config
	 * @param configureSleep in ms NOTE That the actual configureSleep value is a double in seconds.
	 * @throws Exception
	 */
	protected void createParameters(Map<String, Object> config, long configureSleep, int imageCount) throws Exception {
				
		// Params for driving mock mode
		config.put("nframes", imageCount); // IMAGE_COUNT images to write
		config.put("shape", new int[]{64,64});
		
		final File temp = File.createTempFile("testingFile", ".hdf5");
		temp.deleteOnExit();
		config.put("file", temp.getAbsolutePath());
		
		// The exposure is in seconds
		config.put("exposure", 0.5);
		
		double csleep = configureSleep/1000d;
		if (configureSleep>0) config.put("configureSleep", csleep); // Sleeps during configure

	}

	protected IMalcolmDevice configure(final IMalcolmDevice device, final int imageCount) throws Exception {
		
	    Map<String, Object> config = new HashMap<String,Object>(2);
	    
		// Test params for starting the device 		
	    createParameters(config, -1, imageCount);
		device.configure(new MapMalcolmModel(config));
	    return device;	
	}
	
	protected IMalcolmDevice configureInThread(final IMalcolmDevice device, final long confSleepTime, int imageNumber, final List<Throwable> exceptions) throws Exception {
		
	    final Map<String, Object> config = new HashMap<String,Object>(2);
	    createParameters(config, confSleepTime, imageNumber);
		
		final Thread runner = new Thread(new Runnable() {
			public void run() {
				try {
					device.configure(new MapMalcolmModel(config));
				} catch (Exception e) {
					e.printStackTrace();
					exceptions.add(e);
				} // blocks until finished
			}
		}, "Malcolm test execution thread");
		
		runner.start();
		
		// We sleep because this is a test
		// which starts a thread running from the same location.
		Thread.sleep(100); // Let it get going.
		// The idea is that using Malcolm will NOT require sleeps like we used to have.
		
		return device;

	}	
	
	protected IMalcolmDevice runDeviceInThread(final IMalcolmDevice device, final List<Throwable> exceptions) throws Exception {
		
		final Thread runner = new Thread(new Runnable() {
			public void run() {
				try {
					device.run(null);
				} catch (Exception e) {
					exceptions.add(e);
				} // blocks until finished
			}
		}, "Malcolm test execution thread");
		runner.start();
		
		// We sleep because this is a test
		// which starts a thread running from the same location.
		Thread.sleep(1000); // Let it get going.
		// The idea is that using Malcolm will NOT require sleeps like we used to have.
				
		logger.debug("Device is "+device.getDeviceState());
		return device;
	}
	
	
	
	protected void createPauseEventListener(IMalcolmDevice device, final List<MalcolmEventBean> beans) {
		
		device.addMalcolmListener(new IMalcolmListener<MalcolmEventBean>() {
			@Override
			public void eventPerformed(MalcolmEvent<MalcolmEventBean> e) {
				MalcolmEventBean bean = e.getBean();
	   			if (bean.getDeviceState()==DeviceState.PAUSED) {
				    beans.add(bean);
				}
			}
		});	
	}
	
	/**
	 * Override to provide alternative connections for tests that look at multiple connections.
	 * @return
	 * @throws Exception
	 */
	protected IMalcolmDevice createAdditionalConnection() throws Exception {
		return null;
	}
	

	protected IMalcolmDevice pause1000ResumeLoop(IMalcolmDevice device, int imageCount, int threadcount, long sleepTime, boolean expectExceptions) throws Throwable {
		return pause1000ResumeLoop(device, imageCount, threadcount, sleepTime, expectExceptions, true, false);
	}

	/**
	 * Pause and resume a number of threads, listen to the events using a topic.
	 * 
	 * @param imageCount
	 * @param threadcount
	 * @param sleepTime
	 * @throws Throwable
	 */
	protected IMalcolmDevice pause1000ResumeLoop(final IMalcolmDevice device, 
			                                     int imageCount, 
			                                     int threadcount, 
			                                     long sleepTime, 
			                                     boolean expectExceptions, 
			                                     boolean doLatch,
			                                     final boolean separateDevice) throws Throwable {

		final List<Throwable> exceptions = new ArrayList<>(1);
		configure(device, imageCount);
		runDeviceInThread(device, exceptions);
		device.addMalcolmListener(new IMalcolmListener<MalcolmEventBean>() {
			@Override
			public void eventPerformed(MalcolmEvent<MalcolmEventBean> e) {
				if (e.getBean().getMessage()!=null) System.out.println(e.getBean().getMessage());
			}
		});
		
        final List<MalcolmEventBean> beans = new ArrayList<MalcolmEventBean>(IMAGE_COUNT);
        createPauseEventListener(device, beans);	
        
        final List<Integer> usedThreads = new ArrayList<>();
        for (int i = 0; i < threadcount; i++) {
        	final Integer current = i;
        	Thread thread = new Thread(new Runnable() {
        		public void run() {
        			try {
        				IMalcolmDevice sdevice = separateDevice ? createAdditionalConnection() : device;
        				System.out.println("Running thread Thread"+current+". Device = "+sdevice.getName());
        				checkPauseResume(sdevice, 1000, true);

        			} catch(MalcolmDeviceOperationCancelledException mdoce) {
        				mdoce.printStackTrace();
        				usedThreads.add(current);
        				exceptions.add(mdoce);

        			} catch (Exception e) {
        				e.printStackTrace();
        				exceptions.add(e);
        			}
        		}
        	}, "Thread"+i);

        	thread.setPriority(9);
        	if (sleepTime>0) {
        		thread.setDaemon(true); // Otherwise we are running them in order anyway
        	}
        	thread.start();
        	System.out.println("Started thread Thread"+i);

        	if (sleepTime>0) {
        		Thread.sleep(sleepTime);
        	} else{
        		Thread.sleep(100);
        		thread.join();
        	}
        }

        if (expectExceptions && exceptions.size()>0) return device; // Pausing failed as expected

        // Wait for end of run for 30 seconds, otherwise we carry on (test will then likely fail)
        if (doLatch && device.getDeviceState()!=DeviceState.IDLE) {
        	device.latch(30, TimeUnit.SECONDS, DeviceState.RUNNING, DeviceState.PAUSED, DeviceState.SEEKING); // Wait until not running.
        }

		if (exceptions.size()>0) throw exceptions.get(0);
		if (doLatch) { // If we waited we can check it completed, otherwise it is probably still going.
			if (device.getDeviceState()!=DeviceState.IDLE) throw new Exception("The state at the end of the pause/resume cycle(s) must be "+DeviceState.IDLE);
			int expectedThreads = usedThreads.size() > 0 ? usedThreads.get(0) : threadcount;
	 		// TODO Sometimes too many pause events come from the real malcolm connection.
			if (beans.size()<expectedThreads) throw new Exception("The pause event was not encountered the correct number of times! Found "+beans.size()+" required "+expectedThreads);
		}
	
	    return device;
	}

	protected synchronized void checkPauseResume(IMalcolmDevice device, long pauseTime, boolean ignoreReady) throws Exception {
		
		
		// No fudgy sleeps allowed in test must be as dataacq would use.
		if (ignoreReady && device.getDeviceState()==DeviceState.READY) return;
		System.out.println("Pausing device in state: "+device.getDeviceState()+" Its locked state is: "+device.isLocked());
		try {
		    device.pause();
		}
		catch (MalcolmDeviceOperationCancelledException mdoce) {
			System.out.println("Pause operation cancelled for thread " + Thread.currentThread().getId());
			throw mdoce;			
		} 
		catch (MalcolmDeviceException mde) {
			System.out.println(mde.getMessage()); // Done so that the previous print line makes sense.
			throw mde;
		}
		System.out.println("Device is "+device.getDeviceState());
		
		if (pauseTime>0) {
			Thread.sleep(pauseTime);
			System.out.println("We waited with device in state "+device.getDeviceState()+" for "+pauseTime);
		}
		
		DeviceState state = device.getDeviceState();
		if (state!=DeviceState.PAUSED) throw new Exception("The state is not paused! It is "+state);

		try {
			device.resume();  // start it going again, non-blocking
		} catch (MalcolmDeviceOperationCancelledException mdoce) {
			System.out.println("Resume operation cancelled for thread " + Thread.currentThread().getId());
			throw mdoce;			
		} 
		System.out.println("Device is resumed state is "+device.getDeviceState());
	}
}
