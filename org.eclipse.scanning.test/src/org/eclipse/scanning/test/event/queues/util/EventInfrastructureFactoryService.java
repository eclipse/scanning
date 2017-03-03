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
package org.eclipse.scanning.test.event.queues.util;

import java.net.URI;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.test.BrokerTest;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * Manages Broker service through the BrokerTest class and allows 
 * instantiation of a "Dummy" consumer.
 *
 * @author Michael Wharmby
 *
 */
@Deprecated
public class EventInfrastructureFactoryService extends BrokerTest {
	
	private boolean active = false, unitTest = false;
	private IEventService evServ;
	
	/**
	 * Start the broker service & optionally set up the Event Service too.
	 * 
	 * @param unitTest true if this is a unit test, will set up EventService.
	 * @throws Exception
	 */
	public void start(boolean unitTest) throws Exception {
		startBroker();
		this.unitTest = unitTest;
		
		if (unitTest) {
			
			setUpNonOSGIActivemqMarshaller();
			
			evServ =  new EventServiceImpl(new ActivemqConnectorService());
		}
		
		active = true;
	}
	
	/**
	 * Stop the current BrokerService instance.
	 * @throws Exception
	 */
	public void stop() throws Exception {
		if (unitTest) {
			evServ = null;
		}
		
		stopBroker();
		active = false;
	}
	
	/**
	 * Is this service started?
	 * @return
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * The URI of the currently active BrokerService
	 * @return
	 * @throws Exception
	 */
	public URI getURI() throws Exception {
		if (!isActive()) {
			System.out.println("TestBroker not started; starting (is unit test?: "+unitTest+")...");
			start(unitTest);
		}
		return uri;
	}
	
	/**
	 * EventService configured for the currently active BrokerService.
	 * @return
	 */
	public IEventService getEventService() {
		return evServ;
	}
	
	/**
	 * Create a consumer with generic configuration
	 * 
	 * @param bean only needed if creating a runner.
	 * @param withRunner true for a non-functional fake/
	 * @return
	 * @throws Exception
	 */
	public <T extends StatusBean> IConsumer<T> makeConsumer(T bean, 
			boolean withRunner) throws Exception {
		if (!isActive()) {
			System.out.println("TestBroker not started; starting (is unit test?: "+unitTest+")...");
			start(unitTest);
		}
		
		IConsumer<T> cons;
		try {
			cons = evServ.createConsumer(uri);
			if (withRunner) cons.setRunner(makeEmptyRunner(bean));
			return cons;
		} catch (Exception ex) {
			System.out.println("Failed to create consumer.");
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Create a fake, non-functional runner.
	 * @param bean
	 * @return
	 */
	private static <T extends StatusBean> IProcessCreator<T> makeEmptyRunner(T bean) {
		return new IProcessCreator<T>() {

			@Override
			public IConsumerProcess<T> createProcess(T bean,
					IPublisher<T> statusNotifier)
					throws EventException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
	
	/**
	 * Create a publisher configured for the running BrokerService
	 * @param topic to publish to
	 * @return
	 */
	public <T> IPublisher<T> makePublisher(String topic) {
		if (topic == null) topic = IEventService.STATUS_TOPIC;
		return evServ.createPublisher(uri, topic);
	}
	
	/**
	 * Create a publisher configured for the running BrokerService
	 * @param topic to listen on
	 * @return
	 */
	public <T> ISubscriber<IBeanListener<T>> makeSubscriber(String topic) {
		if (topic == null) topic = IEventService.STATUS_TOPIC;
		return evServ.createSubscriber(uri, topic);
	}

}
