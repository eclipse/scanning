package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public final class SubTaskAtomAssembler extends AbstractBeanAssembler<SubTaskAtom> {
	
	private AtomQueueBuilder<SubTaskAtom, QueueAtom> atomQueueBuider;
	
	public SubTaskAtomAssembler(IQueueBeanFactory queueBeanFactory) {
		super(queueBeanFactory);
		atomQueueBuider = new AtomQueueBuilder<>(queueBeanFactory, SubTaskAtom.class);
	}

	@Override
	public SubTaskAtom buildNewBean(SubTaskAtom model) throws QueueModelException {
		SubTaskAtom atom = new SubTaskAtom(model.getShortName(), model.getName());
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		atom.setModel(false);
		
		return atomQueueBuider.populateAtomQueue(model, atom, localValues);
	}

	@Override
	public SubTaskAtom setBeanName(SubTaskAtom bean) {
		// TODO Auto-generated method stub
		return null;
	}

}
