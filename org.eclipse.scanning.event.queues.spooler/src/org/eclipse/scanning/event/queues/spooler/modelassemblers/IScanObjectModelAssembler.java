package org.eclipse.scanning.event.queues.spooler.modelassemblers;

import java.util.List;

import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;

public interface IScanObjectModelAssembler<P> {

	P assemble(String name, DeviceModel model) throws QueueModelException;

	List<String> getRequiredArgReferences();

	String getString(Object model);
}
