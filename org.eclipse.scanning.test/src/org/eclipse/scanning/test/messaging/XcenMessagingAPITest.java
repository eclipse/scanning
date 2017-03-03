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

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.xcen.beans.XcenBean;
import org.eclipse.scanning.example.xcen.consumer.XcenServlet;
import org.eclipse.scanning.server.servlet.Services;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * Class to test the API changes for XcenBean messaging.
 * 
 * NOTE: Change Python messaging examples accordingly, if any of these
 * tests fail. The 'examples' package can be found in:
 * org.eclipse.scanning.example.messaging/scripts
 * 
 * @author Martin Gaughran
 *
 */
public class XcenMessagingAPITest extends BrokerTest {
	
	
	protected IEventService             eservice;
	protected ISubmitter<XcenBean>		submitter;
	protected ISubscriber<IBeanListener<XcenBean>>		subscriber;
	protected XcenServlet xcenServlet;

	@Before
	public void createServices() throws Exception {
		
		// We wire things together without OSGi here 
		// DO NOT COPY THIS IN NON-TEST CODE!
		setUpNonOSGIActivemqMarshaller();
		
		eservice = new EventServiceImpl(new ActivemqConnectorService()); // Do not copy this get the service from OSGi!	
				
		Services.setEventService(eservice);

		connect();
	}

	protected void connect() throws EventException, URISyntaxException {
		
		String submitQueue = "dataacq.xcen.SUBMISSION_QUEUE";
		String statusTopic = "dataacq.xcen.STATUS_TOPIC";
		String statusSet = "dataacq.xcen.STATUS_QUEUE";
		
		xcenServlet = new XcenServlet();
		xcenServlet.setSubmitQueue(submitQueue);
		xcenServlet.setStatusTopic(statusTopic);
		xcenServlet.setStatusSet(statusSet);
		xcenServlet.setBroker(uri.toString());
		xcenServlet.connect();
		xcenServlet.setDurable(true);
		
		submitter = eservice.createSubmitter(uri, submitQueue);
		subscriber = eservice.createSubscriber(uri, statusTopic);
	}
	
	@After
	public void stop() throws EventException {
		
    	if (submitter!=null) submitter.disconnect();
    	if (subscriber!=null) subscriber.disconnect();
    	if (xcenServlet!=null) xcenServlet.disconnect();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getMessageResponse(String sentJson) throws Exception {
		
		XcenBean sentBean = eservice.getEventConnectorService().unmarshal(sentJson, null);
				
		final List<XcenBean> beans = new ArrayList<>(1);
		final CountDownLatch latch = new CountDownLatch(1);

		IBeanListener listener = new IBeanListener() {

			@Override
			public void beanChangePerformed(BeanEvent evt) {
				beans.add((XcenBean) evt.getBean());
				latch.countDown();
			}
		};
		
		subscriber.addListener(listener);
		
		submitter.submit(sentBean);
		
		boolean ok = latch.await(5, TimeUnit.SECONDS);
		subscriber.clear();
		if (!ok) throw new Exception("The latch broke before Xcen responded!");
		
		if (beans.size() == 0) throw new Exception("No Xcen responses have been found!");
		return eservice.getEventConnectorService().marshal(beans.get(beans.size()-1));
	}
	
	public File getXcenRunDir(String json) throws Exception {
		XcenBean bean = eservice.getEventConnectorService().unmarshal(json, null);
		return new File(bean.getRunDirectory());
	}
	
	@Test
	public void testPositioner() throws Exception {
		
		String sentJson = "{\"@type\":\"XcenBean\",\"uniqueId\":\"1441796619081_780ede90-6f30-4aaa-bd1b-c7a09fa12319\",\"status\":\"SUBMITTED\",\"name\":\"Test Xcen\",\"message\":\"A test xcen execution\",\"percentComplete\":0.0,\"userName\":\"lkz95212\",\"hostName\":null,\"runDirectory\":\"xcenrun\",\"submissionTime\":1441796619734,\"beamline\":\"i04-1\",\"visit\":\"nt5073-40\",\"collection\":\"sapA-x56_A\",\"x\":0.0,\"y\":0.0,\"z\":0.0}";
		String expectedJson = "{\"@type\":\"XcenBean\",\"uniqueId\":\"1441796619081_780ede90-6f30-4aaa-bd1b-c7a09fa12319\",\"previousStatus\":\"QUEUED\",\"status\":\"RUNNING\",\"name\":\"Test Xcen\",\"message\":\"A test xcen execution\",\"userName\":\"lkz95212\",\"submissionTime\":1441796619734,\"beamline\":\"i04-1\",\"visit\":\"nt5073-40\",\"collection\":\"sapA-x56_A\",\"x\":0.0,\"y\":0.0,\"z\":0.0}";
		
		String returnedJson = getMessageResponse(sentJson);
		
		System.out.println(returnedJson);
		
		SubsetStatus.assertJsonContains("Failed to return correct Xcen response.", returnedJson, expectedJson);
		
		FileUtils.recursiveDelete(getXcenRunDir(returnedJson));
		
	}
}
