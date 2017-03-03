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
package org.eclipse.scanning.test.messaging;

import static org.eclipse.scanning.api.event.EventConstants.STATUS_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.SUBMISSION_QUEUE;
import static org.eclipse.scanning.api.event.EventConstants.STATUS_SET;
import static org.eclipse.scanning.api.event.EventConstants.DEVICE_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.DEVICE_RESPONSE_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.POSITION_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.POSITIONER_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.POSITIONER_RESPONSE_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.ACQUIRE_REQUEST_TOPIC;
import static org.eclipse.scanning.api.event.EventConstants.ACQUIRE_RESPONSE_TOPIC;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Class to test the topic and queue values, for use by Python examples.
 * 
 * NOTE: Change Python messaging examples accordingly, if any of these
 * tests fail. The 'examples' package can be found in:
 * org.eclipse.scanning.example.messaging/scripts
 * 
 * @author Martin Gaughran
 *
 */
@RunWith(Parameterized.class)
public class TopicAndQueueNameTest {

    @Parameters
    public static Collection<String[]> data() {
        return Arrays.asList(new String[][] {     
        	{"STATUS_TOPIC", "org.eclipse.scanning.status.topic", STATUS_TOPIC}, 
        	{"SUBMISSION_QUEUE", "org.eclipse.scanning.submission.queue", SUBMISSION_QUEUE},
        	{"STATUS_SET", "org.eclipse.scanning.status.set", STATUS_SET},
        	{"DEVICE_REQUEST_TOPIC", "org.eclipse.scanning.request.device.topic", DEVICE_REQUEST_TOPIC},
        	{"DEVICE_RESPONSE_TOPIC", "org.eclipse.scanning.response.device.topic", DEVICE_RESPONSE_TOPIC},
        	{"POSITION_TOPIC", "org.eclipse.scanning.request.position.topic", POSITION_TOPIC},
        	{"POSITIONER_REQUEST_TOPIC", "org.eclipse.scanning.request.positioner.topic", POSITIONER_REQUEST_TOPIC},
        	{"POSITIONER_RESPONSE_TOPIC", "org.eclipse.scanning.response.positioner.topic", POSITIONER_RESPONSE_TOPIC},
        	{"ACQUIRE_REQUEST_TOPIC", "org.eclipse.scanning.request.acquire.topic", ACQUIRE_REQUEST_TOPIC},
        	{"ACQUIRE_RESPONSE_TOPIC", "org.eclipse.scanning.response.acquire.topic", ACQUIRE_RESPONSE_TOPIC}
           });
    }
    
    private String name;
    private String expected;
    private String actual;

    public TopicAndQueueNameTest(String name, String expected, String actual) {
    	this.name= name;
    	this.expected= expected;
    	this.actual = actual;
    }
	
	@Test
	public void testTopicOrQueueValue() throws Exception {
		assertTrue(name + " is different. Please change Python examples.", expected.equals(actual));
	}
	
}
