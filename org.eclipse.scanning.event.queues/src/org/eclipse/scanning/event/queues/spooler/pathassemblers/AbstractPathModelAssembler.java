package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.points.models.IScanPathModel;

public abstract class AbstractPathModelAssembler<P extends IScanPathModel> implements IScanPathModelAssembler<P> {

	protected final String[] required;
		
	protected AbstractPathModelAssembler(String[] required) {
		this.required = required;
	}
	
	@Override
	public List<String> getRequiredArgReferences() {
		return Arrays.asList(required);
	}

}
