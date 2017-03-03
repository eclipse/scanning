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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.annotation.scan.PointStart;
import org.eclipse.scanning.api.annotation.scan.PreConfigure;
import org.eclipse.scanning.api.annotation.scan.ScanEnd;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPointGeneratorService;
import org.eclipse.scanning.api.points.IPosition;

/**
 * 
 * Could use Mockito but always causes compilation issues
 *
 */
public class InjectionDevice extends CountingDevice {
	
	private Map<String, List<Object[]>> calls = new HashMap<>();
	private IPointGenerator<?> pointGenerator;
	
    @PointStart
    public void method1(IRunnableDeviceService rservice) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{rservice});
    }
    @PointStart
    public void method2(IPosition position) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position});
    }
    @PointStart
    public void method3(IRunnableDeviceService rservice, IPosition position) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{rservice, position});
    }
    @PointStart
    public void method4(IPosition position, IRunnableDeviceService rservice) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position, rservice});
    }
    @PointStart
    public void method5(IPosition position, IRunnableDeviceService rservice, IPointGeneratorService pservice) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{position, rservice, pservice});
    }
    @PointStart
    public void method6(IRunnableDeviceService rservice, IPointGeneratorService pservice, IPosition position) throws Exception {
    	count(Thread.currentThread().getStackTrace(), new Object[]{rservice, pservice, position});
    }
    
	protected void count(StackTraceElement[] ste, Object[] oa) {
		String methodName = getMethodName(ste);
		List<Object[]> count = calls.get(methodName);
		if (count==null) {
			count = new ArrayList<>(3);
			calls.put(methodName, count);
		}
		count.add(oa);
	}
	
	@PreConfigure
	public void setPointGenerator(IPointGenerator<?> pointGenerator) {
		this.pointGenerator = pointGenerator;
	}

    @Override
    @ScanEnd
    public void dispose() {
       super.dispose();
       calls.clear();
    }
    
    public List<Object[]> getArguments(String methodName) {
    	return calls.get(methodName);
    }
	public IPointGenerator<?> getPointGenerator() {
		return pointGenerator;
	}
}
