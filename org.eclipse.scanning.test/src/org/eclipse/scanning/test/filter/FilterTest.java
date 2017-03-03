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
package org.eclipse.scanning.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.filter.Filter;
import org.eclipse.scanning.api.filter.IFilter;
import org.eclipse.scanning.api.filter.IFilterService;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FilterTest {
	
	private static IScannableDeviceService sservice;
	private static IFilterService          fservice;

	@BeforeClass
	public static void parseSpring() throws Exception {
		
		fservice = IFilterService.DEFAULT;
		sservice = new MockScannableConnector(null);
	}
	
	@Before
	public void before() throws Exception {
		fservice.clear();
		PseudoSpringParser parser = new PseudoSpringParser();
		parser.parse(FilterTest.class.getResourceAsStream("test_filters.xml"));
	}
	
	@Test
	public void notNull() {
		assertNotNull(IFilterService.DEFAULT);
		assertNotNull(IFilterService.DEFAULT.getFilter("org.eclipse.scanning.scannableFilter"));
		assertEquals(7, IFilterService.DEFAULT.getFilter("org.eclipse.scanning.scannableFilter").getExcludes().size());
		assertEquals(5, IFilterService.DEFAULT.getFilter("org.eclipse.scanning.scannableFilter").getIncludes().size());
	}
	
	@Test
	public void znoFilter() throws ScanningException {
		assertEquals(sservice.getScannableNames(), fservice.filter("not.there", sservice.getScannableNames()));
	}
	
	@Test
	public void testFilterSpring() throws Exception {
		check();
	}

	@Test
	public void testFilterManual() throws Exception {
		
		fservice.clear();
		
		// Spring does this for us in "test_filters.xml"
		IFilter<String> filter = new Filter();
		filter.setName("org.eclipse.scanning.scannableFilter");
		filter.setExcludes(Arrays.asList("qvach", "monitor1", "a", "b", "c", "beam.*", "neXusScannable.*"));
		filter.setIncludes(Arrays.asList("monitor.*", "beamcurrent", "neXusScannable2", "neXusScannable", "rubbish"));
		fservice.register(filter);
		
		check();
	}
	
	@Test
	public void testDuplicatesNoSpring() throws Exception {

		fservice.clear();
		
		IFilter<String> dfilter = new Filter();
		dfilter.setName("duplicates");
		dfilter.setIncludes(Arrays.asList("a"));
		dfilter.setExcludes(Arrays.asList("b"));
		fservice.register(dfilter);
	
		List<String> items = new ArrayList<>(Arrays.asList("a", "a", "b", "b"));
		assertEquals(Arrays.asList("a", "a"), fservice.filter("duplicates", items));
		
		items = new ArrayList<>(Arrays.asList("a", "b", "a", "b"));
		assertEquals(Arrays.asList("a", "a"), fservice.filter("duplicates", items));

		items = new ArrayList<>(Arrays.asList("b", "b", "1", "a", "a"));
		assertEquals(Arrays.asList("1", "a", "a"), fservice.filter("duplicates", items));
		
		items = new ArrayList<>(Arrays.asList("b", "b", "a", "a", "1"));
		assertEquals(Arrays.asList("a", "a", "1"), fservice.filter("duplicates", items));

	}
	
	@Test
	public void testDuplicatesExclude() throws Exception {

		fservice.clear();
		
		IFilter<String> dfilter = new Filter();
		dfilter.setName("duplicates");
		dfilter.setIncludes(Arrays.asList("a"));
		dfilter.setExcludes(Arrays.asList("a", "b"));
		fservice.register(dfilter);
	
		List<String> items = new ArrayList<>(Arrays.asList("a", "a", "b", "b"));
		assertEquals(Arrays.asList("a", "a"), fservice.filter("duplicates", items));
		
		items = new ArrayList<>(Arrays.asList("a", "b", "a", "b"));
		assertEquals(Arrays.asList("a", "a"), fservice.filter("duplicates", items));

		items = new ArrayList<>(Arrays.asList("b", "b", "1", "a", "a"));
		assertEquals(Arrays.asList("1", "a", "a"), fservice.filter("duplicates", items));
		
		items = new ArrayList<>(Arrays.asList("b", "b", "a", "a", "1"));
		
		// Because we excluded it the result changes
		assertEquals(Arrays.asList("1", "a", "a"), fservice.filter("duplicates", items));

	}

	@Test(timeout=10000) // Must complete in 10s or less
	public void testDuplicatesLarge() throws Exception {

		fservice.clear();
		
		IFilter<String> dfilter = new Filter();
		dfilter.setName("duplicates");
		
		List<String> as = Arrays.stream(new String[1000]).map(nothing->"a").collect(Collectors.toList());
		assertEquals(1000, as.size());
		dfilter.setIncludes(as);
		List<String> bs = Arrays.stream(new String[1000]).map(nothing->"b").collect(Collectors.toList());
		assertEquals(1000, bs.size());
		dfilter.setExcludes(bs);
		fservice.register(dfilter);
	
		List<String> items = new ArrayList<>();
		items.addAll(as);
		items.addAll(bs);
		assertEquals(as, fservice.filter("duplicates", items));
		
		items = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
		    items.add(as.get(i));
		    items.add(bs.get(i));
		}
		assertEquals(as, fservice.filter("duplicates", items));

		items = new ArrayList<>();
		items.addAll(bs);
		items.add("1");
		items.addAll(as);
		assertEquals("1", fservice.filter("duplicates", items).get(0));
		assertEquals("a", fservice.filter("duplicates", items).get(1));
		assertEquals("a", fservice.filter("duplicates", items).get(2));
		
		items = new ArrayList<>();
		items.addAll(bs);
		items.addAll(as);
		items.add("1");
		List<String> filtered = fservice.filter("duplicates", items);
		assertEquals("1", filtered.get(filtered.size()-1));
		assertEquals("a", filtered.get(filtered.size()-2));
		assertEquals("a", filtered.get(filtered.size()-3));

	}

	private void check() throws ScanningException {
		
		sservice.getScannable("aa"); // Create an aa scannable
		List<String> names    = sservice.getScannableNames();
		List<String> filtered = fservice.filter("org.eclipse.scanning.scannableFilter", names);
		
		// Stuff not matched should be there
		assertTrue(filtered.contains("stage_x"));
		assertTrue(filtered.contains("stage_y"));
		assertTrue(filtered.contains("x"));
		assertTrue(filtered.contains("y"));
		assertTrue(filtered.contains("z"));

		// Stuff that got included
		assertTrue(filtered.contains("monitor1"));
		assertTrue(filtered.contains("neXusScannable2"));
		assertTrue(filtered.contains("beamcurrent"));
		assertTrue(filtered.contains("aa")); // The one we created
		
		// Things which should surely not be around.
		assertFalse(filtered.contains("qvach"));
		assertFalse(filtered.contains("a"));
		assertFalse(filtered.contains("b"));
		assertFalse(filtered.contains("c"));
		assertFalse(filtered.contains("rubbish"));
		assertFalse(filtered.contains("neXusScannable"));
		assertFalse(filtered.contains("neXusScannable1"));
		assertFalse(filtered.contains("neXusScannable3"));
		assertFalse(filtered.contains("neXusScannable4"));
	}

	
}
