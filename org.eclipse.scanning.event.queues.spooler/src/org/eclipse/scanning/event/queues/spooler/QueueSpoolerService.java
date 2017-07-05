package org.eclipse.scanning.event.queues.spooler;

import java.util.List;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.IQueueSpoolerService;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public class QueueSpoolerService implements IQueueSpoolerService {

	@Override
	public void submitExperiments(List<Long> sampleIds) throws QueueModelException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IQueueBeanFactory getQueueBeanFactory() {
		// TODO Auto-generated method stub
		return null;
	}

}
