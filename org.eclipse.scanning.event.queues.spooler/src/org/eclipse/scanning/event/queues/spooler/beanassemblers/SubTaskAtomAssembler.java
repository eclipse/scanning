package org.eclipse.scanning.event.queues.spooler.beanassemblers;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public final class SubTaskAtomAssembler extends AbstractBeanAssembler<SubTaskAtom> {

	private AtomQueueBuilder<SubTaskAtom, QueueAtom> atomQueueBuider;
	private ExperimentConfiguration config;

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

		SubTaskAtom subTask = atomQueueBuider.populateAtomQueue(model, atom, config);
		config = null;
		return subTask;
	}

	@Override
	public void setBeanName(SubTaskAtom bean) {
		//Name should already be set on the SubTaskAtom
	}

	@Override
	public void updateBeanModel(SubTaskAtom model, ExperimentConfiguration config) throws QueueModelException {
		this.config = config;
	}

}
