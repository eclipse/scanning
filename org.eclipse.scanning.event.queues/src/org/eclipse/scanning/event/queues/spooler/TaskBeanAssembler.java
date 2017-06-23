package org.eclipse.scanning.event.queues.spooler;

import org.eclipse.scanning.api.event.queues.IQueueBeanFactory;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public final class TaskBeanAssembler extends AbstractBeanAssembler<TaskBean> {
	
	private AtomQueueBuilder<TaskBean, SubTaskAtom> atomQueueBuider;
	
	public TaskBeanAssembler(IQueueBeanFactory queueBeanFactory) {
		super(queueBeanFactory);
		atomQueueBuider = new AtomQueueBuilder<>(queueBeanFactory, TaskBean.class);
	}

	@Override
	public TaskBean buildNewBean(TaskBean model)
			throws QueueModelException {
		TaskBean atom = new TaskBean(model.getShortName(), model.getName());
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		atom.setModel(false);
		
		return atomQueueBuider.populateAtomQueue(model, atom, localValues);
	}

	@Override
	public TaskBean setBeanName(TaskBean bean) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
