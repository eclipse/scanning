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
package org.eclipse.scanning.example.scannable;

import org.eclipse.scanning.api.points.IPosition;

/**
 * Designed to monitor topup (pretty badly, just conceptually). 
 * On a step divisible by ten, will force a wait
 * until imaginary topup value is reached.
 * 
 * @author Matthew Gerring
 *
 */
public class MockPausingMonitor extends MockScannable {

	public MockPausingMonitor(String string, double d, int i) {
		super(string,d,i);
	}

	public void setPosition(Number position, IPosition loc) throws Exception {
		
		final int step = loc.getStepIndex();
		if (step%10==0) { // We wait
			System.out.println("Waiting for imaginary topup for 10ms ");
			Thread.sleep(10);
			System.out.println("Bean current is now stable again... ");
		}
		super.setPosition(position, loc);
	}

}
