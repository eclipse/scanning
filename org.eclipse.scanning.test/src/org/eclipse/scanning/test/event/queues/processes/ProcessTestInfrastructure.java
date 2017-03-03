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
package org.eclipse.scanning.test.event.queues.processes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueService;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;
import org.eclipse.scanning.event.queues.processes.QueueProcess;
import org.eclipse.scanning.test.event.queues.dummy.DummyHasQueue;
import org.eclipse.scanning.test.event.queues.mocks.MockConsumer;
import org.eclipse.scanning.test.event.queues.mocks.MockEventService;
import org.eclipse.scanning.test.event.queues.mocks.MockPublisher;
import org.eclipse.scanning.test.event.queues.mocks.MockSubmitter;

public class ProcessTestInfrastructure {
	
	private URI uri = null;
	private String topic = null;
	private MockPublisher<Queueable> statPub = new MockPublisher<>(uri, topic);
	
	private Exception threadException;
	private long execTime;
	private final CountDownLatch analysisLatch = new CountDownLatch(1);
	
	private QueueProcess<? extends Queueable, Queueable> qProc;
	private Queueable qBean;
	
	public ProcessTestInfrastructure() {
		this(50);
	}
	
	public ProcessTestInfrastructure(long time) {
		this.execTime = time;
	}
	/**
	 * Generic method for running a queue process. When complete, it releases the execLatch.
	 * waitForExecutionEnd(timeoutMS) should be placed directly after this call.
	 * @param qProc
	 * @param procBean
	 * @throws Exception
	 */
	public <R extends Queueable> void executeProcess(QueueProcess<R, Queueable> qProc, R procBean) throws Exception {
		executeProcess(qProc, procBean, false);
	}
	
	/**
	 * Generic method for running a queue process. When complete, it releases the execLatch.
	 * waitForExecutionEnd(timeoutMS) should be placed directly after this call.
	 * @param qProc
	 * @param procBean
	 * @throws Exception
	 */
	public <R extends Queueable> void executeProcess(QueueProcess<R, Queueable> qProc, R procBean, boolean hasChildQueue) throws Exception { 
		this.qProc = qProc;
		this.qBean = procBean;
		
		//Check the bean doesn't have some weird initial state set on it:
		assertEquals("Wrong initial status", Status.NONE, procBean.getStatus());
		assertEquals("Should not be non-zero percent complete", 0d, procBean.getPercentComplete(), 0);
		
		//TODO Does this need to be in a thread? And do we then need the CountDownLatch?
		Thread th = new Thread(new Runnable() {
			public void run() {
				try {
					qProc.execute();
				} catch (Exception e) {
					threadException = e;
				} finally {
					analysisLatch.countDown();
				}
			}
		});
		th.setDaemon(true);
		th.setPriority(Thread.MAX_PRIORITY);
		th.start();
		
		System.out.println("INFO: Sleeping for "+execTime+"ms to give the processor time to run...");
		Thread.sleep(execTime);
		assertTrue("QueueProcess should be marked executed after execution", qProc.isExecuted());
		
		if (hasChildQueue) {
			//We only want to test the process not the full pipeline
			qProc.broadcast(Status.RUNNING, 99.5d, "Running finished.");
			qProc.getProcessLatch().countDown();
		}
	}
	
	
	/**
	 * Wait a specific amount of time for the execution to end (indicated by 
	 * release of latch by execution thread. If no unlatch happens before 
	 * timeout, check for exceptions thrown by the execution thread and throw 
	 * those or fail the test.
	 * 
	 * @param timeoutMS
	 * @throws Exception
	 */
	public void waitForExecutionEnd(long timeoutMS) throws Exception {
		boolean unLatched = analysisLatch.await(timeoutMS, TimeUnit.MILLISECONDS);
		exceptionCheck(null);
		if (!unLatched) {
			if (threadException == null) {
				fail("Execution did not complete before timeout");
			} else {
				throw new Exception(threadException);
			}
		}
	}
	
	/**
	 * Wait a specific amount of time before calling terminate method on the 
	 * executing {@link QueueProcess}. Then check for exceptions thrown in the 
	 * execution thread.
	 * 
	 * @param timeoutMS
	 * @throws Exception
	 */
	public void waitToTerminate(long timeoutMS) throws Exception {
		waitToTerminate(timeoutMS, false);
	}
	
