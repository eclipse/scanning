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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.PauseBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubmitter;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.dry.DryRunCreator;
import org.eclipse.scanning.api.event.dry.FastRunCreator;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.test.BrokerTest;
import org.junit.Test;

public class AbstractMConsumerTest extends BrokerTest{

	protected IEventService          eservice;
	protected ISubmitter<StatusBean> submitter;
	protected IConsumer<StatusBean>  consumer;

    @Test
    public void testTwoConsumersOneSubmit() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		IConsumer<StatusBean> consumer2   = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			consumer2.setName("Test Consumer "+2);
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.setRunner(new DryRunCreator<StatusBean>(false));
			consumer2.start();
			
			checkSubmission();
			
		} finally {

			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.disconnect();
		}
    }
    
    @Test
    public void testTwoConsumersTenSubmits() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		IConsumer<StatusBean> consumer2   = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			consumer2.setName("Test Consumer "+2);
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.setRunner(new DryRunCreator<StatusBean>(false));
			consumer2.start();
			
			List<StatusBean> submissions = new ArrayList<StatusBean>(10);
			for (int i = 0; i < 10; i++) {
				submissions.add(doSubmit("Test "+i));
				System.out.println("Submitted: Test "+i);
				Thread.sleep(10);
			}
			 	
			Thread.sleep(16000); // 10000 to do the loop, 6000 for luck
			
			checkStatus(submissions);
			
		} finally {
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.disconnect();
		}
    }
    
    @Test
    public void testTwoConsumersPausedTenSubmits() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		IConsumer<StatusBean> consumer2   = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			consumer2.setName("Test Consumer "+2);
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.setRunner(new DryRunCreator<StatusBean>(false));
			consumer2.start();
			
			List<StatusBean> submissions = new ArrayList<StatusBean>(10);
			for (int i = 0; i < 10; i++) {
				submissions.add(doSubmit("Test "+i));
				System.out.println("Submitted: Test "+i);
				Thread.sleep(10);
			}
			 	
			Thread.sleep(2000); // Let them do something
			
			IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
			PauseBean pbean = new PauseBean();
			pbean.setQueueName(consumer.getSubmitQueueName());
			pauser.broadcast(pbean);

			Thread.sleep(2000); // Let them pause
			
			assertTrue(!consumer.isActive());
			assertTrue(!consumer2.isActive());
			
			pbean.setPause(false);
			pauser.broadcast(pbean);

			Thread.sleep(1000);

			assertTrue(consumer.isActive());
			assertTrue(consumer2.isActive());
	
			Thread.sleep(11000); // 10000 to do the loop, 1000 for luck
			checkStatus(submissions);
			
		} finally {
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.disconnect();
		}
    }
    
    @Test
    public void testReorderingAPausedQueueTwoConsumers() throws Exception {
    	
		consumer.setRunner(new FastRunCreator<StatusBean>(100, true));
		consumer.start();
		
		IConsumer<StatusBean> consumer2   = eservice.createConsumer(consumer.getUri(), consumer.getSubmitQueueName(), consumer.getStatusSetName(), consumer.getStatusTopicName(), IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			consumer2.setName("Test Consumer "+2);
			consumer2.setRunner(new FastRunCreator<StatusBean>(100, true));
			consumer2.start();

			// Bung ten things on there.
			for (int i = 0; i < 10; i++) {
				StatusBean bean = new StatusBean();
				bean.setName("Submission"+i);
				bean.setStatus(Status.SUBMITTED);
				bean.setHostName(InetAddress.getLocalHost().getHostName());
				bean.setMessage("Hello World");
				bean.setUniqueId(UUID.randomUUID().toString());
				bean.setUserName(String.valueOf(i));
				submitter.submit(bean);
			}
	
			Thread.sleep(100);
			IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
			PauseBean pbean = new PauseBean();
			pbean.setQueueName(consumer.getSubmitQueueName());
			pauser.broadcast(pbean);
			
			// Now we are paused. Read the submission queue
			Thread.sleep(100);
			List<StatusBean> submitQ = consumer.getSubmissionQueue();
			assertEquals(8, submitQ.size());
		
			Thread.sleep(2000); // Wait for 0 to run and check again that nothing else is
			
			submitQ = consumer.getSubmissionQueue();
			assertEquals(8, submitQ.size()); // It really has paused has it?
			
			// Right then we will reorder it.
			consumer.cleanQueue(consumer.getSubmitQueueName());
			
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
			
			// Resubmit in new order 9-2
			for (StatusBean statusBean : submitQ) submitter.submit(statusBean);
			
			final Map<String, StatusBean> run = new LinkedHashMap<>(8); // Order important
			ISubscriber<EventListener> sub = eservice.createSubscriber(consumer.getUri(), consumer.getStatusTopicName());
			sub.addListener(new IBeanListener<StatusBean>() {
				@Override
				public void beanChangePerformed(BeanEvent<StatusBean> evt) {
					// Many events come through here but each scan is run in order
					StatusBean bean = evt.getBean();
					run.put(bean.getName(), bean);
				}
			});
	
			while(!consumer.getSubmissionQueue().isEmpty()) Thread.sleep(1000); // Wait for all to run
			
			Thread.sleep(200); // ensure last one is running or ran.
			
			assertEquals(8, run.size());
			
		} finally {
			consumer2.disconnect();
		}
    }


    @Test
    public void testTenConsumersTenSubmitsPaused() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		List<IConsumer<StatusBean>> consumers   = new ArrayList<>(9);
		try {
			for (int i = 2; i < 11; i++) {
				IConsumer<StatusBean> c = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
				c.setName("Test Consumer "+i);
				c.clearQueue(IEventService.SUBMISSION_QUEUE);
				c.clearQueue(IEventService.STATUS_SET);
				c.setRunner(new DryRunCreator<StatusBean>(false));
				c.start();
				consumers.add(c);
			}
			
			List<StatusBean> submissions = new ArrayList<StatusBean>(10);
			for (int i = 0; i < 10; i++) {
				submissions.add(doSubmit("Test "+i));
				System.out.println("Submitted: Test "+i);
				Thread.sleep(10);
			}
			 	
			Thread.sleep(2000);

			IPublisher<PauseBean> pauser = eservice.createPublisher(submitter.getUri(), IEventService.CMD_TOPIC);
			PauseBean pbean = new PauseBean();
			pbean.setQueueName(consumer.getSubmitQueueName());
			pauser.broadcast(pbean);
			

			Thread.sleep(2000); // Let them pause
			
			assertTrue(!consumer.isActive());
			for (IConsumer<StatusBean> cons : consumers) assertTrue(!cons.isActive());
			
			pbean.setPause(false);
			pauser.broadcast(pbean);

			Thread.sleep(1000);

			assertTrue(consumer.isActive());
			for (IConsumer<StatusBean> cons : consumers) assertTrue(cons.isActive());
	
			Thread.sleep(13000); // 10000 to do the loop, 3000 for luck
			
			checkStatus(submissions);
			
		} finally {
			for (IConsumer<StatusBean> c : consumers) {
				c.clearQueue(IEventService.SUBMISSION_QUEUE);
				c.clearQueue(IEventService.STATUS_SET);
				c.disconnect();
			}
		}
    }


    @Test
    public void testTwoConsumersTenSubmitsThreads() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		IConsumer<StatusBean> consumer2   = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
		try {
			consumer2.setName("Test Consumer "+2);
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.setRunner(new DryRunCreator<StatusBean>(false));
			consumer2.start();
			
			final List<StatusBean> submissions = new ArrayList<StatusBean>(10);
			for (int i = 0; i < 10; i++) {
				final int finalI = i;
				final Thread thread = new Thread(new Runnable() {
					public void run () {
						try {
							submissions.add(doSubmit("Test "+finalI));
							System.out.println("Submitted: Thread Test "+finalI);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				thread.setName("Thread "+i);
				thread.setDaemon(true);
				thread.start();
				
				Thread.sleep(100);
			}

			Thread.sleep(16000); // 10000 to do the loop, 6000 for luck
			
			checkStatus(submissions);
			
		} finally {
			consumer2.clearQueue(IEventService.SUBMISSION_QUEUE);
			consumer2.clearQueue(IEventService.STATUS_SET);
			consumer2.disconnect();
		}
    }
    
    
    
    @Test
    public void testTenConsumersTenSubmits() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		List<IConsumer<StatusBean>> consumers   = new ArrayList<>(9);
		try {
			for (int i = 2; i < 11; i++) {
				IConsumer<StatusBean> c = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
				c.setName("Test Consumer "+i);
				c.clearQueue(IEventService.SUBMISSION_QUEUE);
				c.clearQueue(IEventService.STATUS_SET);
				c.setRunner(new DryRunCreator<StatusBean>(false));
				c.start();
				consumers.add(c);
			}
			
			List<StatusBean> submissions = new ArrayList<StatusBean>(10);
			for (int i = 0; i < 10; i++) {
				submissions.add(doSubmit("Test "+i));
				System.out.println("Submitted: Test "+i);
				Thread.sleep(10);
			}
			 	
			Thread.sleep(16000); // 10000 to do the loop, 6000 for luck
			
			checkStatus(submissions);
			
		} finally {
			for (IConsumer<StatusBean> c : consumers) {
				c.clearQueue(IEventService.SUBMISSION_QUEUE);
				c.clearQueue(IEventService.STATUS_SET);
				c.disconnect();
			}
		}
    }

   
    
    @Test
    public void testTenConsumersTenSubmitsThreads() throws Exception {
    	
		consumer.setRunner(new DryRunCreator<StatusBean>(false));
		consumer.start();
 
		List<IConsumer<StatusBean>> consumers   = new ArrayList<>(9);
		try {
			for (int i = 2; i < 11; i++) {
				IConsumer<StatusBean> c = eservice.createConsumer(consumer.getUri(), IEventService.SUBMISSION_QUEUE, IEventService.STATUS_SET, IEventService.STATUS_TOPIC, IEventService.HEARTBEAT_TOPIC, IEventService.CMD_TOPIC);
				c.setName("Test Consumer "+i);
				c.clearQueue(IEventService.SUBMISSION_QUEUE);
				c.clearQueue(IEventService.STATUS_SET);
				c.setRunner(new DryRunCreator<StatusBean>(false));
				c.start();
				consumers.add(c);
			}
			
			final List<StatusBean> submissions = new ArrayList<StatusBean>(10);
			for (int i = 0; i < 10; i++) {
				final int finalI = i;
				final Thread thread = new Thread(new Runnable() {
					public void run () {
						try {
							submissions.add(doSubmit("Test "+finalI));
							System.out.println("Submitted: Thread Test "+finalI);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				thread.setName("Thread "+i);
				thread.setDaemon(true);
				thread.start();
				
				Thread.sleep(100);
			}
			 	
			Thread.sleep(16000); // 10000 to do the loop, 6000 for luck
			
			checkStatus(submissions);
			
		} finally {
			for (IConsumer<StatusBean> c : consumers) {
				c.clearQueue(IEventService.SUBMISSION_QUEUE);
				c.clearQueue(IEventService.STATUS_SET);
				c.disconnect();
			}
		}
    }


    private void checkStatus(List<StatusBean> submissions) throws Exception {
    	
    	List<StatusBean> stati = consumer.getStatusSet();
		if (stati.size()!=10) throw new Exception("Unexpected status size in queue! Should be 10 size is "+stati.size());
		
		for (int i = 0; i < 10; i++) {
			
			StatusBean complete = stati.get(i);
			if (!complete.getName().equals("Test "+(9-i))) {
				throw new Exception("Unexpected run order detected! bean is named "+complete.getName()+" and should be 'Test "+(9-i)+"'");
			}
			
			StatusBean bean     = submissions.get(i);
	       	if (complete.equals(bean)) {
	       		throw new Exception("The bean from the status queue was the same as that submitted! It should have a different status. q="+complete+" submit="+bean);
	       	}
	        
	       	if (complete.getStatus()!=Status.COMPLETE) {
	       		throw new Exception("The bean in the queue is not complete!"+complete);
	       	}
	       	if (complete.getPercentComplete()<100) {
	       		throw new Exception("The percent complete is less than 100!"+complete);
	       	}
		}		
	}
 

    private void checkSubmission() throws Exception {
    	
        StatusBean bean = doSubmit();
	 	
		Thread.sleep(14000); // 10000 to do the loop, 4000 for luck
		
		List<StatusBean> stati = consumer.getStatusSet();
		if (stati.size()!=1) throw new Exception("Unexpected status size in queue! size = "+stati.size());
		
		StatusBean complete = stati.get(0);
		
       	if (complete.equals(bean)) {
       		throw new Exception("The bean from the status queue was the same as that submitted! It should have a different status. q="+complete+" submit="+bean);
       	}
        
       	if (complete.getStatus()!=Status.COMPLETE) {
       		throw new Exception("The bean in the queue is not complete!"+complete);
       	}
       	if (complete.getPercentComplete()<100) {
       		throw new Exception("The percent complete is less than 100!"+complete);
       	}
	}



	private StatusBean doSubmit() throws Exception {
 	   return doSubmit("Test");
    }
    private StatusBean doSubmit(String name) throws Exception {

 		StatusBean bean = new StatusBean();
 		bean.setName(name);
 		bean.setStatus(Status.SUBMITTED);
 		bean.setHostName(InetAddress.getLocalHost().getHostName());
 		bean.setMessage("Hello World");
 		bean.setUniqueId(UUID.randomUUID().toString());

 		submitter.submit(bean);
 		
 		return bean;
 	}

}
