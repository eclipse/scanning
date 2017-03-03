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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.scanning.api.ILevel;
import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IDisconnectable;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.points.IDeviceDependentIterable;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.AbstractPointsModel;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.IBoundingBoxModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.test.BrokerTest;
import org.eclipse.scanning.test.scan.mock.MockDetectorModel;
import org.junit.Test;

public class AbstractScanTest extends BrokerTest {

	protected IRunnableDeviceService      dservice;
	protected IScannableDeviceService     connector;
	protected IPointGeneratorService      gservice;
	protected IEventService               eservice;

	@Test
	public void testSetSimplePosition() throws Exception {

		IPositioner     pos    = dservice.createPositioner();
		pos.setPosition(new MapPosition("x:0:1, y:0:2"));

		IScannable<Number> x = connector.getScannable("x");
		IScannable<Number> y = connector.getScannable("y");
		Number xpos = x.getPosition();
		Number ypos = y.getPosition();
		
		assertTrue(Math.round(xpos.doubleValue()) == 1d);
		assertTrue(Math.round(ypos.doubleValue()) == 2d);
	}
	
	@Test
	public void testNames() throws ScanningException {
		List<String> names = connector.getScannableNames();
		assertTrue(names.contains("x"));
		assertTrue(names.contains("xNex"));
		assertTrue(names.contains("yNex"));
	}
	
	@Test
	public void testLevels() throws Exception {

		IPositioner     pos    = dservice.createPositioner();
		
		final List<String> scannablesMoved = new ArrayList<>(6);
		pos.addPositionListener(new IPositionListener() {
			@Override
			public void levelPerformed(PositionEvent evt) {
				for (INameable s : evt.getLevelObjects()) scannablesMoved.add(s.getName());
			}
		});
		
		pos.setPosition(new MapPosition("a:0:10, b:0:10, p:0:10, q:0:10, x:0:10, y:0:10"));
		
		assertTrue(scannablesMoved.get(0).equals("a") || scannablesMoved.get(0).equals("b"));
		assertTrue(scannablesMoved.get(1).equals("a") || scannablesMoved.get(1).equals("b"));
		assertTrue(scannablesMoved.get(2).equals("p") || scannablesMoved.get(2).equals("q"));
		assertTrue(scannablesMoved.get(3).equals("p") || scannablesMoved.get(3).equals("q"));
		assertTrue(scannablesMoved.get(4).equals("x") || scannablesMoved.get(4).equals("y"));
		assertTrue(scannablesMoved.get(5).equals("x") || scannablesMoved.get(5).equals("y"));
		
		for (String name : pos.getPosition().getNames()) {
			assertTrue(connector.getScannable(name).getPosition().equals(10d));
		}
	}
	
