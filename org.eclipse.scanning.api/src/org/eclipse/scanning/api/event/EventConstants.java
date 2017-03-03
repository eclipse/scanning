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


/**
 * This class represents 
 * 
 * @author Matthew Gerring
 */
public interface EventConstants {

    /**
     * A topic which may be used for scan events.
     * It is usually better to use your own topic rather than the default.
     */
	public static final String SCAN_TOPIC      = "org.eclipse.scanning.scan.topic";
    /**
     * The default topic used for status update events
     * It is usually better to use your own topic rather than the default.
     */
	public static final String STATUS_TOPIC      = "org.eclipse.scanning.status.topic";
	
	/**
	 * The default topic used for heartbeat events.
     * It is usually better to use your own topic rather than the default.
	 */
	public static final String HEARTBEAT_TOPIC = "org.eclipse.scanning.alive.topic";
	
	/**
	 * The default topic used for pause and terminate events. This topic is generally used internally.
	 */
	public static final String CMD_TOPIC = "org.eclipse.scanning.command.topic";

	/**
	 * The default set used for recent instructions to command the consumer. This queue is generally used internally.
	 */
	public static final String CMD_SET = "org.eclipse.scanning.command.set";

	/**
	 * The default queue used for submitting things (like ScanRequests) to a queue.
	 * Ordered by submission.
	 */
	public static final String SUBMISSION_QUEUE = "org.eclipse.scanning.submission.queue";

	/**
	 * The default queue used for holding status events. This queue does not hold order
	 * reliably and behaves like a set. The user interface sorts the status objects by
	 * date submitted (usually) when the user looks at the queue.
	 */
	public static final String STATUS_SET = "org.eclipse.scanning.status.set";
	
	/**
	 * Topic used to tell UI users that a give consumer will be going down.
     * It is usually better to use your own topic rather than the default.
	 */
	public static final String ADMIN_MESSAGE_TOPIC = "org.eclipse.scanning.consumer.administratorMessage";

    /**
     * The default topic used for requests. It is usually better to use your own topic rather than the default.
     * It is usually better to use your own topic rather than the default.
     */
	public static final String DEVICE_REQUEST_TOPIC      = "org.eclipse.scanning.request.device.topic";

    /**
     * The default topic used for responses.  It is usually better to use your own topic rather than the default.
     * It is usually better to use your own topic rather than the default.
     */
	public static final String DEVICE_RESPONSE_TOPIC      = "org.eclipse.scanning.response.device.topic";

	/**
	 * A topic on which the values of all scannables should publish. This can happen quite frequently however
	 * not at a rate that JMS should not be able to handle providing the message is kept small.
	 */
	public static final String POSITION_TOPIC              = "org.eclipse.scanning.request.position.topic";
    
	/**
     * The default topic used for requests. It is usually better to use your own topic rather than the default.
     * It is usually better to use your own topic rather than the default.
     */
	public static final String POSITIONER_REQUEST_TOPIC      = "org.eclipse.scanning.request.positioner.topic";

    /**
     * The default topic used for responses.  It is usually better to use your own topic rather than the default.
     * It is usually better to use your own topic rather than the default.
     */
	public static final String POSITIONER_RESPONSE_TOPIC      = "org.eclipse.scanning.response.positioner.topic";
	
	/**
	 * The is the topic for requests to acquire data from a detector.
	 */
	public static final String ACQUIRE_REQUEST_TOPIC = "org.eclipse.scanning.request.acquire.topic";
	
	/**
	 * The is the topic for responses to acquire data f
	 */
	public static final String ACQUIRE_RESPONSE_TOPIC = "org.eclipse.scanning.response.acquire.topic";
	
	/**
	 * When the user sets up the axes, an AxisConfiguration object will be broadcast on this event.
	 */
	public static final String AXIS_CONFIGURATION_TOPIC      = "org.eclipse.scanning.axis.configuration.topic";

}