	/**
	 * Wait a specific amount of time before calling terminate method on the 
	 * executing {@link QueueProcess}. Then check for exceptions thrown in the 
	 * execution thread. If the {@link QueueProcess} creates a child queue, 
	 * send REQUEST_TERMINATE before terminating the process.
	 * 
	 * @param timeoutMS
	 * @param hasChildQueue
	 * @throws Exception
	 */
	public void waitToTerminate(long timeoutMS, boolean hasChildQueue) throws Exception {
		try {
			Thread.sleep(timeoutMS);
		} catch (InterruptedException iEx){
			System.out.println("ERROR: Sleep before termination interrupted!");
			throw iEx;
		}
		//If process creates a child queue, set some arbitrary percent complete
		if (hasChildQueue) {
			qProc.broadcast(Status.REQUEST_TERMINATE, 20d);
		}
		
		qProc.terminate();
		assertTrue("QueueProcess should be marked terminated after termination", qProc.isTerminated());
		exceptionCheck(null);
	}
	
	/**
	 * Check for thrown exceptions in the execution thread. Check not made 
	 * until analysis thread is unlatched.
	 * 
	 * @param timeout a number of ms to wait before checking for exceptions.
	 * @throws Exception
	 */
	public void exceptionCheck(Long timeout) throws Exception {
		if (timeout != null) {
			System.out.println("INFO: Waiting for thread to finish...");
			boolean released = analysisLatch.await(timeout, TimeUnit.MILLISECONDS);
			if (analysisLatch.getCount() == 0 || !released) {
				fail("Thread running the QueueProcess didn't complete within "+timeout+"ms");
			}
		}
		if (threadException != null) {
			throw threadException;
		}

	}
	
	/**
	 * Check the statuses of the first n beans depending on the number of 
	 * statuses and percentages supplied.
	 * @param qBean with ID expected for broadcast beans
	 * @param beanStatuses
	 * @param beanPercent
	 */
	protected void checkFirstBroadcastBeanStatuses(Status[] beanStatuses, Double[] beanPercent) throws Exception {
		if (beanStatuses.length != beanPercent.length) fail("Number of Statuses and percents to test must be equal");
		
		List<Queueable> broadcastBeans = getBroadcastBeans();
		if (broadcastBeans.size() < beanStatuses.length) fail("Too few beans broadcast (expected: "+beanStatuses.length+"; was: "+broadcastBeans.size()+")");
		
		for (int i = 0; i < beanStatuses.length; i++) {
			Queueable broadBean = broadcastBeans.get(i);
			
			if (!broadBean.getUniqueId().equals(qBean.getUniqueId())){
				throw new EventException(i+"th bean is not the bean we were looking for");
			}
			assertEquals(i+"th bean has wrong status", beanStatuses[i], broadBean.getStatus());
			assertEquals(i+"th bean has wrong percent", beanPercent[i], broadBean.getPercentComplete(), 0);
			if (i > 0){
				assertEquals(i+"th bean has wrong previous status", beanStatuses[i-1], broadBean.getPreviousStatus());
			}
		}
	}
	
	/**
	 * Check the statuses of the first, last and (optionally) the penultimate 
	 * bean, for any process outcome, depending on the supplied state.
	 * @param qBean
	 * @param state
	 * @param prevBean
	 * @throws EventException
	 */
	public void checkLastBroadcastBeanStatuses(Status state, boolean prevBean) throws EventException {
		Double percentComplete = -1d;
		Status previousBeanState = null;
		if (state.equals(Status.NONE)) {
			percentComplete = 0d;
		} else if (state.equals(Status.COMPLETE)) {
			percentComplete = 100d;
			previousBeanState = Status.RUNNING;
		} else if (state.equals(Status.TERMINATED)) {
			previousBeanState = Status.REQUEST_TERMINATE;
		}
		
		StatusBean lastBean, penultimateBean, firstBean;
		List<? extends StatusBean> broadcastBeans = getBroadcastBeans();
		
		//First bean should be RUNNING.
		firstBean = broadcastBeans.get(0);
		if (!firstBean.getUniqueId().equals(qBean.getUniqueId())){
			throw new EventException("First bean is not the bean we were looking for");
		}
		assertEquals("First bean should be running", Status.RUNNING, firstBean.getStatus());
		
		//Get last bean - needed for penultimate bean analysis too
		lastBean = broadcastBeans.get(broadcastBeans.size()-1);
		double lastBPercComp = lastBean.getPercentComplete();
		
		//Penultimate bean should have status depending on the above if/else block
		if (prevBean) {
			penultimateBean = broadcastBeans.get(broadcastBeans.size()-2);
			if (!penultimateBean.getUniqueId().equals(qBean.getUniqueId())){
				throw new EventException("Penultimate bean is not the bean we were looking for");
			}
			assertEquals("Second to last bean has wrong status", previousBeanState, penultimateBean.getStatus());
			double penuBPercComp = penultimateBean.getPercentComplete();
			assertTrue("Percent complete greater than last bean's", lastBPercComp >= penuBPercComp);
			assertTrue("The percent complete is not between 0% & 100% (is: "+lastBPercComp+")", ((penuBPercComp > 0d) && (penuBPercComp < 100d))); 
		}
		
		//Last bean should have status in args and percent complete defined in if/else block
		if (!lastBean.getUniqueId().equals(qBean.getUniqueId())){
			throw new EventException("Last bean is not the bean we were looking for");
		}
		assertTrue("Last bean is not final (was: "+lastBean.getStatus()+")", lastBean.getStatus().isFinal());
		assertEquals("Last bean has wrong status", state, lastBean.getStatus());
		if (percentComplete > 0) {
			assertEquals("Last bean has wrong percent complete", percentComplete, lastBPercComp, 0);
		} else {
			assertTrue("The percent complete is not between 0% & 100% (is: "+lastBPercComp+")", ((lastBPercComp > 0d) && (lastBPercComp < 100d)));
		}
	}
	
