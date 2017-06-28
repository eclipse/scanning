package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public abstract class AbstractBeanAssembler<Q extends Queueable> implements IBeanAssembler<Q> {
	
	private final IQueueBeanFactory queueBeanFactory;
	protected ExperimentConfiguration config;
	
	protected AbstractBeanAssembler(IQueueBeanFactory queueBeanFactory) {
		this.queueBeanFactory = queueBeanFactory;
	}

	@Override
	public IQueueBeanFactory getQueueBeanFactory() {
		return queueBeanFactory;
	}

	@Override
	public void setExperimentConfiguration(ExperimentConfiguration config) {
		this.config = config;
	}
	
	@Override
	public IQueueValue<?> getLocalValue(QueueValue<String> valueReference) throws QueueModelException {
		try {
			return config.getLocalValue(valueReference);
		} catch (QueueModelException qmEx) {
			return queueBeanFactory.getGlobalValue(valueReference);
		}
	}

}
