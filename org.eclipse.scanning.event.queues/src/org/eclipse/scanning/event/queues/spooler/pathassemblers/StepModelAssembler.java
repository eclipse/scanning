package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.points.models.StepModel;

public class StepModelAssembler extends AbstractPathModelAssembler<StepModel> {
	
	private Double start, stop, step;
	
	public StepModelAssembler() {
		super(new CommonArgNames[]{CommonArgNames.START, CommonArgNames.STOP, CommonArgNames.STEP});
	}

	@Override
	public StepModel assemble(String name, DeviceModel model) throws QueueModelException {
		start = (Double) model.getDeviceModelValue(required[0].getValue()).evaluate();
		stop = (Double) model.getDeviceModelValue(required[1].getValue()).evaluate();
		step = (Double) model.getDeviceModelValue(required[2].getValue()).evaluate();
		
		return new StepModel(name, start, stop, step);
	}

	

}
