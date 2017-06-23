package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.MonitorAtom;
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
	public MonitorAtom setBeanName(MonitorAtom bean) {
		// TODO Auto-generated method stub
		return null;
	}




}