	/**
	 * Get beans published to child queue from the {@link MockPublisher} held 
	 * by the configured {@link MockEventService}. Check their {@link Status}es
	 *  and their names against args.
	 *  
	 * @param state
	 * @param childBeanNames
	 */
	@SuppressWarnings("unchecked")
	protected void checkLastBroadcastChildBeanStatus(Status state, String[] childBeanNames) {
		MockPublisher<?> mp = (MockPublisher<?>) ServicesHolder.getEventService().createPublisher(null, null);
		List<DummyHasQueue> childBeans = (List<DummyHasQueue>) getPublishedBeans(mp);
		
		List<String> childNames = Arrays.asList(childBeanNames);
		boolean childBeanBroadcast = false;
		for (DummyHasQueue child : childBeans) {
			//We check that the bean is from the child queue and that it has correct state
			if (!childNames.contains(child.getName())) continue;
			//We've got a child bean
			childBeanBroadcast = true;
			if (child.getStatus() == state) return;
		}
		
		//If we got child beans, but none final, that's a fail, as is no child beans.
		if (childBeanBroadcast) {
			fail("No child bean broadcast with Status "+state);
		} else {
			fail("No child beans broadcast");
		}		
	}
	
	/**
	 * Test whether the {@link MockConsumer}s of the {@link IQueueService} held
	 *  in the referenced {@link MockEventService} have been stopped.
	 *  
	 * @param mockEvServ
	 * @param qServ
	 */
	public void checkConsumersStopped(MockEventService mockEvServ, IQueueService qServ) {
		Map<String, MockConsumer<? extends StatusBean>> consumers = mockEvServ.getRegisteredConsumers();
		
		for (Map.Entry<String, MockConsumer<? extends StatusBean>> entry : consumers.entrySet()) {
			//We don't need to check the job-queue
			if (entry.getKey().equals(qServ.getJobQueueID())) continue;
			assertTrue("Consumer was not stopped (this was expected)", entry.getValue().isStopped());
		}
	}
	
	/**
	 * Wait for bean to reach a specific {@link State} in a given time.
	 * 
	 * @param state
	 * @param timeout
	 * @throws Exception
	 */
	public void waitForBeanStatus(Status state, long timeout) throws Exception {
		waitForBeanState(state, false, timeout);
	}
	
	/**
	 * Wait for a bean to reach any final {@link State} in a given time. 
	 * @param timeout
	 * @throws Exception
	 */
	public void waitForBeanFinalStatus(long timeout) throws Exception {
		waitForBeanState(null, true, timeout);
	}
	
	/**
	 * Wait for the bean configured in the execute() method to reach the 
	 * specified {@link State} or to reach a final {@link State}. A timeout is 
	 * given to limit the waiting time for this to occur in.
	 * 
	 * @param state
	 * @param isFinal
	 * @param timeout
	 * @throws Exception
	 */
	private void waitForBeanState(Status state, boolean isFinal, long timeout) throws Exception {
		StatusBean lastBean= ((MockPublisher<? extends StatusBean>)statPub).getLastQueueable();
		long startTime = System.currentTimeMillis();
		long runTime = 0;
		
		while (runTime <= timeout) {
			if ((lastBean != null) && (lastBean.getUniqueId().equals(qBean.getUniqueId()))) {
				if ((lastBean.getStatus().equals(state)) || (lastBean.getStatus().isFinal() && isFinal)) {
					return;
				}
			}
			Thread.sleep(50);
			runTime = System.currentTimeMillis() - startTime;
			if (threadException != null) {
				throw new EventException(threadException);
			}
			lastBean = ((MockPublisher<? extends StatusBean>)statPub).getLastQueueable();
		}
		
		String beanStatus;
		if (lastBean == null) {
			beanStatus = "~~ bean is null ~~";
		} else {
			beanStatus = lastBean.getStatus().toString();
		}
		throw new Exception("Bean state not reached before timeout (was: "+beanStatus+"; with message: "+lastBean.getMessage()+").");
	}
	
