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
package org.eclipse.scanning.connector.epics;

import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.malcolm.IMalcolmDevice;
import org.eclipse.scanning.api.malcolm.MalcolmDeviceException;
import org.eclipse.scanning.api.malcolm.connector.IMalcolmConnectorService;
import org.eclipse.scanning.api.malcolm.connector.MessageGenerator;
import org.eclipse.scanning.api.malcolm.message.MalcolmMessage;
import org.eclipse.scanning.api.malcolm.message.Type;

/**
 * Class to encapsulate the details of sending stuff.
 * 
 * @author Matthew Taylor
 *
 */
class EpicsV4MalcolmMessageGenerator implements MessageGenerator<MalcolmMessage> {

	private IMalcolmDevice<?>                 device;
	private IMalcolmConnectorService<MalcolmMessage> service;

	EpicsV4MalcolmMessageGenerator(IMalcolmConnectorService<MalcolmMessage> service) {
		this(null, service);
	}

	EpicsV4MalcolmMessageGenerator(IMalcolmDevice<?> device, IMalcolmConnectorService<MalcolmMessage> service) {
		this.device  = device;
		this.service = service;
	}

	private static volatile long callCount = 0;
	
	private MalcolmMessage createMalcolmMessage() {
		MalcolmMessage ret = new MalcolmMessage();
		ret.setId(callCount++);
		return ret;
	}

	@Override
	public MalcolmMessage createSubscribeMessage(String subscription) {
		final MalcolmMessage msg = createMalcolmMessage();
		msg.setType(Type.SUBSCRIBE);
		msg.setEndpoint(subscription);
		return msg;
	}

	@Override
	public MalcolmMessage createUnsubscribeMessage() {
		final MalcolmMessage msg = createMalcolmMessage();
		msg.setType(Type.UNSUBSCRIBE);
		return msg;
	}
	
	@Override
	public MalcolmMessage createGetMessage(String cmd) throws MalcolmDeviceException {
		final MalcolmMessage msg = createMalcolmMessage();
		msg.setType(Type.GET);
		msg.setEndpoint(cmd);
		return msg;
	}
	
	private MalcolmMessage createCallMessage(final String methodName) throws MalcolmDeviceException {
		final MalcolmMessage msg = createMalcolmMessage();
		msg.setType(Type.CALL);
		msg.setMethod(methodName); 
		return msg;
	}
	@Override
	public MalcolmMessage createCallMessage(final String methodName, Object arg) throws MalcolmDeviceException {
		final MalcolmMessage msg = createCallMessage(methodName);
		msg.setArguments(arg);
		return msg;
	}
	
	@Override
	public MalcolmMessage call(StackTraceElement[] stackTrace, DeviceState... latches) throws MalcolmDeviceException {
		final MalcolmMessage msg   = createCallMessage(getMethodName(stackTrace));
		final MalcolmMessage reply = service.send(device, msg);
		// TODO What about state changes? Should we block?
		//if (latches!=null) latch(latches);
		return reply;
	}
	
	private static final String getMethodName(StackTraceElement ste[]) {
	    String methodName = "";
	    boolean flag = false;
	   
	    for (StackTraceElement s : ste) {
	        if (flag) {
	            methodName = s.getMethodName();
	            break;
	        }
	        flag = s.getMethodName().equals("getStackTrace");
	    }
	    return methodName;
	}
	

}
