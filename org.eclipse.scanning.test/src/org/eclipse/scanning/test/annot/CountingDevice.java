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

import org.eclipse.scanning.api.CountableScannable;
import org.eclipse.scanning.api.annotation.scan.LevelStart;
import org.eclipse.scanning.api.annotation.scan.PostConfigure;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.annotation.scan.ScanStart;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanInformation;

/**
 *
 * Could use Mockito but always causes compilation issues
 *
 */
public class CountingDevice extends CountableScannable<Double> {

	protected Double value;

	public CountingDevice() {

	}

	@PreConfigure
    public final void configure(ScanInformation info) throws Exception {
		if (info==null) throw new Exception("No information!");
        count(Thread.currentThread().getStackTrace());
	}

	@PostConfigure
    public final void configured(ScanInformation info) throws Exception {
		if (info==null) throw new Exception("No information!");
        count(Thread.currentThread().getStackTrace());
	}

	@LevelStart
    public final void prepare() throws Exception {
        count(Thread.currentThread().getStackTrace());
	}

    @ScanStart
    public final void prepareVoltages() throws Exception {
	// Do a floating point op for timings
        double v1 = 2;
        double v2 = 2;
        double s = v1*v2;
        count(Thread.currentThread().getStackTrace());
    }
    @ScanEnd
    public void dispose() {
       value = null;
       count(Thread.currentThread().getStackTrace());
    }
	@Override
	public Double getPosition() throws Exception {
		return value;
	}
	@Override
	public Double setPosition(Double value, IPosition position) throws Exception {
		this.value = value;
		return value;
	}

}