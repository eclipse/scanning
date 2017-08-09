package org.eclipse.scanning.example.detector;

import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.PROPERTY_NAME_UNIQUE_KEYS_PATH;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.ServiceHolder;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.Random;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.scanning.api.annotation.scan.ScanFinally;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.rank.IScanRankService;
import org.eclipse.scanning.api.scan.rank.IScanSlice;
import org.eclipse.scanning.example.Services;

public class PosDetector extends AbstractRunnableDevice<PosDetectorModel> implements IWritableDetector<PosDetectorModel>, INexusDevice<NXdetector> {

	private IDataset image;
	private ILazyWriteableDataset data;
	private ILazyWriteableDataset uniqueKeys;
	private int scanRank = -1;
	private String filePath;
	private NexusFile nexusFile;
	
	public PosDetector() throws ScanningException {
		super(Services.getRunnableDeviceService());
		this.model = new PosDetectorModel();
		setDeviceState(DeviceState.READY);
	}
	
	@Override
	public void run(IPosition position)
			throws ScanningException, InterruptedException, TimeoutException, ExecutionException {
		image = Random.rand(64, 64);
	}

	@Override
	public boolean write(IPosition position) throws ScanningException, InterruptedException {
		if (image == null) {
			return false;
		}
		
		if (data == null) {
			createNexusFile();
		}
		
		try {
			IScanSlice rslice = IScanRankService.getScanRankService().createScanSlice(position, 64, 64);
			SliceND sliceND = new SliceND(data.getShape(), data.getMaxShape(),
					rslice.getStart(), rslice.getStop(), rslice.getStep());
			data.setSlice(null, image, sliceND);
			
			// write unique key
			final int uniqueKey = position.getStepIndex() + 1;
			final Dataset uniqueKeyDataset = DatasetFactory.createFromObject(uniqueKey);
			rslice = IScanRankService.getScanRankService().createScanSlice(position);
			sliceND = new SliceND(uniqueKeys.getShape(), uniqueKeys.getMaxShape(),
					rslice.getStart(), rslice.getStop(), rslice.getStep());
			uniqueKeys.setSlice(null, uniqueKeyDataset, sliceND);
			
		} catch (DatasetException e) {
			setDeviceState(DeviceState.FAULT);
			throw new ScanningException("Failed to write the data to the NeXus file", e);
		}
		
		setDeviceState(DeviceState.ARMED);
		return true;
	}
	
	private void createNexusFile() throws ScanningException {
		TreeFile treeFile = NexusNodeFactory.createTreeFile(filePath);
		NXroot root = NexusNodeFactory.createNXroot();
		treeFile.setGroupNode(root);
		NXentry entry = NexusNodeFactory.createNXentry();
		root.setEntry(entry);
		
		NXcollection ndAttributesCollection = NexusNodeFactory.createNXcollection();
		entry.setCollection("NDAttributes", ndAttributesCollection);
		uniqueKeys = ndAttributesCollection.initializeLazyDataset("NDArrayUniqueId", scanRank, Integer.class);
		
		NXdata dataGroup = NexusNodeFactory.createNXdata();
		entry.setData(getName(), dataGroup);
		data = dataGroup.initializeLazyDataset(NXdata.NX_DATA, scanRank + 2, Double.class);
		
		INexusFileFactory nff = ServiceHolder.getNexusFileFactory();
		nexusFile = nff.newNexusFile(filePath, true);
		try {
			nexusFile.createAndOpenToWrite();
			nexusFile.addNode("/", treeFile.getGroupNode());
			nexusFile.activateSwmrMode();
			nexusFile.flush();
		} catch (NexusException e) {
			throw new ScanningException(e);
		}
	}
	
	@Override
	public NexusObjectProvider<NXdetector> getNexusProvider(NexusScanInfo info) throws NexusException {
		scanRank = info.getRank();
		filePath = getFilePath(info);
				
		NXdetector detector = NexusNodeFactory.createNXdetector();
		detector.setCount_timeScalar(model.getExposureTime());
		
		NexusObjectWrapper<NXdetector> nexusWrapper = 
				new NexusObjectWrapper<NXdetector>(getName(), detector, NXdetector.NX_DATA);
		nexusWrapper.addExternalLink(detector, NXdetector.NX_DATA, getFilePath(info),
				"/entry/" + getName() + "/" + NXdata.NX_DATA, info.getRank() + 2);
		nexusWrapper.setPropertyValue(PROPERTY_NAME_UNIQUE_KEYS_PATH,
				"/entry/NDAttributes/NDArrayUniqueId");
		
		return nexusWrapper;
	}
	
	private String getFilePath(NexusScanInfo info) {
		final File scanFile = new File(info.getFilePath());
		final File scanDir = scanFile.getParentFile();
		String scanFileNameNoExtn = scanFile.getName();
		final int dotIndex = scanFileNameNoExtn.indexOf('.');
		if (dotIndex != -1) {
			scanFileNameNoExtn = scanFileNameNoExtn.substring(0, dotIndex);
		}
		final File outputDir = new File(scanDir, scanFileNameNoExtn);
		outputDir.mkdir();
		
		final String filePath = new File(outputDir, "posdetector.h5").getAbsolutePath();
		return filePath;
	}
	
	@ScanFinally
	public void clean() {
		image = null;
		data = null;
		filePath = null;
		if (nexusFile != null) {
			try {
				nexusFile.close();
			} catch (NexusException e) {
				System.err.println("Could not close nexus file" + e);
			}
		}
		nexusFile = null;
	}

}
