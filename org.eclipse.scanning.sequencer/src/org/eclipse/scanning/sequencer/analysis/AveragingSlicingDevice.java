package org.eclipse.scanning.sequencer.analysis;

import java.util.Arrays;

import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.scanning.api.device.models.SlicingModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanSlice;

/**
 * <pre>
 * A device which reads back slices as they are written and writes their
 * average to a dataset. The average dataset is currently scanrank+1. Whatever
 * the data rank, each slice is averaged and written into the dataset
 * called "{@literal average_$detectorName$}"
 * 
 * NOTE This device assumes that each dataset written during the scan will be
 * available for processing. If the NeXus HDF-SWMR API does not flush data
 * during the scan, it cannot work because it reads back the data written
 * by a previous detector and averages it.
 * 
 * </pre>
 * 
 * @author Matthew Gerring
 *
 */
public class AveragingSlicingDevice extends SlicingRunnableDevice<SlicingModel> {

	@Override
	public boolean process(IPosition loc, IScanSlice rslice, ILazyDataset data, IDataset slice) throws ScanningException {
		
		System.out.println(rslice);
		System.out.println(Arrays.toString(slice.getShape()));
		System.out.println(slice.mean());
		
		return true;
	}

}
