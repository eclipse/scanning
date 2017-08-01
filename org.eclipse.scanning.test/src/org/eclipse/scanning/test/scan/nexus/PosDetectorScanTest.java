package org.eclipse.scanning.test.scan.nexus;

import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_UNIQUE_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_SOLSTICE_SCAN;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertAxes;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertDataNodesEqual;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertIndices;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertScanNotFinished;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSignal;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.PositionEvent;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IPositionListenable;
import org.eclipse.scanning.api.scan.event.IPositionListener;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.PosDetectorModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PosDetectorScanTest extends NexusTest {
	
	private IWritableDetector<PosDetectorModel> detector;
	
	@Before
	public void before() throws Exception {
		PosDetectorModel model = new PosDetectorModel(3);
		detector = (IWritableDetector<PosDetectorModel>) dservice.createRunnableDevice(model);
		assertNotNull(detector);
	}
	
	@After
	public void after() throws Exception {
		File parentDir = output.getParentFile();
		String fileName = output.getName().substring(0, output.getName().indexOf('.'));
		File outputDir = new File(parentDir, fileName);
		for (File file : outputDir.listFiles()) {
			file.delete();
		}
		outputDir.delete();
	}
	
	@Test
	public void testPosScan() throws Exception {
		final int[] scanShape = new int[] { 8, 5 };
		IRunnableDevice<ScanModel> scanner = createGridScan(detector, output, false, scanShape);
		// add a the UniqueKeyChecker as a position listener to check the unique key is written at each point
		((IPositionListenable) scanner).addPositionListener(
				new UniqueKeyChecker(scanner.getModel().getFilePath(), scanShape));
		
		assertScanNotFinished(getNexusRoot(scanner).getEntry());
		
		scanner.run(null);
		
		checkNexusFile(scanner, scanShape);
	}
	
	private static class UniqueKeyChecker implements IPositionListener {
		
		private final int[] scanShape;
		private final String filePath;
		private ILazyDataset uniqueKeysDataset;
		
		UniqueKeyChecker(String filePath, final int[] scanShape) {
			this.filePath = filePath;
			this.scanShape = scanShape;
		}
		
		@Override
		public void positionMovePerformed(PositionEvent event) throws ScanningException {
			checkUniqueKeyWritten(event.getPosition());
		}

		@Override
		public void positionPerformed(PositionEvent event) throws ScanningException {
			checkUniqueKeyWritten(event.getPosition());
		}
		
		private IDataset getUniqueKeysDataset() throws ScanningException {
			NexusFile nf = null;
			try {
				ILazyDataset uniqueKeysDataset = null;
				if (uniqueKeysDataset == null) {
					nf = fileFactory.newNexusFile(filePath);
					nf.openToRead();

					TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
					NXroot root = (NXroot) nexusTree.getGroupNode();
					NXentry entry = root.getEntry();

					NXcollection solsticeScanCollection = entry.getCollection(GROUP_NAME_SOLSTICE_SCAN);
					NXcollection keysCollection = (NXcollection) solsticeScanCollection.getGroupNode(GROUP_NAME_KEYS);
					DataNode dataNode = keysCollection.getDataNode(FIELD_NAME_UNIQUE_KEYS);
					uniqueKeysDataset = dataNode.getDataset();
				}
				
//				((IDynamicDataset) uniqueKeysDataset).refreshShape();
				return uniqueKeysDataset.getSlice(); // it's only a small dataset, so this is ok
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				try {
					nf.close();
				} catch (NexusException e) {
					throw new ScanningException(e);
				}
			}
		}
		
		private void checkUniqueKeyWritten(IPosition position) throws ScanningException {
			IDataset uniqueKeysDataset = getUniqueKeysDataset();
			assertNotNull(uniqueKeysDataset);

			int uniqueKey = uniqueKeysDataset.getInt(position.getIndex(0), position.getIndex(1));
			assertEquals(position.getStepIndex() + 1, uniqueKey);
		}
		
	}
	
	private void checkNexusFile(IRunnableDevice<ScanModel> scanner, int[] sizes) throws Exception {
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();
		assertEquals(DeviceState.ARMED, scanner.getDeviceState());
		
		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		// check that the scan points have been written correctly
		assertSolsticeScanGroup(entry, false, false, 8, 5);
		
		String detectorName = detector.getName();
		NXdetector nxDetector = instrument.getDetector(detectorName);
		assertNotNull(nxDetector);
		assertEquals(((IDetectorModel) scanner.getModel().getDetectors().get(0).getModel()).getExposureTime(),
				nxDetector.getCount_timeScalar().doubleValue(), 1e-15);
		
		assertEquals(1, entry.getAllData().size());
		NXdata dataGroup = entry.getData(detectorName);
		assertNotNull(dataGroup);
		
		assertSignal(dataGroup, NXdetector.NX_DATA);
		
		DataNode dataNode = nxDetector.getDataNode(NXdetector.NX_DATA);
		assertNotNull(dataNode);
		assertDataNodesEqual("", dataNode, dataGroup.getDataNode(NXdetector.NX_DATA)); 

		IDataset dataset = dataNode.getDataset().getSlice();
		int[] shape = dataset.getShape();
		for (int i = 0; i < sizes.length; i++) {
			assertEquals(sizes[i], shape[i]);
		}
		
		// Make sure none of the numbers are NaNs. The detector
		// is expected to fill this scan with non-nulls
		final PositionIterator it = new PositionIterator(shape);
		while (it.hasNext()) {
			int[] next = it.getPos();
			assertFalse(Double.isNaN(dataset.getDouble(next)));
		}

		// Check axes
		final IPosition pos = scanModel.getPositionIterable().iterator().next();
		final List<String> scannableNames = pos.getNames();
		assertEquals(sizes.length, scannableNames.size());
		
		// Append _value_demand to each position name, append "." twice for 2 image dimensions
		List<String> expectedAxesNames = Stream.concat(scannableNames.stream().map(x -> x + "_value_set"),
				Collections.nCopies(2, ".").stream()).collect(Collectors.toList());
		assertAxes(dataGroup, expectedAxesNames.toArray(new String[expectedAxesNames.size()]));
		
		int[] defaultDimensionMappings = IntStream.range(0, scannableNames.size()).toArray();
		
		int i = 0;
		for (String positionerName : scannableNames) {
			NXpositioner positioner = entry.getInstrument().getPositioner(positionerName);
			
			// check value_demand data node
			String demandFieldName = positionerName + "_" + NXpositioner.NX_VALUE + "_set";
			assertSame(dataGroup.getDataNode(demandFieldName),
					positioner.getDataNode("value_set"));
			assertIndices(dataGroup, demandFieldName, i);
			NexusAssert.assertTarget(dataGroup, demandFieldName, rootNode,
					"/entry/instrument/" + positionerName + "/value_set");
			
			// check value data node
			String valueFieldName = positionerName + "_" + NXpositioner.NX_VALUE;
			assertSame(dataGroup.getDataNode(valueFieldName),
					positioner.getDataNode(NXpositioner.NX_VALUE));
			assertIndices(dataGroup, valueFieldName, defaultDimensionMappings);
			assertTarget(dataGroup, valueFieldName, rootNode,
					"/entry/instrument/" + positionerName + "/" + NXpositioner.NX_VALUE);
			i++;
		}
	}

}
