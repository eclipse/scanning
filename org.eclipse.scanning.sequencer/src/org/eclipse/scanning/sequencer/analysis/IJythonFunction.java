package org.eclipse.scanning.sequencer.analysis;

import org.eclipse.january.dataset.IDataset;

public interface IJythonFunction {

	/**
	 * This interface 
	 * @param slice
	 * @return
	 */
	public IDataset process(IDataset slice);
}
