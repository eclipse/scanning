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
package org.eclipse.scanning.event;

import java.net.URI;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.core.ISubscriber;

/**
 * Checks for the heartbeat of a named consumer.
 * 
 * @author Matthew Gerring
 *
 */
class HeartbeatChecker {
		
	private URI    uri;
	private String consumerName;
	private long   listenTime;
	private volatile boolean ok = false;
	private IEventService eventService;
	
	public HeartbeatChecker(IEventService eventService, URI uri, String consumerName, long listenTime) {
		this.eventService          = eventService;
		this.uri          = uri;
		this.consumerName = consumerName;
		this.listenTime   = listenTime;
	}
	
	public void checkPulse() throws EventException, InterruptedException {
		
		ISubscriber<IHeartbeatListener>	subscriber = eventService.createSubscriber(uri, IEventService.HEARTBEAT_TOPIC);
        ok = false;
        
        try {
        	subscriber.addListener(new IHeartbeatListener() {
        		@Override
        		public void heartbeatPerformed(HeartbeatEvent evt) {
        			HeartbeatBean bean = evt.getBean();
        			if (!consumerName.equals(bean.getConsumerName())) {
        				return;
        			}
        			ok = true;
        		}
        	});

            Thread.sleep(listenTime);
            
            if (!ok) throw new EventException(consumerName+" Consumer heartbeat absent.\nIt is either stopped or unresponsive.\nPlease contact your support representative.");
        	

        } finally {
        	subscriber.disconnect();
        }
	}

}
