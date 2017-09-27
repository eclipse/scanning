package org.eclipse.scanning.event.queues.spooler;

import java.util.List;

import org.eclipse.scanning.api.database.ISampleDescriptionService;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.IQueueSpoolerService;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.scan.models.ScanMetadata;

public class QueueSpoolerService implements IQueueSpoolerService {

	@Override
	public boolean isExperimentReady(String proposalCode, long proposalNumber, long sampleId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TaskBean createBeansForExperiment(ExperimentConfiguration config, List<ScanMetadata> metadata)
			throws QueueModelException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void doSubmission(TaskBean bean) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IQueueBeanFactory getQueueBeanFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISampleDescriptionService getSampleService() {
		// TODO Auto-generated method stub
		return null;
	}

}
