package org.eclipse.scanning.sequencer.analysis;

import java.util.Arrays;

import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.ModelValidationException;
import org.eclipse.scanning.api.ValidationException;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.DeviceRole;
import org.eclipse.scanning.api.device.models.SlicingModel;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.sequencer.ServiceHolder;

/**
 *
 * This device is one that slices the value of another dataset from the scan file.
 * This is useful because often inline processing, such as an average may need to be done.
 * If that is needed it is possible to extend SlicingRunnableDevice to implement the
 * process method.
 *
 * @author Matthew Gerring
 * @param <T>
 *
 */
public abstract class SlicingRunnableDevice<T extends SlicingModel> extends AbstractRunnableDevice<T> implements IWritableDetector<T>{



	public SlicingRunnableDevice() {
		super(ServiceHolder.getRunnableDeviceService());
		setLevel(100); // Runs at the end of the cycle by default.
		setRole(DeviceRole.PROCESSING);
	}

	@Override
	public void validate(T model) throws ValidationException {
		if (model.getDataFile()==null) throw new ModelValidationException("The data file must be set!", model, "dataFile");
		if (model.getDetectorName()==null) throw new ModelValidationException("The detector name must be set!", model, "detectorName");
		if (model.getTimeout()<=0) throw new ModelValidationException("The timeout must be greater than 0", model, "timeout");
	}


	@Override
	public void run(IPosition position) throws ScanningException, InterruptedException {

	}

	@Override
	public boolean write(IPosition loc) throws ScanningException {

		try {
			// Get the dataset we are slicing
			ILoaderService lservice = ServiceHolder.getLoaderService();
			IDataHolder    holder   = lservice.getData(model.getDataFile(), new IMonitor.Stub());
			ILazyDataset data = holder.getLazyDataset("/entry/instrument/"+model.getDetectorName()+"/data");

			int[] dshape = getDataShape(data);
			IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(loc, dshape);
			SliceND sliceData = new SliceND(data.getShape(), rslice.getStart(), rslice.getStop(), rslice.getStep());
			IDataset slice = data.getSlice(sliceData);

			return process(new SliceDeviceContext(loc, rslice, data, slice));

		} catch (ScanningException se) {
			throw se;
		} catch (Exception other) {
			throw new ScanningException(other);
		}
	}

	/**
	 * This method is called with each slice of scan data read
	 * @param loc
	 * @param rslice
	 * @param slice
	 * @return
	 * @throws ScanningException
	 */
	abstract boolean process(SliceDeviceContext context) throws ScanningException;


	protected int[] getDataShape(ILazyDataset data) {

        int dataRank = getModel().getDataRank();
		int[] dshape = new int[dataRank];
		for (int i = dataRank; i > 0; i--) {
			dshape[dataRank-i] = data.getShape()[data.getShape().length-i];
		}
		return dshape;
	}

}
