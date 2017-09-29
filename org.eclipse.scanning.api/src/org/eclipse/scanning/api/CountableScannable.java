/*-
 *******************************************************************************
 * Copyright (c) 2011, 2017 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.api;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.scan.event.Location;

/**
 *
 * A scannable with the ability to record method counts.
 *
 * This is primarily useful for testing and debugging it
 * means that a dependency on Mockhito is not required
 * in order to count method calls.
 *
 * @author Matthew Gerring
 *
 */
public abstract class CountableScannable<T> extends AbstractScannable<T> {


	private Map<String, Integer> counts = new HashMap<>();

	public CountableScannable() {
		super();
	}

	public CountableScannable(IPublisher<Location> publisher, IScannableDeviceService sservice) {
		super(publisher, sservice);
	}

	public CountableScannable(IPublisher<Location> publisher) {
		super(publisher);
	}

	public CountableScannable(IScannableDeviceService sservice) {
		super(sservice);
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

	public void resetCount() {
		counts.clear();
	}

}
