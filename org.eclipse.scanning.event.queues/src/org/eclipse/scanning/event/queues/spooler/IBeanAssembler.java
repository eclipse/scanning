package org.eclipse.scanning.event.queues.spooler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.models.ModelEvaluationException;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public interface IBeanAssembler<Q extends Queueable> {
	
	default Q assemble(Q model, List<IQueueValue<?>> localValues) throws QueueModelException {
		Q bean;
		if (localValues == null) {
			//Protecting against NPEs
			localValues = new ArrayList<>();
		}
		setLocalValues(localValues);
		
		if (model.isModel()) {
			bean = buildNewBean(model);
		} else {
			bean = model;
		}
		setBeanName(bean);
		setLocalValues(null);
		return bean;
	}
	
	Q buildNewBean(Q model) throws QueueModelException;
	
	Q setBeanName(Q bean);
	
	IQueueBeanFactory getQueueBeanFactory();
	
	/**
	 * Updates the current {@link IQueueValue} representing a value with the 
	 * {@link IQueueValue} stored in local/global values, iff the given 
	 * {@link IQueueValue} is a QueueValue instance and it is marked as a 
	 * variable. Otherwise returns the {@link IQueueValue} given as the 
	 * argument (since it doesn't need to be updated).
	 * @param valueReference {@link IQueueValue to be replaced
	 * @return {@link IQueueValue} to replace argument
	 */
	@SuppressWarnings("unchecked") //Real references should have string type arguments - this is safe
	public default IQueueValue<?> updateIQueueValue(IQueueValue<?> valueReference) {
		if (valueReference instanceof QueueValue && valueReference.isVariable()) {
			try {
				return getQueueValue((QueueValue<String>) valueReference);
			} catch (QueueModelException qmEx) {
				throw new ModelEvaluationException(qmEx);
			}
		}
		return valueReference;
	}
	
	IQueueValue<?> getQueueValue(QueueValue<String> valueReference) throws QueueModelException;
	
	void setLocalValues(List<IQueueValue<?>> localValues);

}
