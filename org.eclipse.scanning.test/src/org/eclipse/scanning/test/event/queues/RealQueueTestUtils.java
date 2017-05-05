package org.eclipse.scanning.test.event.queues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IConsumer;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.scan.IScanListener;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.event.scan.ScanEvent;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;

public class RealQueueTestUtils {
	
	private static URI uri;
	
	private static IEventService evServ;
	private static IQueueControllerService queueControl;
	private static List<ISubscriber<? extends EventListener>> subscList; 
	
	private static List<ConsumerCommandBean> cmdBeans;
	private static List<StatusBean> statusBeans;
	
	//For listening to ScanServlets
	private static List<ScanBean> startEvents, endEvents;
	
	public static void initialise(URI uri) {
		RealQueueTestUtils.uri = uri;
		evServ = ServicesHolder.getEventService();
		queueControl = ServicesHolder.getQueueControllerService();
		
		cmdBeans = new ArrayList<>();
		statusBeans = new ArrayList<>();
		
		subscList = new ArrayList<>();
		
		//For listening to ScanServlets
		startEvents = new ArrayList<>();
		endEvents = new ArrayList<>();
		
	}
	
	public static void reset() throws EventException {
		cmdBeans.clear();
		statusBeans.clear();
		for (ISubscriber<?> subsc : subscList) {
			subsc.disconnect();
		}
		subscList.clear();
		
		//For listening to ScanServlets
		startEvents.clear();
		endEvents.clear();
	}
	
	public static void dispose() throws EventException  {
		reset();
		RealQueueTestUtils.uri = null;
		evServ = null;
		queueControl = null;
	}
	
	/**
	 * Set up listener for a StatusBean with a final state. Return a 
	 * CountDownLatch which releases on hearing such a bean.
	 */
	public static <T extends StatusBean> CountDownLatch createFinalStateBeanWaitLatch(Queueable bean, String queueID) 
			throws EventException {
		return createStatusBeanWaitLatch(queueID, null, bean, null, true);
	}
	
	/**
	 * Set up a listener for a bean in a particular queue or on a topic with a 
	 * state or which might be final.
	 */
	private static <T extends StatusBean> CountDownLatch createStatusBeanWaitLatch(String queueID, String topicName, T bean, Status state, Boolean isFinal) 
			throws EventException {
		final CountDownLatch statusLatch = new CountDownLatch(1);
		
		//Make sure the topic to listen on is set (has it been supplied?)
				if (topicName == null) {
					IQueue<? extends StatusBean> queue = queueControl.getQueue(queueID);
					topicName = queue.getStatusTopicName();
				}
		
		//Create our subscriber and add the listener
		ISubscriber<IBeanListener<? extends IdBean>>statusSubsc = evServ.createSubscriber(uri, topicName);
		statusSubsc.addListener(new IBeanListener<T>() {

			@Override
			public void beanChangePerformed(BeanEvent<T> evt) {
				StatusBean evtBean = evt.getBean();
				statusBeans.add(evtBean);
				if (evtBean.getUniqueId().equals(bean.getUniqueId())) {
					if ((evtBean.getStatus() == state) || (evtBean.getStatus().isFinal() && isFinal)) {
						statusLatch.countDown();
					}
				}
			}
		});
		subscList.add(statusSubsc);
		return statusLatch;
	}
	
	/**
	 * Set up listener for a given number of ConsumerCommandBeans on a given 
	 * topic. Return a CountDownLatch which releases on hearing such a bean.
	 */
	public static <T extends ConsumerCommandBean> CountDownLatch createCommandBeanWaitLatch(String topicName, int nBeans) throws EventException {
		return createCommandBeanWaitLatch(topicName, null, null, nBeans);
	}
	
