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
package org.eclipse.scanning.test.event;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.List;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.dry.FastRunCreator;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.AbstractQueueConnection;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class AbstractPauseTest extends BrokerTest{


	protected IEventService          eservice;
	protected ISubmitter<StatusBean> submitter;
	protected IConsumer<StatusBean>  consumer;


	@After
	public void dispose() throws EventException {
		submitter.disconnect();
		consumer.clearQueue(IEventService.SUBMISSION_QUEUE);
		consumer.clearQueue(IEventService.STATUS_SET);
		consumer.clearQueue(IEventService.CMD_SET);
		consumer.disconnect();
	}

	@Test
	public void testPausingAConsumerByID() throws Exception {

		consumer.setRunner(new FastRunCreator<StatusBean>(100,false));
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(200);

		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		pauser.setStatusSetName(IEventService.CMD_SET);
		pauser.setStatusSetAddRequired(true);

		PauseBean pbean = new PauseBean();
		pbean.setConsumerId(consumer.getConsumerId());
		pauser.broadcast(pbean);

		Thread.sleep(200);

		assertTrue(!consumer.isActive());

		pbean.setPause(false);
		pauser.broadcast(pbean);

		Thread.sleep(100);

		assertTrue(consumer.isActive());
	}


	@Test
	public void testPausingAConsumerByQueueName() throws Exception {

		consumer.setRunner(new FastRunCreator<StatusBean>(100,false));
		consumer.start();

		StatusBean bean = doSubmit();

		Thread.sleep(200);

		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		pauser.setStatusSetName(IEventService.CMD_SET);
		pauser.setStatusSetAddRequired(true);

		PauseBean pbean = new PauseBean();
		pbean.setQueueName(consumer.getSubmitQueueName());
		pauser.broadcast(pbean);

		Thread.sleep(200);

		assertTrue(!consumer.isActive());

		pbean.setPause(false);
		pauser.broadcast(pbean);

		Thread.sleep(100);

		assertTrue(consumer.isActive());
	}

	@Ignore("TODO Find out why this does not work, it is supposed to...")
	@Test
	public void testReorderingAPausedQueue() throws Exception {

		consumer.setRunner(new FastRunCreator<StatusBean>(0,100,10,100, true));
		consumer.start();

		// Bung ten things on there.
		for (int i = 0; i < 5; i++) {
			StatusBean bean = new StatusBean();
			bean.setName("Submission"+i);
			bean.setStatus(Status.SUBMITTED);
			bean.setHostName(InetAddress.getLocalHost().getHostName());
			bean.setMessage("Hello World");
			bean.setUniqueId(UUID.randomUUID().toString());
			bean.setUserName(String.valueOf(i));
			submitter.submit(bean);
		}

		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		pauser.setStatusSetName(IEventService.CMD_SET);
		pauser.setStatusSetAddRequired(true);

		PauseBean pbean = new PauseBean();
		pbean.setQueueName(consumer.getSubmitQueueName());
		pauser.broadcast(pbean);

		// Now we are paused. Read the submission queue
		Thread.sleep(200);
		List<StatusBean> submitQ = consumer.getSubmissionQueue();
		assertTrue(submitQ.size()>=4);

		Thread.sleep(1000); // Wait for a while and check again that nothing else is

		submitQ = consumer.getSubmissionQueue();
		assertTrue(submitQ.size()>=4);

		// Right then we will reorder it.
		consumer.clearQueue(consumer.getSubmitQueueName());
		consumer.clearQueue(consumer.getStatusSetName());

		// Reverse sort
		Collections.sort(submitQ, new Comparator<StatusBean>() {
			@Override
			public int compare(StatusBean o1, StatusBean o2) {
				int y = Integer.valueOf(o1.getUserName());
				int x = Integer.valueOf(o2.getUserName());
				return (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
		});

		// Start the consumer again
		pbean.setPause(false);
		pauser.broadcast(pbean);

		// Resubmit in new order 4-1
		final List<String> submitted = new ArrayList<>(4); // Order important
		for (StatusBean statusBean : submitQ) {
			System.out.println("Submitting "+statusBean.getName());
			submitter.submit(statusBean);
			submitted.add(statusBean.getName());
		}

		final List<String> run = new ArrayList<>(4); // Order important
		ISubscriber<EventListener> sub = eservice.createSubscriber(consumer.getUri(), consumer.getStatusTopicName());
		sub.addListener(new IBeanListener<StatusBean>() {
			@Override
			public void beanChangePerformed(BeanEvent<StatusBean> evt) {
				// Many events come through here but each scan is run in order
				StatusBean bean = evt.getBean();
				if (!run.contains(bean.getName())) run.add(bean.getName());
			}
		});

		while(!consumer.getSubmissionQueue().isEmpty()) Thread.sleep(100); // Wait for all to run

		Thread.sleep(500); // ensure last one is in the status set

		assertTrue(run.size()>=4);

		assertTrue(submitted.equals(run));

		sub.disconnect();
	}

	@Test
	public void checkDAQ_342() throws Exception {

		consumer.setRunner(new FastRunCreator<StatusBean>(0,100,10,100, true));
		consumer.start();
		
		Thread.sleep(500); 
		IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
		pauser.setStatusSetName(IEventService.CMD_SET);
		pauser.setStatusSetAddRequired(true);

		PauseBean pbean = new PauseBean();
		pbean.setQueueName(consumer.getSubmitQueueName());
		pauser.broadcast(pbean);
		
		Thread.sleep(500);
		assertTrue(!consumer.isActive());
		
		// Submit two things.
		StatusBean bean = null; 
		for (int i = 0; i < 2; i++) {
			bean = new StatusBean();
			bean.setName("Submission"+i);
			bean.setStatus(Status.SUBMITTED);
			bean.setHostName(InetAddress.getLocalHost().getHostName());
			bean.setMessage("Hello World");
			bean.setUniqueId(UUID.randomUUID().toString());
			bean.setUserName(String.valueOf(i));
			submitter.submit(bean);
		}
	
		submitter.reorder(bean, consumer.getSubmitQueueName(), 1);
		
		Thread.sleep(500);
		assertTrue(!consumer.isActive());
		assertEquals(2, submitter.getQueue().size());
		
		pbean.setPause(false);
		pauser.broadcast(pbean);

		Thread.sleep(500);
		assertTrue(consumer.isActive());

	}

	private StatusBean doSubmit() throws Exception {
		return doSubmit("Test");
	}
	private StatusBean doSubmit(String name) throws Exception {

		StatusBean bean = new StatusBean();
		bean.setName(name);
		return doSubmit(bean);
	}
	private StatusBean doSubmit(StatusBean bean) throws Exception {

		bean.setStatus(Status.SUBMITTED);
		bean.setHostName(InetAddress.getLocalHost().getHostName());
		bean.setMessage("Hello World");
		bean.setUniqueId(UUID.randomUUID().toString());

		submitter.submit(bean);

		return bean;
	}
}
