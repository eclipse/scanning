package org.eclipse.scanning.event.queues.spooler.beanassemblers;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public final class MonitorAtomAssembler extends AbstractBeanAssembler<MonitorAtom> {

	public MonitorAtomAssembler(IQueueBeanFactory queueBeanFactory) {
		super(queueBeanFactory);
	}

	@Override
	public MonitorAtom buildNewBean(MonitorAtom model) throws QueueModelException {
		MonitorAtom atom = new MonitorAtom(model.getShortName(), false, model.getMonitor());
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());

		return atom;
	}


	@Override
	public void setBeanName(MonitorAtom bean) {
		bean.setName("Measure current value of '"+bean.getMonitor()+"'");
	}

	@Override
	public void updateBeanModel(MonitorAtom model, ExperimentConfiguration config) throws QueueModelException {
		//No values to update here
	}




}
