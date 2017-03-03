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
package org.eclipse.scanning.api.event;

import java.net.URI;
import java.util.EventListener;

import org.eclipse.scanning.api.INameable;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.IQueueReader;
import org.eclipse.scanning.api.event.core.IRequester;
import org.eclipse.scanning.api.event.core.IResponder;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * 
 * The scanning event service allows one to subscribe to 
 * and broadcast events. It may be backed by the EventBus or
 * plain JMS queues and topics depending on the service implementor.
 * 
 * <pre>
 * <b>
 * o publisher/subscriber used for broadcasting to multiple clients.
 * o submitter/consumer used for processing queues
 * o request/response used for get/post synchronous interaction 
 *   (the uuid is used to ensure that the request and response match)
 *   
 * </b>
 * </pre>
 *
 * <pre>
 * <code>
 *   IEventService service = ... // OSGi
 *   final IEventSubscriber subscriber = service.createSubscriber(...);
 *   
 *   IScanListener listener = new IScanListener() { // Listen to any scan
 *       void scanEventPerformed(ScanEvent evt) {
 *           ScanBean scan = evt.getBean();
 *           System.out.println(scan.getName()+" @ "+scan.getPercentComplete());
 *       }    
 *   };
 *   
 *   subscriber.addScanListener(listener);
 *   // Subscribe to anything
 *
 *
 *
 *   IEventService service = ... // OSGi
 *   
 *   final IPublisher publisher = service.createPublisher(...);
 *   final ScanBean scan = new ScanBean(...);
 *   
 *   publisher.broadcast(scan);
 *   
 *   // An event comes internally that the scan has changed state, so we notify like this:
 *   scan.setPercentComplete(3.14);
 *   publisher.broadcast(scan);
 *   </code>
 *   </pre>
 * 
 * @author Matthew Gerring
 *
 */
public interface IEventService extends EventConstants {
	
	
    /**
     * Create an object capable of getting a queue of any objects. NOTE that
     * an ISubmitter is an IQueueConnection but it only manages queues of
     * StatusBeans.
     * 
     * @param uri
     * @param queueName
     * @return
     */
	public <T> IQueueReader<T> createQueueReader(URI uri, String queueName);
	
	/**
	 * Creates an ISubscriber with the default scan event topic and default heartbeat topic.
	 * Useful on the client for adding event listeners to be notified.
	 * 
	 *  Normally this method is good enough to connect to the acquisition server
	 *  and receive its heartbeat and scan events. 
	 *  
	 *  Scan events have a unique id with which to assertain if a given scan event
	 *  came from given scan.
	 * 
	 * @param uri - the location of the JMS broker
	 * @return IEventManager
	 */
	public <T extends EventListener> ISubscriber<T> createSubscriber(URI uri, String topicName);
	

	/**
	 * Creates an IEventPublisher with the default scan event topic and no heartbeat events.
	 * 
	 * @param uri - the location of the JMS broker
	 * @return IEventManager
	 */
	public <U> IPublisher<U> createPublisher(URI uri, String topicName);


	/**
	 * Create a submitter for adding a bean of type U onto the queue.
	 * @param uri
	 * @param queueName
	 * @return
	 */
	public <U extends StatusBean> ISubmitter<U> createSubmitter(URI uri, String queueName);
	

	/**
	 * Create a consumer with the default, status topic, submission queue, status queue and termination topic.
	 * @param uri
	 * @param service
	 * @return
	 */
	public <U extends StatusBean> IConsumer<U> createConsumer(URI uri) throws EventException;
	
	/**
	 * Create a consumer with the submission queue, status queue, status topic and termination topic passed in.
	 * 
	 * @param uri
	 * @param submissionQName
	 * @param statusQName
	 * @param statusTName
	 * @param terminateTName
	 * @param service, may be null, should be null in the OSGi case
	 * @return
	 */
	public <U extends StatusBean> IConsumer<U> createConsumer(URI uri, String submissionQName, 
						                                        String statusQName,
						                                        String statusTName) throws EventException;

	/**
	 * Create a consumer with the submission queue, status queue, status topic and termination topic passed in.
	 * 
	 * @param uri
	 * @param submissionQName
	 * @param statusQName
	 * @param statusTName
	 * @param commandTName
	 * @param service, may be null, should be null in the OSGi case
	 * @return
	 */
	public <U extends StatusBean> IConsumer<U> createConsumer(URI uri, String submissionQName, 
						                                        String statusQName,
						                                        String statusTName,
						                                        String heartbeatTName, 
						                                        String commandTName) throws EventException;

	/**
	 * A poster encapsulates sending and receiving a reply. For instance request a list of 
	 * detectors on the server. This is the same as creating a broadcaster, sending an object
	 * then subscribing to the reply.
	 * 
	 * @param uri
	 * @param requestTopic
	 * @param responseTopic
	 * @return
	 * @throws EventException
	 */
	public <T extends IdBean> IRequester<T> createRequestor(URI uri, String requestTopic, String responseTopic) throws EventException;
	
	/**
	 * Creates a responder on a given topic.
	 * 
	 * @param uri
	 * @param requestTopic
	 * @param responseTopic
	 * @return
	 * @throws EventException
	 */
	public <T extends IdBean> IResponder<T> createResponder(URI uri, String requestTopic, String responseTopic) throws EventException;
	
	/**
	 * Checks the heartbeat can be found and if it cannot in the given time, throws an exception.
	 * @param uri - URI
	 * @param patientName - Name of entity we are checking the hearbeat of
	 * @param listenTime - Time to listener before giving up
	 * @return
	 * @throws EventException
	 */
	public void checkHeartbeat(URI uri, String patientName, long listenTime) throws EventException, InterruptedException;
	
	/**
	 * Checks the topic has things published on it intermittently 
	 * If it does not, in the given time, throws an exception.
	 * @param uri - URI
	 * @param patientName - Name of entity we are checking the hearbeat of
	 * @param listenTime - Time to listener before giving up
	 * @param topicName - The topic, or null to use default heartbeat topic
	 * @param beanClass - The bean class that will be broadcast or null to not specify
	 * @return
	 * @throws EventException
	 */
	public <T extends INameable> void checkTopic(URI uri, String patientName, long listenTime, String topicName, Class<T> beanClass) throws EventException, InterruptedException;

	
	/**
	 * The current event connector service that this event service is using to 
	 * talk to messaging and to marshall objects.
	 * 
	 * @return
	 */
	public IEventConnectorService getEventConnectorService();
	
	/**
	 * Use this call to create a remote service. A wrapper will be created around the service
	 * such that methods called on the client will cause an event to trigger which has a response
	 * generated by the server.
	 * 
	 * The event service caches remote services assuming that each service should exist once.
	 * 
	 * @param uri
	 * @param serviceClass
	 * @return
	 * @throws EventException
	 */
	public <T> T createRemoteService(URI uri, Class<T> serviceClass) throws EventException;
}
