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
package org.eclipse.scanning.test.scan;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.device.IPausableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceOperationCancelledException;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.eclipse.scanning.test.scan.mock.MockWritableDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandelbrotDetector;
import org.eclipse.scanning.test.scan.mock.MockWritingMandlebrotModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThreadScanTest extends BrokerTest {
	
	private IRunnableDeviceService           sservice;
	private IScannableDeviceService    connector;
	private IPointGeneratorService          gservice;
	private IEventService              eservice;
	private ISubscriber<IScanListener> subscriber;
	private IPublisher<ScanBean>       publisher;

	protected final static int IMAGE_COUNT = 5;

	@Before
	public void setup() throws Exception {
		
		setUpNonOSGIActivemqMarshaller();
		eservice   = new EventServiceImpl(new ActivemqConnectorService());
		
		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		connector = new MockScannableConnector(eservice.createPublisher(uri, EventConstants.POSITION_TOPIC));
		sservice  = new RunnableDeviceServiceImpl(connector);
		RunnableDeviceServiceImpl impl = (RunnableDeviceServiceImpl)sservice;
		impl._register(MockDetectorModel.class, MockWritableDetector.class);
		impl._register(MockWritingMandlebrotModel.class, MockWritingMandelbrotDetector.class);
		
		gservice  = new PointGeneratorService();

		// Use in memory broker removes requirement on network and external ActiveMQ process
		// http://activemq.apache.org/how-to-unit-test-jms-code.html
		subscriber = eservice.createSubscriber(uri, IEventService.SCAN_TOPIC); // Create an in memory consumer of messages.
		publisher  = eservice.createPublisher(uri, IEventService.SCAN_TOPIC);
	}
	
	@After
	public void shutdown() throws Exception {
		subscriber.disconnect();
		publisher.disconnect();
	}
	
	@Test
	public void testPauseAndResume2Threads() throws Throwable {
		
		IPausableDevice<?> device = createConfiguredDevice(5, 5);
		pause1000ResumeLoop(device, 2, 2000, false);
	}
	
	@Test
	public void testPauseAndResume10Threads() throws Throwable {
		
		IPausableDevice<?> device = createConfiguredDevice(10, 10);
		pause1000ResumeLoop(device, 10, 2000, false);
	}


	protected IRunnableDevice<?> pause1000ResumeLoop(final IPausableDevice<?> device, 
													 int threadcount, 
													 long sleepTime, 
													 boolean expectExceptions) throws Throwable {

		final List<Throwable> exceptions = new ArrayList<>(1);
		runDeviceInThread(device, exceptions);
		
		subscriber.addListener(new IScanListener() {
			@Override
			public void scanEventPerformed(ScanEvent e) {
			    if (e.getBean().getMessage()!=null) System.out.println(e.getBean().getMessage());
			}
		});

		final List<ScanBean> beans = new ArrayList<ScanBean>(IMAGE_COUNT);
		createPauseEventListener(device, beans);	

		final List<Integer> usedThreads = new ArrayList<>();
		for (int i = 0; i < threadcount; i++) {
			final Integer current = i;
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						System.out.println("Running thread Thread"+current+". Device = "+device.getName());
						checkPauseResume(device, 1000, true);

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

		if (expectExceptions && exceptions.size()>0) {
			for (Throwable ne : exceptions) {
				ne.printStackTrace();
			}
			return device; // Pausing failed as expected
		}

		// Wait for end of run for 30 seconds, otherwise we carry on (test will then likely fail)
		if (device.getDeviceState()!=DeviceState.READY) {
			latch(30, TimeUnit.SECONDS, DeviceState.RUNNING, DeviceState.PAUSED, DeviceState.SEEKING); // Wait until not running.
		}

		if (exceptions.size()>0) throw exceptions.get(0);
		
		if (device.getDeviceState()!=DeviceState.READY) throw new Exception("The state at the end of the pause/resume cycle(s) must be "+DeviceState.READY+" not "+device.getDeviceState());
		int expectedThreads = usedThreads.size() > 0 ? usedThreads.get(0) : threadcount;
		// TODO Sometimes too many pause events come from the real malcolm connection.
		if (beans.size()<expectedThreads) throw new Exception("The pause event was not encountered the correct number of times! Found "+beans.size()+" required "+expectedThreads);

		return device;
	}
	
	
	private void latch(long time, TimeUnit unit, final DeviceState... invalids) throws Exception {
		
		final CountDownLatch latch = new CountDownLatch(1);
		// Use in memory broker removes requirement on network and external ActiveMQ process
		// http://activemq.apache.org/how-to-unit-test-jms-code.html
		ISubscriber<IScanListener> subscriber = eservice.createSubscriber(uri, IEventService.SCAN_TOPIC); // Create an in memory consumer of messages.
		subscriber.addListener(new IScanListener() {
			@Override
			public void scanStateChanged(ScanEvent evt) {
				ScanBean bean = evt.getBean();
	   			if (!Arrays.asList(invalids).contains(bean.getDeviceState())) {
	   				latch.countDown();
	   			}
			}
		});
		latch.await(time, unit);
	}


	protected void createPauseEventListener(IRunnableDevice<?> device, final List<ScanBean> beans) throws EventException, URISyntaxException {
		
		subscriber.addListener(new IScanListener() {
			@Override
			public void scanStateChanged(ScanEvent evt) {
				ScanBean bean = evt.getBean();
	   			if (bean.getDeviceState()==DeviceState.PAUSED) {
				    beans.add(bean);
				}
			}
		});	
	}

	private IPausableDevice<?> createConfiguredDevice(int rows, int columns) throws ScanningException, GeneratorException, URISyntaxException {
		
		// Configure a detector with a collection time.
		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setExposureTime(0.1);
		dmodel.setName("detector");
		IRunnableDevice<MockDetectorModel> detector = sservice.createRunnableDevice(dmodel);
		
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel();
		gmodel.setSlowAxisPoints(rows);
		gmodel.setFastAxisPoints(columns);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));	
		IPointGenerator<?> gen = gservice.createGenerator(gmodel);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		
		// Create a scan and run it
		IPausableDevice<ScanModel> scanner = (IPausableDevice<ScanModel>) sservice.createRunnableDevice(smodel, publisher);
		return scanner;
	}


	protected IRunnableDevice<?> runDeviceInThread(final IRunnableDevice<?> device, final List<Throwable> exceptions) throws Exception {
		
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
				
		return device;
	}
	
	protected synchronized void checkPauseResume(IPausableDevice<?> device, long pauseTime, boolean ignoreReady) throws Exception {
		
		
		// No fudgy sleeps allowed in test must be as dataacq would use.
		if (ignoreReady && device.getDeviceState()==DeviceState.READY) return;
		System.out.println("Pausing device in state: "+device.getDeviceState());
		
		device.pause();
		System.out.println("Device is "+device.getDeviceState());
		
		if (pauseTime>0) {
			Thread.sleep(pauseTime);
			System.out.println("We waited with for "+pauseTime+" device is now in state "+device.getDeviceState());
		}
		
		DeviceState state = device.getDeviceState();
		if (state!=DeviceState.PAUSED) throw new Exception("The state is not paused!");

		device.resume();  // start it going again, non-blocking

		Thread.sleep(100);
		System.out.println("Device is resumed state is "+device.getDeviceState());
	}

}
