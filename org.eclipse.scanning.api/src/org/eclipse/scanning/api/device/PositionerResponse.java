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
package org.eclipse.scanning.api.device;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IResponseProcess;
import org.eclipse.scanning.api.event.scan.PositionRequestType;
import org.eclipse.scanning.api.event.scan.PositionerRequest;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositioner;

public class PositionerResponse implements IResponseProcess<PositionerRequest>{
	
	private static Map<String, Reference<IPositioner>> positioners;

	private IRunnableDeviceService        dservice;
	private PositionerRequest             bean;
	private IPublisher<PositionerRequest> publisher;

	public PositionerResponse(IRunnableDeviceService        dservice, 
			                  PositionerRequest             bean, 
			                  IPublisher<PositionerRequest> statusNotifier) {

		this.dservice    = dservice;
		this.bean        = bean;
		this.publisher   = statusNotifier;
		if (positioners==null) positioners = new HashMap<>();
	}

	@Override
	public PositionerRequest getBean() {
		return bean;
	}

	@Override
	public IPublisher<PositionerRequest> getPublisher() {
		return publisher;
	}

	@Override
	public PositionerRequest process(PositionerRequest request) throws EventException {
		try {
			IPositioner positioner = getPositioner(request);
			if (request.getPositionType()==PositionRequestType.SET && request.getPosition()!=null) {
				boolean ok = positioner.setPosition(request.getPosition());
				if (!ok) throw new EventException("Internal Error: setPosition() did not return ok!");
				
			} else if (request.getPositionType()==PositionRequestType.ABORT) {
				positioner.abort();
			}else if (request.getPositionType()==PositionRequestType.CLOSE) {
				positioner.close();
			}
			// Return the current position.
			request.setPosition(positioner.getPosition());
			return request;
			
		} catch(ScanningException | InterruptedException ne) {
			throw new EventException("Cannot connect to positioner!", ne);
		}
	}

	private IPositioner getPositioner(PositionerRequest request) throws ScanningException {
		final String id = request.getUniqueId();
		if (positioners.containsKey(id) && positioners.get(id).get()!=null) {
			return positioners.get(id).get();
		}

		IPositioner positioner = dservice.createPositioner();
		positioners.put(request.getUniqueId(), new SoftReference<IPositioner>(positioner));
		return positioner;
	}

}
