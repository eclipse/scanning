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
import java.util.List;

import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test which attemps to see if the mock scannables do reasonable things when moving them.
 * 
 * @author Matthew Gerring
 *
 */
public class RealisticishMockTest {

	private static IScannableDeviceService cservice;
	private static IScannable<Number>      temp;

	@BeforeClass
	public static void setup() throws Exception {
		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		cservice = new MockScannableConnector(null);
		temp = cservice.getScannable("T");
		temp.setPosition(295d); // We operate it from a known point.
		
		((MockScannable)temp).setMoveRate(5); // Slow tests are bad.
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		((MockScannable)temp).setMoveRate(0.5);
	}

	
	@Test
	public void addFive() throws Exception {
		checkTemperature(5);
	}
	@Test
	public void subtractFive() throws Exception {
		checkTemperature(-5);
	}
	
	private void checkTemperature(double delta) throws Exception {
		
		List<Double> positions = new ArrayList<>();
		((IPositionListenable)temp).addPositionListener(new IPositionListener() {
			public void positionChanged(PositionEvent evt) throws ScanningException {
				double val = (Double)evt.getPosition().get("T");
//				System.out.println("The value of T was at "+val);
				positions.add(val);
			}
		});
		System.out.println("Moving to "+(temp.getPosition().doubleValue()+delta)+" from "+temp.getPosition());
		temp.setPosition(temp.getPosition().doubleValue()+delta);

        assertEquals(10, positions.size());
	}
}
