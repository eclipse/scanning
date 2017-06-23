package org.eclipse.scanning.event.queues.spooler;

import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.QueueAtom;
import org.eclipse.scanning.api.event.queues.beans.SubTaskAtom;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public final class SubTaskAtomAssembler implements IBeanAssembler<SubTaskAtom> {

	@Override
	public SubTaskAtom buildNewBean(SubTaskAtom model, Map<QueueValue<String>, IQueueValue<?>> localValues)
			throws QueueModelException {
		SubTaskAtom atom = new SubTaskAtom(model.getShortName(), model.getName());
		atom.setBeamline(model.getBeamline());
		atom.setRunTime(model.getRunTime());
		atom.setModel(false);
		
		AtomQueueBuilder<SubTaskAtom, QueueAtom> aqb = new AtomQueueBuilder<>(model, atom, localValues, getQueueBeanFactory());
		return aqb.populateAtomQueue(SubTaskAtom.class);
	}

	@Override
	public SubTaskAtom setBeanName(SubTaskAtom bean) {
		// TODO Auto-generated method stub
		return null;
	}

}
