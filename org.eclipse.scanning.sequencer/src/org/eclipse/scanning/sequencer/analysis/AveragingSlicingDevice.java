package org.eclipse.scanning.sequencer.analysis;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.device.models.SlicingModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
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
public class AveragingSlicingDevice extends SlicingRunnableDevice<SlicingModel> implements INexusDevice<NXdetector> {

	public static final String AVERAGE_QUALIFIER = "_average";
	
	private ILazyWriteableDataset averaged;
	private NexusScanInfo         info;

	@Override
	public boolean process(SliceDeviceContext context) throws ScanningException {
		
		averaged.setChunking(info.createChunk(getDataShape(context.getData())));
		 		
		double mean = (Double)context.getSlice().squeeze().mean();

		IScanSlice sslice  = IScanRankService.getScanRankService().createScanSlice(context.getLocation());
		SliceND    slicenD = new SliceND(averaged.getShape(), averaged.getMaxShape(), sslice.getStart(), sslice.getStop(), sslice.getStep());
		try {
			averaged.setSlice(null, DatasetFactory.createFromObject(mean), slicenD);
		} catch (DatasetException e) {
			throw new ScanningException(e);
		}
	
		return true;
	}
	
	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		
		final NXdetector detector = NexusNodeFactory.createNXdetector();
		
		this.averaged = detector.initializeLazyDataset(NXdetector.NX_DATA, info.getRank(), Double.class);
		this.info     = info;		
		
		Attributes.registerAttributes(detector, this);

		NexusObjectWrapper<NXdetector> nexusProvider = new NexusObjectWrapper<NXdetector>(getModel().getDetectorName()+AVERAGE_QUALIFIER, detector);

		// Add all fields for any NXdata groups that this device creates
		nexusProvider.setAxisDataFieldNames(NXdetector.NX_DATA);
		
		// "data" is the name of the primary data field (i.e. the 'signal' field of the default NXdata)
		nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);

		return nexusProvider;
	}
}
