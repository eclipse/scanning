package org.eclipse.scanning.event.queues.spooler;

import java.util.Map;

import org.eclipse.scanning.api.event.queues.beans.ScanAtom;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;

public final class ScanAtomAssembler implements IBeanAssembler<ScanAtom> {

	@Override
	public ScanAtom buildNewBean(ScanAtom model, Map<QueueValue<String>, IQueueValue<?>> localValues)
			throws QueueModelException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ScanAtom setBeanName(ScanAtom bean) {
		// TODO Auto-generated method stub
		return null;
	}

}
