package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import java.util.List;

import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public interface IScanPathModelAssembler<P> {
	
	P assemble(String name, DeviceModel model) throws QueueModelException;
	
	List<String> getRequiredArgReferences();
	
}
