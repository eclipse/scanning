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
package org.eclipse.scanning.test.malcolm.device;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;

public class LatchDelegate {

	
	protected Map<Long, CountDownLatch> testLatchMap;
	
	public LatchDelegate() {
		this.testLatchMap = new ConcurrentHashMap<>(3);
	}

	protected DeviceState latch(IMalcolmDevice device, DeviceState... ignored) throws Exception {
        return latch(device, -1, TimeUnit.SECONDS, ignored);
	}
	
	protected DeviceState latch(IMalcolmDevice device, long time, TimeUnit unit, DeviceState... ignored) throws Exception {
		Long id = Thread.currentThread().getId();
		if (testLatchMap.containsKey(id)) {
			throw new MalcolmDeviceException(device, "The thread '"+Thread.currentThread().getName()+"'is already waiting for state change! Something went wrong with the latch locking!");
		}

		CountDownLatch latch = new CountDownLatch(1);
		testLatchMap.put(id, latch);

		await(device, latch, time, unit);  // We cannot latch forever or risk deadlock
		if (testLatchMap.containsKey(id)) {
			throw new MalcolmDeviceException(device, "Unexpected latch key thread '"+Thread.currentThread().getName()+"' not cleared!");
		}

		if (ignored!=null) {
			List<DeviceState> states = Arrays.asList(ignored);
			while(states.contains(device.getDeviceState())) {
				latch = new CountDownLatch(1);
				testLatchMap.put(id, latch);
				await(device, latch, time, unit); // We cannot latch forever or risk deadlock
				if (testLatchMap.containsKey(id)) {
					throw new MalcolmDeviceException(device, "Unexpected latch key thread '"+Thread.currentThread().getName()+"' not cleared!");
				}
			}
		} 
		
		return device.getDeviceState();
	}
	
	private final void await(IMalcolmDevice device, CountDownLatch latch, long timeout, TimeUnit unit) throws Exception {
		try {
			if (timeout < 1) {
				latch.await();
			} else {
				boolean ok = latch.await(timeout, unit);
				if (!ok) {
					throw new MalcolmDeviceException(device, "Latch took longer than "+timeout+" "+unit+". Device state is " + device.getDeviceState());
				}
			}
		} catch (InterruptedException e) {
			throw new Exception("Cannot latch on to state change!", e);
		}
	}

	public void setState(DeviceState state) {
		
		if (testLatchMap != null) {
			// Important, copy, clear, notify or will not work
			CountDownLatch[] latches = testLatchMap.values().toArray(new CountDownLatch[testLatchMap.size()]);
			testLatchMap.clear();
			for (CountDownLatch latch : latches)  {
				latch.countDown();
			}
		}
	}


}
