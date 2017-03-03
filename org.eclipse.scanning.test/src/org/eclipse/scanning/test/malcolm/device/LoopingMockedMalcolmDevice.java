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

import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.event.MalcolmEventBean;
import org.eclipse.scanning.api.malcolm.models.MapMalcolmModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;

/**
 * Example Device that executes a callable task a number of times as defined by the params
 * @author fri44821
 *
 */
public class LoopingMockedMalcolmDevice extends PausableMockedMalcolmDevice {
	
	protected int count;
	protected int amount;
	
	public LoopingMockedMalcolmDevice(final String name,  final LatchDelegate latcher) throws ScanningException {
		super(name, latcher);
		setState(DeviceState.IDLE);
		setName(name);
	}

	@Override
	public void validate(MapMalcolmModel params) throws ValidationException {
		throw new ValidationException("Validate is not implemented!");
	}

	@Override
	public void run(IPosition pos) throws MalcolmDeviceException {
		if (!getState().isRunnable()) {
			throw new MalcolmDeviceException("Malcolm is in non-runnable state "+getState());
		}

		if (getState().isRunning()) {
			throw new MalcolmDeviceException(this, "Device '"+getName()+"' is already running or paused!");
		}
		
		try {
			setState(DeviceState.RUNNING); // Will send an event

	        count  = 0;
	        amount = (int)model.getParameterMap().get("nframes");
	        
	        // Send scan start
	        MalcolmEventBean bean = new MalcolmEventBean();
	        bean.setPreviousState(DeviceState.READY);
	        bean.setDeviceState(DeviceState.RUNNING);
			sendEvent(bean);
	           
			while(getState().isRunning()) {
				
				executeTask();
				// Break if done
				if (count>=amount) {
					break;
				}
				
				// Sleep (no need to lock while sleeping)
				long sleep = Math.round((double)model.getParameterMap().get("exposure")*1000d);
				Thread.sleep(sleep);

			} // End fake scanning loop.
			
			setState(DeviceState.IDLE); // State change
	        bean = new MalcolmEventBean();
	        bean.setPreviousState(DeviceState.RUNNING);
	        bean.setDeviceState(DeviceState.IDLE);
			sendEvent(bean);
        } 
		catch (Exception ne) {
			ne.printStackTrace();
    		setState(DeviceState.FAULT, ne.getMessage());
     	    throw new MalcolmDeviceException(this, ne.getMessage(), ne);     	    
        } 
		finally {
			try {
				close();
			} 
			catch (Exception e) {
				throw new MalcolmDeviceException(this, "Cannot cleanly close JMS session", e);
			}
		}
	}
}
