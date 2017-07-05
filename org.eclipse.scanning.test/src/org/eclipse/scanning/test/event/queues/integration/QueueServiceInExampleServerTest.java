package org.eclipse.scanning.test.event.queues.integration;

import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.dawnsci.json.MarshallerService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.IdBean;
import org.eclipse.scanning.api.event.alive.ConsumerCommandBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.queues.IQueue;
import org.eclipse.scanning.api.event.queues.IQueueControllerService;
import org.eclipse.scanning.api.event.queues.beans.PositionerAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;
import org.eclipse.scanning.event.EventServiceImpl;
import org.eclipse.scanning.example.classregistry.ScanningExampleClassRegistry;
import org.eclipse.scanning.points.classregistry.ScanningAPIClassRegistry;
import org.eclipse.scanning.points.serialization.PointsModelMarshaller;
import org.eclipse.scanning.test.ScanningTestClassRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueueServiceInExampleServerTest {
	
	private static URI uri;
	private static IEventService eservice;
	private static MarshallerService marshaller;
	
	private IQueueControllerService queueControl;
	private ISubscriber<IBeanListener<StatusBean>> statusSubsc;
	private String jqID;
	
	@BeforeClass
	public static void setUpTestSuite() throws URISyntaxException {
		uri = new URI("failover:(tcp://localhost:61616)?startupMaxReconnectAttempts=3");
		
		marshaller = new MarshallerService(
				Arrays.asList(new ScanningAPIClassRegistry(),
						new ScanningExampleClassRegistry(),
						new ScanningTestClassRegistry()),
				Arrays.asList(new PointsModelMarshaller())
				);
		ActivemqConnectorService.setJsonMarshaller(marshaller);
		eservice  = new EventServiceImpl(new ActivemqConnectorService());
	}
	
	@Before
	public void setUp() throws EventException {
		queueControl = eservice.createRemoteService(uri, IQueueControllerService.class);
		queueControl.startQueueService();
		
		jqID = queueControl.getJobQueueID();
		IQueue<? extends StatusBean> queue = queueControl.getQueue(jqID);
		String topicName = queue.getStatusTopicName();
		statusSubsc = eservice.createSubscriber(uri, topicName);
	}
	
	@After
	public void tearDown() throws EventException {
		queueControl.stopQueueService(true);
		
		statusSubsc.disconnect();
	}
	
	@Test
	public void testPositioner() throws Exception {
		PositionerAtom posAt = new PositionerAtom("setT", "T", 290);//Starts at 295 with rate of change 1/s
		SubTaskAtom stAt = new SubTaskAtom(null, "testSubTask");
		TaskBean tBean = new TaskBean(null, "testTask");
		stAt.addAtom(posAt);
		tBean.addAtom(stAt);

		//Create our subscriber and add the listener
		final CountDownLatch statusLatch = new CountDownLatch(1);
		statusSubsc.addListener(new IBeanListener<StatusBean>() {

			@Override
			public void beanChangePerformed(BeanEvent<StatusBean> evt) {
				StatusBean evtBean = evt.getBean();
				if (evtBean.getUniqueId().equals(tBean.getUniqueId())) {
					if (evtBean.getStatus().isFinal()) {
						statusLatch.countDown();
					}
				}
			}
		});

		queueControl.submit(tBean, jqID);

		statusLatch.await();
		System.out.println("RELEASED");
//		if (released) {
//			System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~\n Final state reached\n~~~~~~~~~~~~~~~~~~~~~~~~~");
//		} else {
//			System.out.println("#########################\n No final state reported\n#########################");
//		}
	}

}
