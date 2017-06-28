package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.scanning.api.event.queues.models.arguments.IQueueValue;
import org.eclipse.scanning.api.event.queues.models.arguments.QueueValue;
import org.eclipse.scanning.api.points.models.IScanPathModel;

public abstract class AbstractPathModelAssembler<P extends IScanPathModel> implements IScanPathModelAssembler<P> {

	protected final CommonArgNames[] required;
		
	protected AbstractPathModelAssembler(CommonArgNames[] required) {
		this.required = required;
	}
	
	@Override
	public List<IQueueValue<String>> getRequiredArgReferences() {
		return Arrays.stream(required).map(arg -> arg.getValue()).collect(Collectors.toList());
	}
	
	protected enum CommonArgNames {
		START(new QueueValue<>("start", true)), 
		STOP(new QueueValue<>("stop", true)), 
		STEP(new QueueValue<>("step", true)),
		POSITIONS(new QueueValue<>("positions", true));
		
		private final IQueueValue<String> value;
		
		CommonArgNames(IQueueValue<String> value) { this.value = value; }
		
		IQueueValue<String> getValue() { return value; }
	}

}
