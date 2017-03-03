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

import java.util.Map;

import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;

/**
 * 
 * Could use Mockito but always causes compilation issues
 *
 */
public class InvalidInjectionDevice extends InjectionDevice {
		
    @PointStart
    public void validMethod1(IRunnableDeviceService rservice) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{rservice});
    }
    @PointStart
    public void validMethod2(IPosition position) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position});
    }
    
    @PointStart
    public void invalidMethod1(Integer unknown) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{unknown});
    }
    @PointStart
    public void invalidMethod2(String unknown1, Integer unknown2,  Map<?,?> unknown3) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{unknown1, unknown2, unknown3});
    }
    @PointStart
    public void invalidMethod3(IPosition position, String unknown) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position, unknown});
    }
    @PointStart
    public void invalidMethod4(IPosition position, IRunnableDeviceService rservice, Map<?,?> unknown) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position, rservice, unknown});
    }
    @PointStart
    public void invalidMethod5(IPosition position, Map<?,?> unknown, IRunnableDeviceService rservice) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position, unknown, rservice});
    }

    @PointStart
    public void invalidMethod6(IRunnableDeviceService rservice, IPointGeneratorService pservice, IPosition position, String unknown1, Integer unknown2,  Map<?,?> unknown3) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{rservice, pservice, position, unknown1, unknown2, unknown3});
    }
 }
