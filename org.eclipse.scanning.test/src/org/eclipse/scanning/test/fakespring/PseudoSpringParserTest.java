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
package org.eclipse.scanning.test.fakespring;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * A few quick and dirty tests of the parser. We only have to support
 * a tiny subset of spring.
 * 
 * @author Matthew Gerring
 *
 */
public class PseudoSpringParserTest {

	@BeforeClass
	public static void systemProperties() {
		System.setProperty("org.eclipse.scanning.test", "true");
		System.setProperty("org.eclipse.scanning.broker.uri", "http://localhost:61616");
	}
	
	@Test
	public void testBasicFile() throws Exception {
		parseFile("file1.xml", 2);
	}
	
	@Test
	public void testNothingInFile() throws Exception {
		parseFile("nothing.xml", 0);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testAbscentFile() throws Exception {
		parseFile("thisfiledoesnotexist.xml", -1);
	}

	
	@Test
	public void testBasicOSGiFile() throws Exception {
		parseFile("file2.xml", 2);
	}

	@Test
	public void testDetectorBeans() throws Exception {
		parseFile("detector_beans.xml", 6);
	}
	
	@Test
	public void testControlTree() throws Exception {
		parseFile("control_tree.xml", 7);
	}

	@Test
	public void testDetectorWithRef() throws Exception {
		parseFile("detectors_with_ref.xml", 9);
	}

	private void parseFile(String name, int size) throws Exception {
		InputStream stream = getClass().getResourceAsStream(name);
		PseudoSpringParser parser = new PseudoSpringParser();
		Map<String, Object> created = parser.parse(stream);	
		assertEquals(size, created.size());
		System.out.println(name+" parsed and size was: "+created.size());
	}
	

	@Test
	public void testOnlyLinks() throws Exception {
		
		File dir = new File("src"); // The test bundle dir.
		final String frag = getClass().getPackage().getName().replace('.', '/');
		dir = new File(dir.getAbsolutePath()+"/"+frag);
		
		final File links = new File(dir, "links.xml");
		PseudoSpringParser parser = new PseudoSpringParser();
		Map<String, Object> created = parser.parse(links.getAbsolutePath());	
		System.out.println(links.getName()+" parsed and size was: "+created.size());
		assertEquals(14, created.size());
	}
	

}
