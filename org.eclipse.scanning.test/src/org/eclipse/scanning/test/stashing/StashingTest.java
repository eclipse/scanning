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
package org.eclipse.scanning.test.stashing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dawnsci.analysis.api.persistence.IMarshallerService;
import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.scan.SampleData;
import org.eclipse.scanning.api.scan.AxisConfiguration;
import org.eclipse.scanning.api.stashing.IStashing;
import org.eclipse.scanning.api.stashing.IStashingService;
import org.eclipse.scanning.device.ui.ServiceHolder;
import org.eclipse.scanning.device.ui.util.StashingService;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

public class StashingTest {


	private IStashing stash;
	
	@BeforeClass
	public static void marshalling() {
		
		IMarshallerService marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(), new ScanningExampleClassRegistry(), new ScanningTestClassRegistry(), new TestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
		);
		ActivemqConnectorService.setJsonMarshaller(marshaller);
		ServiceHolder.setMarshallerService(marshaller);
		
		ServiceHolder.setStashingService(new StashingService());
	}

	@Before
	public void createStash() {
		IStashingService stashingService = ServiceHolder.getStashingService();
		this.stash = stashingService.createStash("test.json");
	}
	
	@After
	public void deleteFile() {
		if (stash.isStashed()) assertTrue(stash.getFile().delete());
	}
	
	@Test
	public void primitivesStash() throws Exception {
		
		stash.stash('c');
		Object value = stash.unstash(Character.class);
		assertTrue(value.equals('c'));
		
		stash.stash(1);
		value = stash.unstash(Integer.class);
		assertTrue(value.equals(1));
		
		stash.stash(1L);
		value = stash.unstash(Long.class);
		assertTrue(value.equals(1L));
		
		stash.stash(1.1f);
		value = stash.unstash(Float.class);
		assertTrue(value.equals(1.1f));

		stash.stash(1.1d);
		value = stash.unstash(Double.class);
		assertTrue(value.equals(1.1d));

	}

	@Test
	public void stringStash() throws Exception {
		
		stash.stash("Hello World");
		String value = stash.unstash(String.class);
		assertEquals("Hello World", value);
	}
	
	@Test
	public void axisConfig() throws Exception {
		
		AxisConfiguration config = new AxisConfiguration();
		config.setApplyModels(true);
		config.setApplyRegions(true);
		config.setFastAxisName("stage_x");
		config.setFastAxisStart(0);
		config.setFastAxisEnd(100);
		config.setSlowAxisName("stage_y");
		config.setSlowAxisStart(-100);
		config.setSlowAxisEnd(-200);
		config.setMicroscopeImage("C:/tmp/fred.png");
		
		stash.stash(config);
		AxisConfiguration stored = stash.unstash(AxisConfiguration.class);
		assertEquals(config, stored);

	}
	
	@Test
	public void map() throws Exception {
		
		AxisConfiguration ac = new AxisConfiguration();
		ac.setApplyModels(true);
		ac.setApplyRegions(true);
		ac.setFastAxisName("stage_x");
		ac.setFastAxisStart(0);
		ac.setFastAxisEnd(100);
		ac.setSlowAxisName("stage_y");
		ac.setSlowAxisStart(-100);
		ac.setSlowAxisEnd(-200);
		ac.setMicroscopeImage("C:/tmp/fred.png");
		
		SampleData sd = new SampleData();
		sd.setName("Sample name");
		sd.setDescription("Hello World");
		
		Map<String,Object> map = new HashMap();
		map.put("ac", ac);
		map.put("sd", sd);
	
		stash.stash(map);
		Map<String,Object> stored = stash.unstash(Map.class);
		assertEquals(map, stored);

	}

	@Test
	public void list() throws Exception {
		
		AxisConfiguration ac = new AxisConfiguration();
		ac.setApplyModels(true);
		ac.setApplyRegions(true);
		ac.setFastAxisName("stage_x");
		ac.setFastAxisStart(0);
		ac.setFastAxisEnd(100);
		ac.setSlowAxisName("stage_y");
		ac.setSlowAxisStart(-100);
		ac.setSlowAxisEnd(-200);
		ac.setMicroscopeImage("C:/tmp/fred.png");
		
		SampleData sd = new SampleData();
		sd.setName("Sample name");
		sd.setDescription("Hello World");
		
		List<Object> list = new ArrayList();
		list.add(ac);
		list.add(sd);
	
		stash.stash(list);
		List<Object> stored = stash.unstash(List.class);
		assertEquals(list, stored);

	}

	@Test
	public void sampleData() throws Exception {
		
		SampleData config = new SampleData();
		config.setName("Sample name");
		config.setDescription("Hello World");
		
		stash.stash(config);
		SampleData stored = stash.unstash(SampleData.class);
		assertEquals(config, stored);

	}

	@Test(expected=IllegalArgumentException.class)
	public void wrongType() throws Exception {
		
		SampleData config = new SampleData();
		config.setName("Sample name");
		config.setDescription("Hello World");
		
		stash.stash(config);
		stash.unstash(AxisConfiguration.class);
	}
	
	@Test
	public void unregisteredSimpleType() throws Exception {
		
		UnregisteredTypeBeanV1 bean = new UnregisteredTypeBeanV1();
		stash.stash(bean); // This type is unregistered and will not work.
		
		UnregisteredTypeBeanV1 naeb = stash.unstash(UnregisteredTypeBeanV1.class);
		assertEquals(bean, naeb);

	}
	
	@Test
	public void unregisteredSimpleTypeExtraField() throws Exception {
		
		UnregisteredTypeBeanV1 bean = new UnregisteredTypeBeanV1();
		stash.stash(bean); // This type is unregistered and will not work.
		
		RegisteredTypeBeanV2 naeb = stash.unstash(RegisteredTypeBeanV2.class);
		assertEquals(bean.getValue2(), naeb.getValue2());
	}

	@Test
	public void unregisteredSimpleTypeLostField() throws Exception {
		
		UnregisteredTypeBeanV1 bean = new UnregisteredTypeBeanV1();
		stash.stash(bean); // This type is unregistered and will not work.
		
		UnregisterdTypeBeanV0 naeb = stash.unstash(UnregisterdTypeBeanV0.class);
		assertEquals(bean.getValue2(), naeb.getValue2());
	}
	
	@Test
	public void registeredSimpleType() throws Exception {
		
		RegisteredTypeBeanV1 bean = new RegisteredTypeBeanV1();
		stash.stash(bean); // This type is unregistered and will not work.
		
		RegisteredTypeBeanV1 naeb = stash.unstash(RegisteredTypeBeanV1.class);
		assertEquals(bean, naeb);

	}
	
	@Test(expected=Exception.class)
	public void registeredSimpleTypeExtraField() throws Exception {
		
		RegisteredTypeBeanV1 bean = new RegisteredTypeBeanV1();
		stash.stash(bean); // This type is unregistered and will not work.
		
		RegisteredTypeBeanV2 naeb = stash.unstash(RegisteredTypeBeanV2.class);
		assertEquals(bean.getValue2(), naeb.getValue2());
	}

	@Test(expected=Exception.class)
	public void registeredSimpleTypeLostField() throws Exception {
		
		RegisteredTypeBeanV1 bean = new RegisteredTypeBeanV1();
		stash.stash(bean); // This type is unregistered and will not work.
		
		RegisteredTypeBeanV0 naeb = stash.unstash(RegisteredTypeBeanV0.class);
		assertEquals(bean.getValue2(), naeb.getValue2());
	}


	@Test(expected=Exception.class)
	public void unregisteredTypeInMap() throws Exception {
		
		UnregisteredTypeBeanV1 bean = new UnregisteredTypeBeanV1();
		Map<String, UnregisteredTypeBeanV1> map = new HashMap<>();
		map.put("stb", bean);
		
		stash.stash(map); // This type is unregistered and will not work.
		
		Map<String, UnregisteredTypeBeanV1> pam = stash.unstash(Map.class);
		assertEquals(map, pam);

	}
	
	@Test(expected=Exception.class)
	public void unregisteredTypeInList() throws Exception {
		
		UnregisteredTypeBeanV1 bean = new UnregisteredTypeBeanV1();
		List<UnregisteredTypeBeanV1> list = new ArrayList<>();
		list.add(bean);
		
		stash.stash(list); // This type is unregistered and will not work.
		List<UnregisteredTypeBeanV1> tsil = stash.unstash(List.class);
		assertEquals(list, tsil);

	}

}
