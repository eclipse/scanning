package org.eclipse.scanning.sequencer.analysis;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.device.models.JythonModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.jython.JythonObjectFactory;

public class JythonDevice extends SlicingRunnableDevice<JythonModel>  implements INexusDevice<NXdetector> {

	private JythonObjectFactory<IJythonFunction> factory;
	private ILazyWriteableDataset processed;
	private NexusScanInfo info;

	@Override
	public void configure(JythonModel model) throws ScanningException {
		super.configure(model);
		this.factory = new JythonObjectFactory<>(IJythonFunction.class, model.getModuleName(), model.getClassName(), "org.eclipse.scanning.sequencer");
	}

	@Override
	boolean process(SliceDeviceContext context) throws ScanningException {

		processed.setChunking(info.createChunk(getDataShape(context.getData())));

		IJythonFunction jython = factory.createObject();
		IDataset ret = jython.process(context.getSlice());

		IScanSlice sslice  = IScanRankService.getScanRankService().createScanSlice(context.getLocation(), ret.getShape());
		SliceND    slicenD = new SliceND(processed.getShape(), processed.getMaxShape(), sslice.getStart(), sslice.getStop(), sslice.getStep());
		try {
			processed.setSlice(null, ret, slicenD);
		} catch (DatasetException e) {
			throw new ScanningException(e);
		}

		return true;
	}

	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {

		final NXdetector detector = NexusNodeFactory.createNXdetector();

		this.processed = detector.initializeLazyDataset(NXdetector.NX_DATA, info.getRank()+(getModel().getOutputRank()-1), Double.class);
		this.info      = info;

		Attributes.registerAttributes(detector, this);

		NexusObjectWrapper<NXdetector> nexusProvider = new NexusObjectWrapper<NXdetector>(getModel().getDetectorName()+"_"+getModel().getName(), detector);

		// Add all fields for any NXdata groups that this device creates
		nexusProvider.setAxisDataFieldNames(NXdetector.NX_DATA);

		// "data" is the name of the primary data field (i.e. the 'signal' field of the default NXdata)
		nexusProvider.setPrimaryDataFieldName(NXdetector.NX_DATA);

		return nexusProvider;
	}

}
