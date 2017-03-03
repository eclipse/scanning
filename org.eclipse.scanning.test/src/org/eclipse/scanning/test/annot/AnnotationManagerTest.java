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
package org.eclipse.scanning.test.annot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.annotation.scan.AnnotationManager;
import org.eclipse.scanning.api.annotation.scan.LevelComparitor;
import org.eclipse.scanning.api.annotation.scan.LevelEnd;
import org.eclipse.scanning.api.annotation.scan.LevelStart;
import org.eclipse.scanning.api.annotation.scan.PointEnd;
import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.Point;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.points.PointGeneratorService;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * Test
 * 1. Basic counts
 * 2. Inheritance
 * 3. Injected arguments, including services, ScanInformation and IPosition instances.
 * 4. Large call size performance per call cycle.
 * 5. Calling order test (deviceA before deviceB etc.)
 * 
 * @author Matthew Gerring
 *
 */
public class AnnotationManagerTest {
	
	private static IPointGeneratorService pservice;
	
	@BeforeClass
	public static void createGeneratorService() {
		pservice = new PointGeneratorService();
	}

	
	private AnnotationManager      manager;
	
	
	// Test devices
	private SimpleDevice           sdevice;
	private CountingDevice         cdevice;
	private ExtendedCountingDevice edevice;
	private InjectionDevice        idevice;
	private InvalidInjectionDevice invDevice;
	
	@Before
	public void before() throws Exception {
		
		final Map<Class<?>, Object> testServices = new HashMap<>();
		testServices.put(IPointGeneratorService.class,  new PointGeneratorService());
		testServices.put(IScannableDeviceService.class, new MockScannableConnector(null));
		testServices.put(IRunnableDeviceService.class,  new RunnableDeviceServiceImpl((IScannableDeviceService)testServices.get(IScannableDeviceService.class)));
		manager = new AnnotationManager(null, testServices);

		sdevice   = new SimpleDevice();
		cdevice   = new CountingDevice();
		edevice   = new ExtendedCountingDevice();
		idevice   = new InjectionDevice();
		invDevice = new InvalidInjectionDevice();
		manager.addDevices(sdevice, cdevice, edevice, idevice, invDevice);
	}
	
	@After
	public void after() {
		manager.dispose();
	}
	
	@Test
	public void countSimple() throws Exception {
		manager.invoke(ScanStart.class); 
		manager.invoke(ScanStart.class); 
		manager.invoke(ScanStart.class); 
		manager.invoke(ScanStart.class); 
		manager.invoke(ScanStart.class); 
		assertEquals(sdevice.getCount(), 5);
	}
	
	@Test(expected=InvocationTargetException.class)
	public void countConfigureNoScanInfo() throws Exception {
		manager.invoke(PreConfigure.class); 
		assertEquals(1, cdevice.getCount("configure"));
	}

	@Test
	public void countConfigure() throws Exception {
		
		ScanInformation info = new ScanInformation();
		try {
			manager.addContext(info);
			manager.invoke(PreConfigure.class); 
			assertEquals(1, cdevice.getCount("configure"));
		} finally {
			manager.removeContext(info);
		}
	}

	
	@Test
	public void countInherited() throws Exception {
		
		manager.invoke(ScanStart.class); 
		for (int i = 0; i < 5; i++) cycle(i);
		manager.invoke(ScanEnd.class); 
		
		assertEquals(1, cdevice.getCount("prepareVoltages"));
		assertEquals(1, cdevice.getCount("dispose"));
		assertEquals(5, edevice.getCount("prepare"));  // Points done.
		
		assertEquals(1, edevice.getCount("prepareVoltages"));
		assertEquals(1, edevice.getCount("moveToNonObstructingLocation"));
		assertEquals(5, edevice.getCount("prepare"));  // Points done.
		assertEquals(5, edevice.getCount("checkNextMoveLegal"));  // Points done.
		assertEquals(5, edevice.getCount("notifyPosition")); 
		assertEquals(1, edevice.getCount("dispose")); // 
	}
	
	private void cycle(int i) throws Exception {
		manager.invoke(LevelStart.class); 
		manager.invoke(PointStart.class, new Point(i, i*10, i, i*20)); 
		manager.invoke(PointEnd.class, new Point(i, i*10, i, i*20)); 
		manager.invoke(LevelEnd.class); 
	}
	
	@Test
	public void simpleInject() throws Exception {
		
		manager.invoke(PointStart.class, new Point(0, 10, 0, 20)); 
		assertEquals(1, edevice.getPositions().size());
		
		IPosition firstPoint = edevice.getPositions().get(0);
		if (firstPoint==null) throw new Exception("The manager failed to inject a position!");
		assertTrue(firstPoint.equals(new Point(0, 10, 0, 20)));
	}
	
