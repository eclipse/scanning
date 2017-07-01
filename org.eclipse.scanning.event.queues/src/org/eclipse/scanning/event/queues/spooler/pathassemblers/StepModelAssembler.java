package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.points.models.StepModel;

public class StepModelAssembler extends AbstractPathModelAssembler<StepModel> {
	
	private Double start, stop, step;
	
	public StepModelAssembler() {
		super(new String[]{"start", "stop", "step"});
	}

	@Override
	public StepModel assemble(String name, DeviceModel model) throws QueueModelException {
		start = (Double) model.getDeviceModelValue(required[0]);
		stop = (Double) model.getDeviceModelValue(required[1]);
		step = (Double) model.getDeviceModelValue(required[2]);
		
		return new StepModel(name, start, stop, step);
	}

	

}
