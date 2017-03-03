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
package org.eclipse.scanning.test.command;

import org.junit.Test;

public class MScanServletTest extends AbstractMScanTest {

	public MScanServletTest() {
		super(false);
	}

	@Test
	public void testGridScan() throws Exception {
		pi.exec("sr = scan_request(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(5, 5), snake=True), det=detector('mandelbrot', 0.1))");
		runAndCheck("sr", false, 20);
	}
	
	@Test(expected=Exception.class)
	public void testGridScanWrongAxis() throws Exception {
		pi.exec("sr = scan_request(grid(axes=('x', 'y'), start=(0, 0), stop=(10, 10), count=(5, 5), snake=True), det=detector('mandelbrot', 0.1))");
		runAndCheck("sr", false, 20);
	}
	
	@Test
	public void testGridScanNoDetector() throws Exception {
		pi.exec("sr = scan_request(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(5, 5), snake=True))");
		runAndCheck("sr", false, 20);
	}
	
	@Test
	public void testGridWithROIScan() throws Exception {
		pi.exec("sr = scan_request(grid(axes=('xNex', 'yNex'), start=(0.0, 1.0), stop=(10.0, 12.0), count=(3, 4), snake=False, roi=[circ(origin=(0.0, 0.0), radius=1.0)]), det=detector('mandelbrot', 0.1))");
		runAndCheck("sr", false, 20);
	}
	
	@Test(expected=Exception.class)
	public void testGridScanWithBadTimeout() throws Exception {
		pi.exec("sr = scan_request(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True), det=detector('mandelbrot', 1.2, timeout=1))");
		runAndCheck("sr", false, 20);
	}
	
	@Test
	public void testGridScanWithGoodTimeout() throws Exception {
		pi.exec("sr = scan_request(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True), det=detector('mandelbrot', 1.2, timeout=2))");
		runAndCheck("sr", false, 20);
	}

}