	@Test
	public void scanPointGeneratorInject() throws Exception {
		
		IPointGenerator<GridModel> gen = pservice.createGenerator(new GridModel());
		manager.invoke(PreConfigure.class, gen, new ScanInformation()); 
		assertEquals(gen, idevice.getPointGenerator());
	}

	
	@Test
	public void scanInfoInject() throws Exception {
		
		manager.invoke(ScanStart.class); 
		assertEquals(null, edevice.getScanInformation());
		
		manager.addContext(new ScanInformation());
		manager.invoke(ScanStart.class); 
		assertTrue(edevice.getScanInformation()!=null);
	}

	
	@Test
	public void somePointsInject() throws Exception {
		
		manager.invoke(ScanStart.class); 
		for (int i = 0; i < 5; i++) cycle(i);

		assertEquals(5, edevice.getPositions().size());
		for (IPosition p : edevice.getPositions()) {
			if (p==null) throw new Exception("The manager failed to inject a position!");
		}
		
		assertEquals(1, edevice.getServices().size());
		for (IRunnableDeviceService p : edevice.getServices()) {
			if (p==null) throw new Exception("The manager failed to inject a IRunnableDeviceService!");
		}
		
		manager.invoke(ScanEnd.class); 
		assertEquals(0, edevice.getPositions().size());
	}

	@Test
	public void complexInject() throws Exception {
		
		manager.invoke(PointStart.class, new Point(0, 10, 0, 20));
		checkCalls(1, idevice, "method1");
		checkCalls(1, idevice, "method2");
		checkCalls(1, idevice, "method3");
		checkCalls(1, idevice, "method4");
		checkCalls(1, idevice, "method5");
		checkCalls(1, idevice, "method6");
		
		manager.invoke(ScanEnd.class); 
		checkCalls(0, idevice, "method1");

	}
	
	@Test
	public void complexMultipleInjects() throws Exception {
		
		manager.invoke(ScanStart.class); 
		for (int i = 0; i < 5; i++) cycle(i);

		checkCalls(5, idevice, "method1");
		checkCalls(5, idevice, "method2");
		checkCalls(5, idevice, "method3");
		checkCalls(5, idevice, "method4");
		checkCalls(5, idevice, "method5");
		checkCalls(5, idevice, "method6");
		
		manager.invoke(ScanEnd.class); 
		checkCalls(0, idevice, "method1");

	}
	
	@Test(expected=Exception.class)
	public void checkNoDevicesError() throws Exception {
		AnnotationManager m = new AnnotationManager(null);
		m.addDevices();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void checkRepeatedTypes() throws Exception {
		try {
			AnnotationManager m = new AnnotationManager(null);
	        m.addDevices(new RepeatedTypeDevice());
		} catch(Exception ne) {
			System.out.println(ne.getMessage());
			throw ne;
		}
	}


	private void checkCalls(int size, InjectionDevice device, String methodName) {
		
		if (size<1) {
			assertTrue(device.getArguments(methodName)==null);
			return;
		}
		assertEquals(size, device.getArguments(methodName).size());
		
		Class<?>[] methodClasses = getFirstMethodArgs(device, methodName);
		for (Object[] oa : device.getArguments(methodName)) {
			assertTrue(oa.length>0);
			for (int i = 0; i < oa.length; i++) {
				assertTrue(oa[i]!=null);
				assertTrue(getClasses(oa[i]).contains(methodClasses[i]));
			}
		}
		
	}
	
	
	@Test
	public void invalidComplexInject() throws Exception {
		
		manager.invoke(PointStart.class, new Point(0, 10, 0, 20));
		
		checkCalls(1, invDevice, "validMethod1");
		checkCalls(1, invDevice, "validMethod2");
		
		List<Object[]> oa = invDevice.getArguments("invalidMethod1");
		assertTrue(oa.get(0)[0]==null); // Couldn't find that
		
		oa = invDevice.getArguments("invalidMethod2");
		assertTrue(oa.get(0)[0]==null); // Couldn't find that
		assertTrue(oa.get(0)[1]==null); // Couldn't find that
		assertTrue(oa.get(0)[2]==null); // Couldn't find that

		oa = invDevice.getArguments("invalidMethod3");
		assertTrue(oa.get(0)[0]!=null); 
		assertTrue(oa.get(0)[1]==null); // Couldn't find that

		oa = invDevice.getArguments("invalidMethod4");
		assertTrue(oa.get(0)[0]!=null); 
		assertTrue(oa.get(0)[1]!=null); 
		assertTrue(oa.get(0)[2]==null); // Couldn't find that

		oa = invDevice.getArguments("invalidMethod5");
		assertTrue(oa.get(0)[0]!=null); 
		assertTrue(oa.get(0)[1]==null); // Couldn't find that
		assertTrue(oa.get(0)[2]!=null);
		
		oa = invDevice.getArguments("invalidMethod6");
		assertTrue(oa.get(0)[0]!=null); 
		assertTrue(oa.get(0)[1]!=null); 
		assertTrue(oa.get(0)[2]!=null); 
		assertTrue(oa.get(0)[3]==null); // Couldn't find that
		assertTrue(oa.get(0)[4]==null); // Couldn't find that
		assertTrue(oa.get(0)[5]==null); // Couldn't find that
	
		manager.invoke(ScanEnd.class); 
		checkCalls(0, invDevice, "validMethod1");

	}

	@Test
	public void checkSimpleOrder() throws Exception {
		
		AnnotationManager m = new AnnotationManager(null);
		
		List<OrderedDevice> devices = new ArrayList<>();
		for (int i = 0; i < 100; i++) devices.add(new OrderedDevice("device"+i));
		
		m.addDevices(devices);
		m.invoke(PointStart.class, new Point(0, 10, 0, 20));

		final List<String> orderedNames = devices.stream().map(x -> x.getName()).collect(Collectors.toList());
		final List<String> names = OrderedDevice.getCalledNames();
		
		assertTrue(orderedNames.equals(names));
		
		m.invoke(ScanEnd.class);
	}

	@Test
	public void checkOrderByLevel() throws Exception {
		
		AnnotationManager m = new AnnotationManager(null);
		
		// We add them not by level
		List<OrderedDevice> devices = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			OrderedDevice d = new OrderedDevice("device"+i);
			d.setLevel(i%10); // Devices in different level order...
			devices.add(d);
		}
		
		m.addDevices(devices);
		
		// We invoke them
		m.invoke(PointStart.class, new Point(0, 10, 0, 20));

		// We sort them by level
		Collections.sort(devices, new LevelComparitor());
		final List<String> orderedNames = devices.stream().map(x -> x.getName()).collect(Collectors.toList());
		final List<String> names = OrderedDevice.getCalledNames();
		
		// The called names should have been sorted by level in the first place.
		assertTrue(orderedNames.equals(names));
		
		m.invoke(ScanEnd.class);
	}
	
