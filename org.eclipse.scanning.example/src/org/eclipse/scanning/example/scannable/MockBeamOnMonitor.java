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
 * Designed to model exiting the scan because a certain value is illegal
 * 
 * @author Matthew Gerring
 *
 */
public class MockBeamOnMonitor extends MockScannable {

	public MockBeamOnMonitor(String string, double d, int i) {
		super(string,d,i);
	}

	public void setPosition(Number position, IPosition loc) throws Exception {
		
		final int step = loc.getStepIndex();
		if (step>0 && step%10==0) { // We wait
			System.out.println("Beam is deamed to be off ");
			throw new Exception("Cannot run scan further!");
		}
		super.setPosition(position, loc);
	}

}
