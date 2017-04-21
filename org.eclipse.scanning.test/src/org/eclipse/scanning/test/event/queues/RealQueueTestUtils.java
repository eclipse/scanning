package org.eclipse.scanning.test.event.queues;

import java.net.URI;
import java.util.ArrayList;
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
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.event.queues.ServicesHolder;

public class RealQueueTestUtils {
	
	private static URI uri;
	
	private static IEventService evServ;
	
	private static List<ConsumerCommandBean> cmdBeans;
	private static CountDownLatch cmdLatch;
	private static List<StatusBean> statusBeans;
	
	public static void initialise(URI uri) {
		RealQueueTestUtils.uri = uri;
		evServ = ServicesHolder.getEventService();
		
		cmdBeans = new ArrayList<>();
		statusBeans = new ArrayList<>();
	}
	
	/**
	 * Listen to a topic, waiting until a given number of bean events happen 
	 * before returning. If the timeout is exceeded before the requested 
	 * number of events, return anyway printing a warning.
	 * 
	 * @param topicName
	 * @param timeout
	 * @param nBeans
	 * @throws EventException
	 * @throws InterruptedException
	 */
	public static void waitForBean(String topicName, Long timeout, Integer nBeans, StatusBean bean) throws EventException, InterruptedException {
		if (timeout == null) timeout = 3000L; //Defaults
		if (nBeans == null) nBeans = 1;
		
		CountDownLatch beanLatch = waitForBean(topicName, statusBeans, nBeans, bean);
		
		boolean released = beanLatch.await(timeout, TimeUnit.MILLISECONDS);
		if (!released) {
			System.out.println("\n**************\nTimed out waiting for "+topicName+"\n**************");
		}
	}
	
	/**
	 * Listen to a command topic, waiting until a given number of command bean 
	 * events happen before returning. If the timeout is exceeded before the 
	 * requested number of events, return anyway printing a warning.
	 * 
	 * @param commandTopicName
	 * @param nBeans
	 * @throws EventException
	 * @throws  
	 */
	public static void listenerForCommandBeans(String commandTopicName, Integer nBeans) throws EventException {
		if (nBeans == null) nBeans = 1; //Default
		
		cmdLatch = waitForBean(commandTopicName, cmdBeans, nBeans, null);
	}
	
	private static <T extends IdBean> CountDownLatch waitForBean(String topicName, List<T> heardBeans, Integer nBeans, IdBean bean) throws EventException {
		final CountDownLatch beanLatch = new CountDownLatch(nBeans);
		ISubscriber<IBeanListener<T>> queueListener = evServ.createSubscriber(uri, topicName);
		queueListener.addListener(new IBeanListener<T>() {

			@Override
			public void beanChangePerformed(BeanEvent<T> evt) {
				heardBeans.add(evt.getBean());
				//When listening for a particular bean, only release on hearing it
				if (bean != null && evt.getBean().getUniqueId().equals(bean.getUniqueId())) {
					beanLatch.countDown();
				} else{
					beanLatch.countDown();
				}
			}
		});
		return beanLatch;
	}
	
	public static ConsumerCommandBean waitToGetCmdBean(Long timeout) throws InterruptedException {
		if (timeout == null) timeout = 3000L; //Default
		
		boolean released = cmdLatch.await(timeout, TimeUnit.MILLISECONDS);
		if (!released) {
			System.out.println("\n**************\nTimed out waiting for command bean\n**************");
		}
		
		if (cmdBeans.size() == 0) return null;
		return cmdBeans.get(cmdBeans.size()-1);
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

}
