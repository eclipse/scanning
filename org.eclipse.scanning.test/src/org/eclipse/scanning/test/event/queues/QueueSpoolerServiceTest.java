package org.eclipse.scanning.test.event.queues;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.IQueueSpoolerService;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.event.queues.spooler.QueueSpoolerService;
import org.eclipse.scanning.test.event.queues.mocks.MockSampleDescriptionService;
import org.junit.Before;
import org.junit.Test;

public class QueueSpoolerServiceTest {
	
	@Before
	public void setUp() {
		ISampleDescriptionService samServ = new MockSampleDescriptionService();
		((MockSampleDescriptionService)samServ).setSampleIdNames(makeFakeExperimentIdNames());
	}
	
	private Map<Long, String> makeFakeExperimentIdNames() {
		Map<Long, String> fakeExperIdNames = new HashMap<>();
		fakeExperIdNames.put(3245L, "XPDF Test1");
		fakeExperIdNames.put(5324L, "XPDF Test2");
		fakeExperIdNames.put(321345L, "XPDF Test5");
		fakeExperIdNames.put(95222L, "XPDF testgg");
		return fakeExperIdNames;
	}
	
	@Test
	public void testIsExperimentReady() {
		IQueueSpoolerService qss = new QueueSpoolerService();
		assertTrue("ID is not ready, even though it is in the list", qss.isExperimentReady("CM", 14451L, 3245L));
		assertFalse("ID is ready, even though it doesn't exist", qss.isExperimentReady("CM", 14451L, 5L));
	}
	
	@Test
	public void testCreateBeans() throws QueueModelException {
		IQueueSpoolerService qss = new QueueSpoolerService();
		//FIXME Shouldn't the name of the task bean be the name of the experiment???
		//TODO add scanmetadata
		TaskBean tb = new TaskBean("testTb", "EXPNAME!", true);
		qss.getQueueBeanFactory().registerTask(tb);
		
		TaskBean expectedTB = expectedTaskBean();
		TaskBean madeTB = qss.createBeansForExperiment(null, null); //TODO replace null with metadatas!
		if (!expectedTB.equals(madeTB)) {
			//TODO How do they differ?
		}
	}
	
	@Test
	public void testSubmission() throws EventException {
		IQueueSpoolerService qss = new QueueSpoolerService();
		
		//TODO Set MockIQueueControllerService in ServicesHolder
		TaskBean submitMe = expectedTaskBean();
		qss.doSubmission(submitMe);
		
		//TODO Get submitted bean from MockQueueControllerService & compare to submitMe
//		assertEquals("Submitted and original TaskBeans differ", submitMe, mockQCont.getSubmitted());
	}
	
	private TaskBean expectedTaskBean() {
		TaskBean expectedTB = new TaskBean("testTb", "Experiment test");
		//TODO add configured beans!
		
		return expectedTB;
	}

}