	@Test
	public void testMassiveMove() throws Exception {

		MapPosition pos = new MapPosition();
		for (int ilevel = 0; ilevel < 100; ilevel++) {
			for (int iscannable = 0; iscannable < 1000; iscannable++) {
				String name = "pos"+ilevel+"_"+iscannable;
				
				// We set the level in this loop, normally this comes
				// in via spring.
				IScannable<?> motor = connector.getScannable(name);
				motor.setLevel(ilevel);
				if (motor instanceof MockScannable) ((MockScannable)motor).setRequireSleep(false);
				
				// We set the position required
				pos.put(name, ilevel+iscannable);
 			}
		} 
		
		IPositioner positioner   = dservice.createPositioner();

		final List<String> levelsMoved = new ArrayList<>(6);
		positioner.addPositionListener(new IPositionListener() {
			@Override
			public void levelPerformed(PositionEvent evt) {
				for (ILevel s : evt.getLevelObjects()) {
					levelsMoved.add(String.valueOf(s.getLevel()));
				}
			}
		});

		long start = System.currentTimeMillis();
		positioner.setPosition(pos);
		long end   = System.currentTimeMillis();
		
		// Check the size
		assertTrue(levelsMoved.size()==100000);
		
		// Check that the level order was right
		final List<String> sorted = new ArrayList<String>(levelsMoved.size());
	    sorted.addAll(levelsMoved);
	    Collections.sort(sorted, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return Integer.parseInt(o1)-Integer.parseInt(o2);
			}
		});
		
	    for (int i = 0; i < levelsMoved.size(); i++) {
		    assertEquals("The wrong level was encountered sorted='"+sorted.get(i)+"' moved='"+levelsMoved.get(i)+"'", levelsMoved.get(i), sorted.get(i));
		}
	    
		System.out.println("Positioning 100,000 test motor with 100 levels took "+(end-start)+" ms");
	}
	
	@Test
	public void testSimpleScan() throws Exception {
				
		IRunnableDevice<ScanModel> scanner = createTestScanner(null, null, null, null, null);
		scanner.run(null);
		checkRun(scanner);
	}
	
	//@Test
	public void testAbortSimpleScan() throws Exception {
				
		IRunnableDevice<ScanModel> scanner = createTestScanner(null, null, null, null, null);
		scanner.start(null);
		Thread.sleep(100);
		scanner.abort();
		Thread.sleep(100);
		assertTrue("The Device state was "+scanner.getDeviceState()+" not "+DeviceState.ABORTED, scanner.getDeviceState()==DeviceState.ABORTED);
	}
	
	@Test
	public void testThreadCount() throws Exception {
			
		IRunnableDevice<ScanModel> scanner = createTestScanner(null, null, null, null, null);
		int before = Thread.activeCount();
		scanner.run(null);
		if (connector instanceof IDisconnectable) ((IDisconnectable)connector).disconnect();
		Thread.sleep(25); // Just allows any threads no longer required to exit.
		int after = Thread.activeCount();
		System.out.println("Before = "+before);
		System.out.println("After  = "+after);
		int tolerance = 3;
		System.out.println("Tolerance  = "+tolerance);
		if (after>before+tolerance) throw new Exception("too many extra threads after scan! Expected not more than "+(before+tolerance)+" got "+after);
	}

	
	@Test
	public void testStepScan() throws Exception {
		
		StepModel model = new StepModel();
		model.setStart(0);
		model.setStop(100);
		model.setStep(1);
		model.setName("myScannable");
		
		IRunnableDevice<ScanModel> scanner = createTestScanner(model, null, null, null, null);
		scanner.run(null);
		checkRun(scanner);
	}
	
	@Test
	public void testInvalidStepScan() throws Exception {

		StepModel model = new StepModel();
		model.setStart(0);
		model.setStop(10);
		model.setStep(-1);
		model.setName("myScannable");
		
		try {
			IRunnableDevice<ScanModel> scanner = createTestScanner(model, null, null, null, null);
			
			// Cast to AbstractRunnableDevice gives us non-blocking .start() method.
			((AbstractRunnableDevice<ScanModel>) scanner).start(null);
			
			Thread.sleep(5000);  // testStepScan (the valid one) takes ~2 seconds total.
			
		} catch (Exception ex) {
			assertEquals(ScanningException.class, ex.getClass());
			assertEquals(ModelValidationException.class, ex.getCause().getClass());
			assertTrue(ex.getCause().getMessage().toLowerCase().indexOf("wrong direction")>0);
			return;
		}
		
		throw new Exception("Scanner failed to throw an exception.");
	}

	@Test
	public void testZeroStepStepScan() throws Exception {

		StepModel model = new StepModel();
		model.setStart(0);
		model.setStop(10);
		model.setStep(0);
		model.setName("myScannable");
		
		try {
			IRunnableDevice<ScanModel> scanner = createTestScanner(model, null, null, null, null);
			
			// Cast to AbstractRunnableDevice gives us non-blocking .start() method.
			((AbstractRunnableDevice<ScanModel>) scanner).start(null);
			
			Thread.sleep(5000);  // testStepScan (the valid one) takes ~2 seconds total.
			
		} catch (Exception ex) {
			assertEquals(ScanningException.class, ex.getClass());
			assertEquals(ModelValidationException.class, ex.getCause().getClass());
			assertEquals("Model step size must be nonzero!", ex.getCause().getMessage());
			return;
		}
		
		throw new Exception("Scanner failed to throw an exception.");
	}
	
	
	@Test
	public void testScanError() throws Exception {
		
		// 1. Set the model to make the detector throw an exception
		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setExposureTime(0.001);
		dmodel.setAbortCount(3); // Aborts on the third write call by throwing an exception
		IWritableDetector<MockDetectorModel> detector = (IWritableDetector<MockDetectorModel>)dservice.createRunnableDevice(dmodel);
		
		// 2. Check run fails and check exception is that which the detector provided
		// Not some horrible reflection one.
		IRunnableDevice<ScanModel> scanner = createTestScanner(null, null, null, null, detector);
		boolean ok=false;
		try {
		    scanner.run(null);
		} catch (ScanningException expected) {
			if (!expected.getMessage().equals("The detector had a problem running! This exception should stop the scan running!")) {
				if (expected.getCause()!=null) expected.getCause().printStackTrace();
				throw new Exception("Expected the precise message from the mock detector not to be lost but it was! It was '"+expected.getMessage()+"'");
			}
			ok=true;
		}
		if (!ok) throw new Exception("The exception was not thrown by the scan as expected!");
		
		// 3. Check that it died after 3 and it is FAULT
		assertEquals(3, dmodel.getRan());
		assertTrue(scanner.getDeviceState()==DeviceState.FAULT);
		
		// 4. Check that running it again fails
		ok=false;
		try {
			scanner.run(null);
		} catch (ScanningException expected) {
			ok=true;
		}
		if (!ok) throw new Exception("The exception was not thrown by the scan as expected!");
		
		// 5. Reset everything and see if can run ok
		scanner.reset();
		
		// Put the model back for the test and reconfigure
		dmodel.setRan(0);
		dmodel.setWritten(0);
		dmodel.setAbortCount(-1); // Solves the problem with the detector!
		detector.configure(dmodel);
		
		// Reconfigure - might need better way of doing?
		AbstractRunnableDevice<ScanModel> ascanner = (AbstractRunnableDevice<ScanModel>)scanner;
		scanner.configure(ascanner.getModel());
 		
		// Run again, should be ok.
		scanner.run(null);
		checkRun(scanner);
	}


	@Test
	public void testSimpleScanWithStatus() throws Exception {
			
		
		final ScanBean bean = new ScanBean();
		bean.setName("Fred");
		bean.setUniqueId("fred");
		
		// Use in memory broker removes requirement on network and external ActiveMQ process
		// http://activemq.apache.org/how-to-unit-test-jms-code.html
		final IPublisher<ScanBean> publisher = eservice.createPublisher(uri, IEventService.STATUS_TOPIC);
		
		final ISubscriber<IScanListener> subscriber = eservice.createSubscriber(uri, IEventService.STATUS_TOPIC);
		final List<ScanBean>    events = new ArrayList<ScanBean>(11);
		final List<DeviceState> states = new ArrayList<DeviceState>(11);
		subscriber.addListener(new IScanListener() {		
			@Override
			public void scanStateChanged(ScanEvent evt) {
				states.add(evt.getBean().getDeviceState());
			}
			@Override
			public void scanEventPerformed(ScanEvent evt) {
				events.add(evt.getBean());
//				System.out.println("State : "+evt.getBean().getDeviceState());
//				System.out.println("Percent complete : "+evt.getBean().getPercentComplete());
//				System.out.println(evt.getBean().getPosition());
			}
		});
		
		try {
			
			// Create a scan and run it without publishing events
			IRunnableDevice<ScanModel> scanner = createTestScanner(null, bean, publisher, null, null);
			scanner.run(null);
			
			Thread.sleep(100); // Wait for all events to make it over from ActiveMQ
			
			checkRun(scanner);
			
			// Bit of a hack to get the generator from the model - should this be easier?
			IPointGenerator<?> gen = (IPointGenerator<?>)((ScanModel)((AbstractRunnableDevice)scanner).getModel()).getPositionIterable();
			assertEquals(gen.size(), events.size());
			assertEquals(Arrays.asList(DeviceState.CONFIGURING, DeviceState.READY, DeviceState.RUNNING, DeviceState.READY), states);
			
			for (ScanBean b : events) assertEquals("fred", b.getUniqueId());
		
		} finally {
			publisher.disconnect();
		}
	}

	@Test
	public void testSimpleScanSetPositionCalls() throws Exception {
			
		IScannable<Number> p = connector.getScannable("p");
		IRunnableDevice<ScanModel> scanner = createTestScanner(null, null, null, null, null, new String[]{"p","q"});
		
		scanner.run(null);
		
		checkRun(scanner);
		
		// NOTE Did with Mockito but caused dependency issues.
		MockScannable ms = (MockScannable)p;
		ms.verify(0.3, new Point("p",0,0.3, "q",0,0.3));
		ms.verify(0.3, new Point("p",0,0.3, "q",2,1.5));
		ms.verify(1.5, new Point("p",2,1.5, "q",0,0.3));
		ms.verify(1.5, new Point("p",2,1.5, "q",2,1.5));
	}
	
	@Test
	public void testSimpleScanWithMonitor() throws Exception {
			
		IScannable<Number> monitor = connector.getScannable("monitor");
		IRunnableDevice<ScanModel> scanner = createTestScanner(null, null, null, monitor, null);
		
		scanner.run(null);
		
		checkRun(scanner);
		
		// NOTE Did with Mockito but caused dependency issues.
		MockScannable ms = (MockScannable)monitor;
		ms.verify(null, new Point(0,0.3,0,0.3));
		ms.verify(null, new Point(0,0.3,2,1.5));
		ms.verify(null, new Point(2,1.5,0,0.3));
		ms.verify(null, new Point(2,1.5,2,1.5));
	}
	
	private void checkRun(IRunnableDevice<ScanModel> scanner) throws Exception {
		// Bit of a hack to get the generator from the model - should this be easier?
		// Do not copy this code
		ScanModel smodel = (ScanModel)((AbstractRunnableDevice)scanner).getModel();
		IPointGenerator<?> gen = (IPointGenerator<?>)smodel.getPositionIterable();
		MockDetectorModel dmodel = (MockDetectorModel)((AbstractRunnableDevice)smodel.getDetectors().get(0)).getModel();
		assertEquals(gen.size(), dmodel.getRan());
		assertEquals(0, dmodel.getWritten()); // write() not called as no nexus file set
	}

	private IRunnableDevice<ScanModel> createTestScanner(AbstractPointsModel pmodel,
			final ScanBean bean,
			final IPublisher<ScanBean> publisher,
			IScannable<?> monitor,
			IRunnableDevice<MockDetectorModel> detector) throws Exception {
		return createTestScanner(pmodel, bean, publisher, monitor, detector, null);
	}

	private IRunnableDevice<ScanModel> createTestScanner(AbstractPointsModel pmodel,
														final ScanBean bean,
														final IPublisher<ScanBean> publisher,
														IScannable<?> monitor,
														IRunnableDevice<MockDetectorModel> detector,
														String[] axes) throws Exception {
		
		// Configure a detector with a collection time.
		if (detector == null) {
			MockDetectorModel dmodel = new MockDetectorModel();
			dmodel.setExposureTime(0.001);
			dmodel.setName("detector");
			detector = dservice.createRunnableDevice(dmodel);
		}
		
		// If none passed, create scan points for a grid.
		if (pmodel == null) {
			pmodel = new GridModel("x", "y");
			((GridModel) pmodel).setSlowAxisPoints(5);
			((GridModel) pmodel).setFastAxisPoints(5);
			((GridModel) pmodel).setBoundingBox(new BoundingBox(0,0,3,3));
		}
		
		if (axes!=null && pmodel instanceof IBoundingBoxModel) {
			IBoundingBoxModel bmodel = (IBoundingBoxModel)pmodel;
			bmodel.setFastAxisName(axes[0]);
			bmodel.setSlowAxisName(axes[1]);
		}
		
		IPointGenerator<?> gen = gservice.createGenerator(pmodel);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(detector);
		smodel.setBean(bean);
		if (monitor!=null) smodel.setMonitors(monitor);
		
		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, publisher);
		return scanner;
	}
	
	/**
	 * This test creates a generator which pauses on the next() call.
	 * This simulates motors moving and checks that nothing in the 
	 * scanning times out if this happens.
	 * 
	 * http://jira.diamond.ac.uk/browse/DAQ-150
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGeneratorWhichMimiksHardwareOperation() throws Exception {

		final PausingIterable iterable = new PausingIterable(5);
		
		MockDetectorModel dmodel = new MockDetectorModel();
		dmodel.setExposureTime(0.001);
		dmodel.setName("detector");
		IRunnableDevice<MockDetectorModel> detector = dservice.createRunnableDevice(dmodel);

		
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(iterable);
		smodel.setDetectors(detector);
		smodel.setBean(new ScanBean());
		
		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel);
		scanner.run(null);

		// Check that using IDeviceDependentIterable, five means six - the additional call is done in ScanEstimator to get the scan rank
		assertEquals("The iterator should be asked "+iterable.size()+" times for position and it was asked "+iterable.getTotalPositions(),
				    iterable.size + 1, iterable.getTotalPositions());
	}

	private class PausingIterable implements Iterable<IPosition>, IDeviceDependentIterable {
		
		private int totalPositions = 0;
		private int size;

		public PausingIterable(int i) {
			this.size = i;
		}

		@Override
		public Iterator<IPosition> iterator() {
			return new Iterator<IPosition>() {

				private int count = 0;
				@Override
				public boolean hasNext() {
					return count<size;
				}

				@Override
				public IPosition next() {
					MapPosition next = new MapPosition();
					next.put("x", count);
					next.put("y", count);
					++count;
					++totalPositions;

					System.out.println("Next position is "+next);
					if (count%2==0) {
						long amount = count*100;
						System.out.println("Sleeping for "+amount+" while we get to it.");
						try {
							Thread.sleep(amount); // 200, 400
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					return next;
				}

			};
		}

		public int getTotalPositions() {
			return totalPositions;
		}

		@Override
		public int size() {
			return size;
		}
		
		@Override
		public List<String> getScannableNames() {
			return Arrays.asList("x", "y");
		}
	} 

}
