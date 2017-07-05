package org.eclipse.scanning.event.queues.spooler.beanassemblers;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.ExperimentConfiguration;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public final class TaskBeanAssembler extends AbstractBeanAssembler<TaskBean> {
	
	private AtomQueueBuilder<TaskBean, SubTaskAtom> atomQueueBuider;
	private ExperimentConfiguration config;
	
	public TaskBeanAssembler(IQueueBeanFactory queueBeanFactory) {
		super(queueBeanFactory);
		atomQueueBuider = new AtomQueueBuilder<>(queueBeanFactory, TaskBean.class);
	}

	@Override
	public TaskBean buildNewBean(TaskBean model) throws QueueModelException {
		TaskBean atom = new TaskBean(model.getShortName(), model.getName());
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		atom.setModel(false);
		
		TaskBean task = atomQueueBuider.populateAtomQueue(model, atom, config);
		config = null;
		return task;
	}

	@Override
	public void setBeanName(TaskBean bean) {
		//Name should already be set on the TaskBean
	}

	@Override
	public void updateBeanModel(TaskBean model, ExperimentConfiguration config) throws QueueModelException {
		this.config = config;
	}

	
}