	/**
	 * Test whether submitted beans (extending {@link StatusBean}) on a given 
	 * {@link MockSubmitter} (usually of a child queue of the queueBean under 
	 * test) have the SUBMITTED {@link State} and the fields hostname, username
	 *  and beamline set.
	 *  
	 * @param ms
	 * @param queueID
	 * @throws Exception
	 */
	public void checkSubmittedBeans(MockSubmitter<? extends StatusBean> ms, String queueID) throws Exception {
		List<? extends StatusBean> submittedBeans = getSubmittedBeans(ms, queueID);
		assertTrue("No beans in the final status set", submittedBeans.size() != 0);
		for (StatusBean dummy : submittedBeans) {
			//First check beans are in final state
			assertEquals("Final bean "+dummy.getName()+" is not submitted (was: "+dummy.getStatus()+")", Status.SUBMITTED ,dummy.getStatus());
			//Check the properties of the parent atom/bean have been correctly passed down
			assertFalse("No hostname set", dummy.getHostName() == null);
			assertEquals("Incorrect hostname", qBean.getHostName(), dummy.getHostName());
			assertFalse("No username set", dummy.getUserName() == null);
			assertEquals("Incorrect username", qBean.getUserName(), dummy.getUserName());
			//
			if (dummy instanceof QueueAtom) {
				assertFalse("No beamline set", ((QueueAtom)dummy).getBeamline() == null);
				assertEquals("Incorrect beamline", qBean.getBeamline(), ((QueueAtom)dummy).getBeamline());
			} else if (dummy instanceof ScanBean) {
				assertFalse("No beamline set", ((ScanBean)dummy).getBeamline() == null);
				assertEquals("Incorrect beamline", qBean.getBeamline(), ((ScanBean)dummy).getBeamline());
			}
		}
	}
	
	/**
	 * From a given {@link MockSubmitter}, get all the beans submitted to queueID.
	 * 
	 * @param ms
	 * @param queueID
	 * @return
	 */
	public List<? extends StatusBean> getSubmittedBeans(MockSubmitter<? extends StatusBean> ms, String queueID) {
		String qName = queueID+IQueue.SUBMISSION_QUEUE_SUFFIX;
		return ms.getQueue(qName);
	}
	
	/**
	 * Request all the broadcast beans from the {@link MockPublisher}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Queueable> getBroadcastBeans() {
		return (List<Queueable>) getPublishedBeans(statPub);
	}
	
	/**
	 * Get the last bean broadcast to statpub.
	 * 
	 * @return
	 */
	public StatusBean getLastBroadcastBean() {
		List<?> publBeans = getPublishedBeans(statPub);
		return (StatusBean) publBeans.get(publBeans.size()-1);
	}
	
	/**
	 * Get the last bean published to the given {@link MockPublisher}.
	 * 
	 * @param mockPub
	 * @return
	 */
	public IdBean getLastPublishedBean(MockPublisher<?> mockPub) {
		List<?> publBeans = getPublishedBeans(mockPub);
		return (IdBean) publBeans.get(publBeans.size()-1);
	}
	
	/**
	 * Get the complete list of beans published to the given {@link MockPublisher}.
	 * 
	 * @param mockPub
	 * @return
	 */
	public List<?> getPublishedBeans(MockPublisher<?> mockPub) {
		List<?> publBeans = mockPub.getBroadcastBeans();
		if (publBeans.size() == 0) fail("No beans broadcast to publisher");
		return publBeans;
	}
	
	/**
	 * Get the statPub
	 * 
	 * @return
	 */
	public MockPublisher<Queueable> getPublisher() {
		return statPub;
	}
	
	/**
	 * Get the exception thrown by the execution thread (if any).
	 * 
	 * @return
	 */
	public Exception getProcThreadException() {
		return threadException;
	}
	
	/**
	 * Rest the infrastructure used by this ProcessTestInfrastructure instance.
	 */
	public void resetInfrastructure() {
		statPub = new MockPublisher<>(uri, topic);
		qProc = null;
		qBean = null;
	}

}
