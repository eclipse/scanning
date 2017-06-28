package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import java.util.List;

import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;

public interface IScanPathModelAssembler<P> {
	
	P assemble(String name, DeviceModel model) throws QueueModelException;
	
	List<IQueueValue<String>> getRequiredArgReferences();
	
}
