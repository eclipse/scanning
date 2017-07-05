package org.eclipse.scanning.api.event.queues;

import java.util.List;

import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public interface IQueueSpoolerService {
	
	/**
	 * 
	 * @param sampleIds List<Long> of sampleIds
	 * @throws QueueModelException if one or more sampleIds could not be submitted
	 */
	void submitExperiments(List<Long> sampleIds) throws QueueModelException;
	
	/**
	 * 
	 * @return
	 */
	IQueueBeanFactory getQueueBeanFactory();

}
