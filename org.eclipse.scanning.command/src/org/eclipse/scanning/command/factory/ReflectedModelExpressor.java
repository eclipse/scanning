package org.eclipse.scanning.command.factory;

import org.eclipse.scanning.api.device.models.IReflectedModel;

public class ReflectedModelExpressor extends PyModelExpresser<IReflectedModel> {

	String pyExpress(IReflectedModel model, boolean verbose) throws Exception {
		return model.getCommandString(verbose);
	}
}
