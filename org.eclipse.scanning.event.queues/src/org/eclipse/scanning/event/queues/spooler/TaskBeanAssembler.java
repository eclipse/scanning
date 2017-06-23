package org.eclipse.scanning.event.queues.spooler;

import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.beans.TaskBean;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public final class TaskBeanAssembler implements IBeanAssembler<TaskBean> {

	@Override
	public TaskBean buildNewBean(TaskBean model, Map<QueueValue<String>, IQueueValue<?>> localValues)
			throws QueueModelException {
		TaskBean atom = new TaskBean(model.getShortName(), model.getName());
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		atom.setModel(false);
		
		AtomQueueBuilder<TaskBean, SubTaskAtom> aqb = new AtomQueueBuilder<>(model, atom, localValues, getQueueBeanFactory());
		return aqb.populateAtomQueue(TaskBean.class);
	}

	@Override
	public TaskBean setBeanName(TaskBean bean) {
		// TODO Auto-generated method stub
		return null;
	}

}
