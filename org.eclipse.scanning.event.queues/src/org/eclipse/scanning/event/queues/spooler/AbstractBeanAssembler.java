package org.eclipse.scanning.event.queues.spooler;

import java.util.List;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public abstract class AbstractBeanAssembler<Q extends Queueable> implements IBeanAssembler<Q> {
	
	private final IQueueBeanFactory queueBeanFactory;
	protected List<IQueueValue<?>> localValues;
	
	protected AbstractBeanAssembler(IQueueBeanFactory queueBeanFactory) {
		this.queueBeanFactory = queueBeanFactory;
	}

	@Override
	public IQueueBeanFactory getQueueBeanFactory() {
		return queueBeanFactory;
	}

	@Override
	public void setLocalValues(List<IQueueValue<?>> localValues) {
		this.localValues = localValues;
	}
	
	@Override
	public IQueueValue<?> getQueueValue(QueueValue<String> valueReference) throws QueueModelException {
		/*
		 * Identify whether a value corresponding to valueReference is in the 
		 * localValues
		 */
		int valueIndex = -1;
		for (IQueueValue<?> val : localValues) {
			if (valueReference.isReference(val)) {
				valueIndex = localValues.indexOf(val);
			}
		}
		
		//Get the value from somewhere and return it
		if (valueIndex == -1) {
			return queueBeanFactory.getGlobalValue(valueReference);
		} else {
			return localValues.get(valueIndex);
		}
	}

}
