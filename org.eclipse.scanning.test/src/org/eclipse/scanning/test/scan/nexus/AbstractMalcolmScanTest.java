/*-
 *******************************************************************************
 * Copyright (c) 2011, 2016 Diamond Light Source Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Gerring - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.scanning.test.scan.nexus;

import static org.eclipse.scanning.example.malcolm.DummyMalcolmDevice.FILE_EXTENSION_HDF5;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertAxes;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertDataNodesEqual;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertIndices;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSignal;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.event.scan.ScanBean;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmControlledDetectorModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmDatasetModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractMalcolmScanTest extends NexusTest {
	
	protected String malcolmOutputDir;

	protected IRunnableDevice<DummyMalcolmModel> malcolmDevice;

	protected MockScanParticpiant participant;
	
	@Before
	public void before() throws Exception {
		// create a temp directory for the dummy malcolm device to write hdf files into
		malcolmOutputDir = org.eclipse.scanning.sequencer.ServiceHolder.getFilePathService().createFolderForLinkedFiles(output.getName());
		final DummyMalcolmModel model = createMalcolmModel();
		
		malcolmDevice = dservice.createRunnableDevice(model);
		assertNotNull(malcolmDevice);
		((AbstractMalcolmDevice<?>) malcolmDevice).addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException {
				System.out.println("Ran test malcolm device @ " + evt.getPosition());
			}
		});
		participant = new MockScanParticpiant();
		dservice.addScanParticipant(participant);
	}
	
	@After
	public void teardown() throws Exception {
		// delete the temp directory and all its files
		for (File file : new File(malcolmOutputDir).listFiles()) {
			file.delete();
		}
		new File(malcolmOutputDir).delete();
		participant.clear();
	}
	
	protected abstract DummyMalcolmModel createMalcolmModel(); 

	// Create model, but do not set axes etc.
	protected DummyMalcolmModel createMalcolmModelTwoDetectors() {
		DummyMalcolmModel model = new DummyMalcolmModel();
		model.setTimeout(10 * 60); // increased timeout for debugging purposes
		model.setFileDir(malcolmOutputDir);

		DummyMalcolmControlledDetectorModel det1Model = new DummyMalcolmControlledDetectorModel();
		det1Model.setName("detector");

		DummyMalcolmDatasetModel detector1dataset1 = new DummyMalcolmDatasetModel();
		detector1dataset1.setName("detector");
		detector1dataset1.setRank(2);
		detector1dataset1.setDtype(Double.class);
	
		DummyMalcolmDatasetModel detector1dataset2 = new DummyMalcolmDatasetModel();
		detector1dataset2.setName("sum");
		detector1dataset2.setRank(1);
		detector1dataset2.setDtype(Double.class);
		det1Model.setDatasets(Arrays.asList(detector1dataset1, detector1dataset2));

		DummyMalcolmControlledDetectorModel det2Model = new DummyMalcolmControlledDetectorModel();
		det2Model.setName("detector2");

		DummyMalcolmDatasetModel detector2dataset = new DummyMalcolmDatasetModel();
		detector2dataset.setName("detector2");
		detector2dataset.setRank(2);
		detector2dataset.setDtype(Double.class);
		det2Model.setDatasets(Arrays.asList(detector2dataset));

		model.setDummyDetectorModels(Arrays.asList(det1Model, det2Model));

		return model;
	}
	protected void checkSize(IRunnableDevice<?> scanner, int[] shape) {
		@SuppressWarnings("unchecked")
		ScanBean bean =  ((AbstractRunnableDevice<ScanBean>) scanner).getBean();
		int expectedSize = Arrays.stream(shape).reduce(1, (x, y) -> x * y);
		System.err.println("Expected size = " + expectedSize + ", shape = " + Arrays.toString(shape));
		assertEquals(expectedSize, bean.getSize());
	}
	
	private Map<String, List<String>> getExpectedPrimaryDataFieldsPerDetector() {
		Map<String, List<String>> primaryDataFieldsPerDetector = new HashMap<>();
		for (DummyMalcolmControlledDetectorModel detectorModel : malcolmDevice.getModel().getDummyDetectorModels()) {
			List<String> list = detectorModel.getDatasets().stream().map(d -> d.getName())
				.collect(Collectors.toCollection(ArrayList::new));
			list.set(0, NXdata.NX_DATA); // the first dataset is the primary one, so the field is called 'data' in the nexus tree
			primaryDataFieldsPerDetector.put(detectorModel.getName(), list);
		}
		
		return primaryDataFieldsPerDetector;
	}
	
	private List<String> getExpectedExternalFiles(DummyMalcolmModel dummyMalcolmModel) {
		List<String> expectedFileNames = dummyMalcolmModel.getDummyDetectorModels().stream()
			.map(d -> d.getName() + FILE_EXTENSION_HDF5)
			.collect(Collectors.toCollection(ArrayList::new));
		if (!dummyMalcolmModel.getPositionerNames().isEmpty()) {
			expectedFileNames.add("panda" + FILE_EXTENSION_HDF5);
		}
		
		return expectedFileNames;
	}
	
	protected NXentry checkNexusFile(IRunnableDevice<ScanModel> scanner, boolean snake, int[] sizes) throws Exception {
		final DummyMalcolmModel dummyMalcolmModel = malcolmDevice.getModel();
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();
		
		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		// check that the scan points have been written correctly
		List<String> expectedExternalFiles = getExpectedExternalFiles(dummyMalcolmModel);
		assertSolsticeScanGroup(entry, true, snake, false, expectedExternalFiles, sizes);
		
		// map from detector name -> primary data fields
		Map<String, List<String>> primaryDataFieldNamesPerDetector = getExpectedPrimaryDataFieldsPerDetector();
		Map<String, NXdata> nxDataGroups = entry.getChildren(NXdata.class);
		assertEquals(primaryDataFieldNamesPerDetector.values().stream().flatMap(list -> list.stream()).count(),
				nxDataGroups.size());

		for (DummyMalcolmControlledDetectorModel detectorModel : dummyMalcolmModel.getDummyDetectorModels()) {
			String detectorName = detectorModel.getName();
			NXdetector detector = instrument.getDetector(detectorName);
			
			assertEquals(dummyMalcolmModel.getExposureTime(), detector.getCount_timeScalar().doubleValue(), 1e-15);
			
			List<String> primaryDataFieldNames = primaryDataFieldNamesPerDetector.get(detectorName);
			Map<String, String> expectedDataGroupNames = primaryDataFieldNames.stream().collect(Collectors.toMap(
					Function.identity(),
					fieldName -> detectorName + (fieldName.equals(NXdetector.NX_DATA) ? "" : "_" + fieldName)));
			
			assertTrue(nxDataGroups.keySet().containsAll(expectedDataGroupNames.values()));
			
			boolean isFirst = true;
			for (DummyMalcolmDatasetModel datasetModel : detectorModel.getDatasets()) {
				String fieldName = datasetModel.getName();
				String nxDataGroupName = isFirst ? detectorName : detectorName + "_" + fieldName;
				NXdata nxData = entry.getData(nxDataGroupName);
				
				String sourceFieldName = fieldName.equals(detectorName) ? NXdetector.NX_DATA :
					fieldName.substring(fieldName.indexOf('_') + 1);
				
				assertSignal(nxData, sourceFieldName);
				// check the nxData's signal field is a link to the appropriate source data node of the detector
				DataNode dataNode = detector.getDataNode(sourceFieldName);
				IDataset dataset = dataNode.getDataset().getSlice();
				// test the data nodes for equality instead of identity as they both come from external links
				assertDataNodesEqual("/entry/instrument/"+detectorName+"/"+sourceFieldName,
						dataNode, nxData.getDataNode(sourceFieldName));
//				assertTarget(nxData, sourceFieldName, rootNode, "/entry/instrument/" + detectorName
//						+ "/" + sourceFieldName);
				
				// check that other primary data fields of the detector haven't been added to this NXdata
				for (String primaryDataFieldName : primaryDataFieldNames) {
					if (!primaryDataFieldName.equals(sourceFieldName)) {
						assertNull(nxData.getDataNode(primaryDataFieldName));
					}
				}
				
				int[] shape = dataset.getShape(); 
				for (int i = 0; i < sizes.length; i++) 
					assertEquals(sizes[i], shape[i]);
				
				// Make sure none of the numbers are NaNs. The detector is expected
				// to fill this scan with non-nulls
				final PositionIterator it = new PositionIterator(shape);
				while (it.hasNext()) {
					int[] next = it.getPos();
					assertFalse(Double.isNaN(dataset.getDouble(next)));
				}
				
				// Check axes
				final IPosition pos = scanModel.getPositionIterable().iterator().next();
				final Collection<String> axisNames = pos.getNames();
				
				// Append _value_set to each name in list, then add detector axes fields to result
				// stage_y doesn't have _value_set as to mimic the real i18 malcolm device
				// malcolm doesn't know this value (as instead it has three jacks j1,j2,j3 for the y position)
				int additionalRank = datasetModel.getRank(); // i.e. rank per position, e.g. 2 for images
				List<String> expectedAxesNames = Stream.concat(
						axisNames.stream().map(axisName -> axisName + 
								(!axisName.equals("stage_y") ? "_value_set" : "")),
						Collections.nCopies(additionalRank, ".").stream()).collect(Collectors.toList()); // TODO 2 should be what number?
				assertAxes(nxData, expectedAxesNames.toArray(new String[expectedAxesNames.size()]));
				
				int[] defaultDimensionMappings = IntStream.range(0, sizes.length).toArray();
				int i = -1;
				for (String axisName : axisNames) {
					i++;
					NXpositioner positioner = instrument.getPositioner(axisName);
					assertNotNull(positioner);
					
					dataNode = positioner.getDataNode("value_set");
					dataset = dataNode.getDataset().getSlice();
					shape = dataset.getShape();
					assertEquals(1, shape.length);
					assertEquals(sizes[i], shape[0]);
					
					String nxDataFieldName = axisName + (!axisName.equals("stage_y") ? "_value_set" : "");
					assertDataNodesEqual("", dataNode, nxData.getDataNode(nxDataFieldName));
					assertIndices(nxData, nxDataFieldName, i);
					// The value of the target attribute seems to come from the external file
//					assertTarget(nxData, nxDataFieldName, rootNode,
//							"/entry/" + firstDetectorName + "/" + nxDataFieldName);
					
					// value field (a.k.a rbv) only created if in list of positioners in model
					if (dummyMalcolmModel.getPositionerNames().contains(axisName)) {
						// Actual values should be scanD
						dataNode = positioner.getDataNode(NXpositioner.NX_VALUE);
						assertNotNull(dataNode);
						dataset = dataNode.getDataset().getSlice();
						shape = dataset.getShape();
						assertArrayEquals(sizes, shape);
						
						nxDataFieldName = axisName + "_" + NXpositioner.NX_VALUE;
//						assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
						assertDataNodesEqual("", dataNode, nxData.getDataNode(nxDataFieldName));
						assertIndices(nxData, nxDataFieldName, defaultDimensionMappings);
	//					assertTarget(nxData, nxDataFieldName, rootNode,
	//							"/entry/instrument/" + axisName + "/" + NXpositioner.NX_VALUE);
					}
				}
				isFirst = false;
			}
		}
		
		return entry;
	}
}
