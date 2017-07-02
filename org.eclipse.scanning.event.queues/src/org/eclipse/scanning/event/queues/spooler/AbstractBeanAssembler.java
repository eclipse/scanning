package org.eclipse.scanning.event.queues.spooler;

import java.util.Map;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public abstract class AbstractBeanAssembler<Q extends Queueable> implements IBeanAssembler<Q> {
	
	private final IQueueBeanFactory queueBeanFactory;
	
	protected AbstractBeanAssembler(IQueueBeanFactory queueBeanFactory) {
		this.queueBeanFactory = queueBeanFactory;
	}

	@Override
	public IQueueBeanFactory getQueueBeanFactory() {
		return queueBeanFactory;
	}

	@Override
	public IQueueValue<?> getRealValue(QueueValue<String> valueReference, ExperimentConfiguration config) throws QueueModelException {
		try {
			return config.getLocalValue((QueueValue<String>) valueReference);
		} catch (QueueModelException qmEx) {
			return queueBeanFactory.getGlobalValue((QueueValue<String>) valueReference);
		}
	}
	
	protected void replaceMapIQueueValues(Map<String, Object> map, ExperimentConfiguration config) {
		map.entrySet().stream().filter(option -> (option.getValue() instanceof IQueueValue))
		.forEach(option -> map.put(option.getKey(), setValue((IQueueValue<?>) option.getValue(), config)));
	}
	
	@SuppressWarnings("unchecked")//We check value is safe to cast before getting near the cast - this is fine 
	protected Object setValue(IQueueValue<?> queueValue, ExperimentConfiguration config) {
		if (queueValue.isReference() && queueValue instanceof QueueValue && queueValue.getValueType().equals(String.class)) {
			try {
				String argName = queueValue.getName();
				queueValue = getRealValue((QueueValue<String>)queueValue, config);
				queueValue.setName(argName);
			} catch (QueueModelException qmEx) {
				throw new ModelEvaluationException(qmEx);
			}
		}//TODO Add checking of arg type recursively here.
		return queueValue.evaluate();
	}

}