	/**
	 * Set up a listener for a given number of beans on a queue or topic.
	 */
	private static <T extends ConsumerCommandBean> CountDownLatch createCommandBeanWaitLatch(String topicName, String queueID, T bean, Integer nBeans) 
			throws EventException {
		if (nBeans == null) nBeans = 1;
		final CountDownLatch commandLatch = new CountDownLatch(nBeans);
		
		//Make sure the topic to listen on is set (has it been supplied?)
		if (topicName == null) {
			IQueue<? extends StatusBean> queue = queueControl.getQueue(queueID);
			topicName = queue.getStatusTopicName();
		}
		
		//Create our subscriber and add the listener
		ISubscriber<IBeanListener<? extends IdBean>> cmdSubsc = evServ.createSubscriber(uri, topicName);
		cmdSubsc.addListener(new IBeanListener<T>() {

			@Override
			public void beanChangePerformed(BeanEvent<T> evt) {
				ConsumerCommandBean evtBean = evt.getBean();
				cmdBeans.add(evtBean);
				//When listening for a particular bean, only release on hearing it
				if (bean != null && evt.getBean().getUniqueId().equals(bean.getUniqueId())) {
					commandLatch.countDown();
				} else{
					commandLatch.countDown();
				}
			}
		});
		subscList.add(cmdSubsc);
		return commandLatch;
	}
	
	public static CountDownLatch createScanEndEventWaitLatch(ScanBean bean, String topicName) throws EventException {
		final CountDownLatch scanLatch = new CountDownLatch(1);
		
		ISubscriber<IScanListener> beanEvtSubsc = evServ.createSubscriber(uri, topicName);
		beanEvtSubsc.addListener(new IScanListener() {

			@Override
			public void scanStateChanged(ScanEvent evt) {
				if (evt.getBean().scanStart()) {
					startEvents.add(evt.getBean()); // Should be just one
				}
				if (evt.getBean().scanEnd()) {
					endEvents.add(evt.getBean());
					scanLatch.countDown();
				}
			}
		});
		subscList.add(beanEvtSubsc);
		return scanLatch;
	}
	
	/**
	 * Wait for the given CountDownLatch to countdown or to exceed its timeout 
	 * (10000ms if no time specified).
	 */
	public static void waitForEvent(CountDownLatch latch) throws InterruptedException {
		waitForEvent(latch, 10000, false);
	}
	public static void waitForEvent(CountDownLatch latch, long timeout) throws InterruptedException {
		waitForEvent(latch, timeout, false);
	}
	public static void waitForEvent(CountDownLatch latch, long timeout, Boolean noFail) throws InterruptedException {
		//We may get stuck if the consumer finishes processing faster than the test works through
		//If so, we need to test for a non-empty status set with last bean status equal to our expectation

		//Once finished, check whether the latch was released or timedout
		boolean released = latch.await(timeout, TimeUnit.MILLISECONDS);
		if (released) {
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~\n Final state reached\n~~~~~~~~~~~~~~~~~~~~~~~~~");
		} else {
			System.out.println("#########################\n No final state reported\n#########################");
			if (!noFail) {
				fail("No final state reported");
			}
		}
	}
	
	/**
	 * In addition to waiting for an event to occur, this method then returns the last heard command bean.
	 * @param latch
	 * @param timeout
	 * @return
	 * @throws InterruptedException
	 */
	public static ConsumerCommandBean waitToGetCmdBean(CountDownLatch latch, Long timeout, boolean noBeanExpected) throws InterruptedException {
		waitForEvent(latch, timeout, noBeanExpected);
		
		if (noBeanExpected) {
			assertEquals("Command beans heard, but didn't expect any", 0, cmdBeans.size());
			return null;
		} else {
			if (cmdBeans.size() == 0) fail("No command beans recorded.");
			return cmdBeans.get(cmdBeans.size()-1);
		}
	}
	
	/**
	 * Wait for the submission queue of the given consumer to reach a certain 
	 * size before returning. If the timeout is exceeded before the requested 
	 * number of events, return anyway printing a warning.
	 * @param cons
	 * @param timeout
	 * @param nBeans
	 * @throws EventException
	 * @throws InterruptedException
	 */
	public static void waitForSubmitQueueLength(IConsumer<?> cons, Long timeout, Integer nBeans) throws EventException, InterruptedException {
		if (timeout == null) timeout = 3000L; //Defaults
		if (nBeans == null) nBeans = 1;
		
		Long startTime = System.currentTimeMillis();
		while ((System.currentTimeMillis() - startTime) < timeout) {
			if (cons.getSubmissionQueue().size() >= nBeans) {
				return;
			}
			Thread.sleep(50);
		}
		System.out.println("\n**************\nTimed out waiting for "+cons.getSubmitQueueName()+"\n**************");
	}

	public static List<ScanBean> getStartEvents() {
		return startEvents;
	}

	public static List<ScanBean> getEndEvents() {
		return endEvents;
	}
}
