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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.AbstractScannable;
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
public class CountingDevice extends AbstractScannable<Double> {
	
	protected Double value;
	protected Map<String, Integer> counts = new HashMap<>();
	
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
	public void setPosition(Double value, IPosition position) throws Exception {
		this.value = value;
	}
	
	protected void count(StackTraceElement[] ste) {
		String methodName = getMethodName(ste);
		Integer count = counts.get(methodName);
		if (count==null) count = 0;
		count = count+1;
		counts.put(methodName, count);
	}
	
	public int getCount(String method) {
		if (!counts.containsKey(method)) return 0;
		return counts.get(method);
	}
	
	protected static final String getMethodName ( StackTraceElement ste[] ) {  
		   
	    String methodName = "";  
	    boolean flag = false;  
	   
	    for ( StackTraceElement s : ste ) {  
	   
	        if ( flag ) {  
	   
	            methodName = s.getMethodName();  
	            break;  
	        }  
	        flag = s.getMethodName().equals( "getStackTrace" );  
	    }  
	    return methodName;  
	}

}