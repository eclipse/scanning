package org.eclipse.scanning.event.queues.spooler.pathassemblers;

import org.eclipse.scanning.api.event.queues.models.DeviceModel;
import org.eclipse.scanning.api.event.queues.models.QueueModelException;
import org.eclipse.scanning.api.points.models.ArrayModel;

public class ArrayModelAssembler extends AbstractPathModelAssembler<ArrayModel> {
	
	private double[] positions;

	public ArrayModelAssembler() {
		super(new CommonArgNames[]{CommonArgNames.POSITIONS});
	}

	@Override
	public ArrayModel assemble(String name, DeviceModel model) throws QueueModelException {
		Double[] storedPositions = (Double[]) model.getDeviceModelValue(required[0].getValue()).evaluate();
		positions = new double[storedPositions.length];
		for (int i = 0; i < positions.length; i++) {
			positions[i] = storedPositions[i];
		}
		
		ArrayModel arrayModel = new ArrayModel(positions);
		arrayModel.setName(name);
		return arrayModel;
	}



}
