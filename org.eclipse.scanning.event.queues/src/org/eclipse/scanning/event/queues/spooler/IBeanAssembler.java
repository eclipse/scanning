package org.eclipse.scanning.event.queues.spooler;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.Queueable;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.event.queues.ServicesHolder;

public interface IBeanAssembler<Q extends Queueable> {
	
	default Q assemble(Q model, Map<QueueValue<String>, IQueueValue<?>> localValues) throws QueueModelException {
		Q bean;
		if (localValues == null) {
			//Protecting against NPEs
			localValues = new HashMap<>();
		}
		
		if (model.isModel()) {
			bean = buildNewBean(model, localValues);
		} else {
			bean = model;
		}
		setBeanName(bean);
		return bean;
	}
	
	Q buildNewBean(Q model, Map<QueueValue<String>, IQueueValue<?>> localValues) throws QueueModelException;
	
	Q setBeanName(Q bean);
	
	default IQueueBeanFactory getQueueBeanFactory() {
		ServicesHolder.getIQueueSpoolerService().getQueueBeanFactory();
	}

}
