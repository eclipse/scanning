package org.eclipse.scanning.test.event.queues;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.queues.IQueueSpoolerService;
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
	public void testSubmitExperiments() throws QueueModelException {
		IQueueSpoolerService qss = new QueueSpoolerService();
		
		try {
			List<Long> wrongIds = Arrays.asList(2L, 6L, 6443L);
			qss.submitExperiments(wrongIds);
			fail("Should not be able to submit IDs that are known to the sample information service");
		} catch (QueueModelException qme) {
			//Expected. We don't allow wrong IDs to be submitted
		}
		
		List<Long> idsToSubmit = Arrays.asList(3245L, 95222L, 321345L);
		qss.submitExperiments(idsToSubmit);
		
	}

}