	@Test
	public void checkOrderByCallThenByLevel() throws Exception {
		
		AnnotationManager m = new AnnotationManager(null);
		
		// We add them not by level
		List<OrderedDevice> fds = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			OrderedDevice d = new OrderedDevice("fd"+i);
			d.setLevel(i%2); // Devices in different level order...
			fds.add(d);
		}
		m.addDevices(fds);
		
		List<OrderedDevice> sds = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			OrderedDevice d = new OrderedDevice("sd"+i);
			d.setLevel(i%2); // Devices in different level order...
			sds.add(d);
		}
		m.addDevices(sds);
	
		// We invoke them
		m.invoke(PointStart.class, new Point(0, 10, 0, 20));

		final List<String> orderedNames = new ArrayList<String>(20);
		Collections.sort(fds, new LevelComparitor());
		orderedNames.addAll(fds.stream().map(x -> x.getName()).collect(Collectors.toList()));
		Collections.sort(sds, new LevelComparitor());
		orderedNames.addAll(sds.stream().map(x -> x.getName()).collect(Collectors.toList()));
		
		final List<String> names = OrderedDevice.getCalledNames();
		assertTrue(orderedNames.equals(names));
		
		m.invoke(ScanEnd.class);
	}
	
	@Test
	public void checkPerformancePerCycle() throws Exception {
		
		final int size = 1000;
		
		AnnotationManager m = new AnnotationManager(null);
		
		// We add them not by level
		for (int i = 0; i < size; i++) {
			ExtendedCountingDevice d = new ExtendedCountingDevice();
			d.setLevel(i%2); // Devices in different level order...
			m.addDevices(d);
		}

		long start = System.currentTimeMillis();
		manager.invoke(ScanStart.class); 
		for (int i = 0; i < size; i++) cycle(i);
		manager.invoke(ScanEnd.class); 
		long end = System.currentTimeMillis();
		
		long time = (end-start)/size;
		System.out.println("Each cycle took "+time+"ms. We ran '"+size+"' devices with '"+size+"' cycles.");
		assertTrue(time<10); // These cycles must be fast
	}
		
	private Class<?>[] getFirstMethodArgs(InjectionDevice device, String methodName) {
		for (Method method : device.getClass().getMethods()) {
			if (method.getName().equals(methodName)) return method.getParameterTypes();
		}
		return null;
	}

	private Collection<Class<?>> getClasses(Object object) {
		
		final Class<?> clazz = object.getClass();
		
		final Collection<Class<?>> classes = new HashSet<>();
		classes.add(clazz);
		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class<?> class1 : interfaces)  classes.add(class1);
		
		// TODO Currently only support one level deep
		classes.add(clazz.getSuperclass());
		interfaces = clazz.getSuperclass().getInterfaces();
		for (Class<?> class1 : interfaces)  classes.add(class1);
		
		return classes;
	}

}
