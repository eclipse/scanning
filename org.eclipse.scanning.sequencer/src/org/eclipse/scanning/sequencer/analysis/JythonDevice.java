package org.eclipse.scanning.sequencer.analysis;

import org.eclipse.january.dataset.IDataset;
import org.eclipse.scanning.api.device.models.JythonModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.jython.JythonObjectFactory;

public class JythonDevice extends SlicingRunnableDevice<JythonModel> {
	
	private JythonObjectFactory<IJythonFunction> factory;

	@Override
	public void configure(JythonModel model) throws ScanningException {
		super.configure(model);
		this.factory = new JythonObjectFactory<>(IJythonFunction.class, model.getModuleName(), model.getClassName(), "org.eclipse.scanning.sequencer");
	}

	@Override
	boolean process(SliceDeviceContext context) throws ScanningException {
		
		IJythonFunction jython = factory.createObject();
		IDataset ret = jython.function(context.getSlice());
		
		System.out.println(ret);
		
		return true;
	}

}
