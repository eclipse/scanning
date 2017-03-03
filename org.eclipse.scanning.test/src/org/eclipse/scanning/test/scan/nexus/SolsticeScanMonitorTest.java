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

import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_DURATION;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_ESTIMATED_DURATION;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_FINISHED;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_RANK;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_SHAPE;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_UNIQUE_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_KEYS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.SymbolicNode;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.AbstractNexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.january.DatasetException;
import org.eclipse.january.IMonitor;
import org.eclipse.january.dataset.DTypeUtils;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.ILazyWriteableDataset;
import org.eclipse.january.dataset.LazyDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.january.io.ILazySaver;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.points.StaticPosition;
import org.eclipse.scanning.api.scan.ScanEstimator;
import org.eclipse.scanning.api.scan.ScanInformation;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.sequencer.nexus.SolsticeScanMonitor;
import org.junit.Test;


public class SolsticeScanMonitorTest {
	
	public static class MockLazySaver implements ILazySaver {

		private static final long serialVersionUID = 1L;

		private IDataset lastWrittenData = null;
		
		private SliceND lastSlice = null;
		
		private int numWrites = 0;
		
		@Override
		public boolean isFileReadable() {
			return true;
		}

		@Override
		public IDataset getDataset(IMonitor mon, SliceND slice) throws IOException {
			return null;
		}

		@Override
		public void initialize() throws IOException {
			// do nothing
		}

		@Override
		public boolean isFileWriteable() {
			return true;
		}

		@Override
		public void setSlice(IMonitor mon, IDataset data, SliceND slice) throws IOException {
			// TODO could write to a dataset? SliceIterator may be useful here
			lastWrittenData = data;
			lastSlice = slice;
			numWrites++;
		}
		
		public IDataset getLastWrittenData() {
			return lastWrittenData;
		}
		
		public SliceND getLastSlice() {
			return lastSlice;
		}
		
		public int getNumberOfWrites() {
			return numWrites;
		}
		
	}
	
	public static class ExternalFileWritingPositioner extends AbstractNexusObjectProvider<NXpositioner> {
		
		public ExternalFileWritingPositioner(String name) {
			super(name, NexusBaseClass.NX_POSITIONER, NXpositioner.NX_VALUE);
			addExternalFileName("panda.nxs");
			setPropertyValue("uniqueKeys", MALCOLM_UNIQUE_KEYS_PATH);
		}
		
		@Override
		protected NXpositioner createNexusObject() {
			final NXpositioner positioner = NexusNodeFactory.createNXpositioner();
			addExternalLink(positioner, NXpositioner.NX_VALUE, "/entry/data", 2);
			
			return positioner;
		}
	}
	
	public static class ExternalFileWritingDetector extends AbstractNexusObjectProvider<NXdetector> {
		
		public static final String EXTERNAL_FILE_NAME = "detector.nxs";
		
		public ExternalFileWritingDetector() {
			super("detector", NexusBaseClass.NX_DETECTOR);
			addExternalFileName(EXTERNAL_FILE_NAME);
			setPropertyValue("uniqueKeys", MALCOLM_UNIQUE_KEYS_PATH);
		}

		@Override
		protected NXdetector createNexusObject() {
			final NXdetector detector = NexusNodeFactory.createNXdetector();
			addExternalLink(detector, NXdetector.NX_DATA, "/entry/data", 4);
			
			return detector;
		}
		
	}
	
	private static final String MALCOLM_UNIQUE_KEYS_PATH = "/entry/NDAttributes/NDArrayUniqueId";
	
	
	@Test
	public void testCreateNexusObject() throws Exception {
		// Arrange
		List<NexusObjectProvider<?>> nexusObjectProviders = new ArrayList<>();
		nexusObjectProviders.add(new ExternalFileWritingDetector());
		String[] positionerNames = new String[] { "xPos", "yPos" };
		for (String positionerName : positionerNames) {
			nexusObjectProviders.add(new ExternalFileWritingPositioner(positionerName));
		}

		ScanModel scanModel = new ScanModel();
		Iterable<IPosition> positions = Collections.nCopies(25, new StaticPosition());
		ScanEstimator scanEstimator = new ScanEstimator(positions, null, 100);
		scanModel.setScanInformation(new ScanInformation(scanEstimator));
		SolsticeScanMonitor solsticeScanMonitor = new SolsticeScanMonitor(scanModel);
		solsticeScanMonitor.setNexusObjectProviders(nexusObjectProviders);
		
		final int[] scanShape = new int[] { 8, 5 };
		final int scanRank = scanShape.length;
		NexusScanInfo scanInfo = new NexusScanInfo();
		scanInfo.setRank(scanRank);
		scanInfo.setShape(scanShape);
		int[] expectedChunking = new int[scanInfo.getRank()];
		Arrays.fill(expectedChunking, 1);
		expectedChunking[expectedChunking.length-1] = 8;

		// Act
		NXcollection solsticeScanCollection = solsticeScanMonitor.createNexusObject(scanInfo);
		
		// Assert
		assertNotNull(solsticeScanCollection);

		// assert scan finished dataset created correctly - value must be false
		DataNode scanFinishedDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_FINISHED);
		assertTrue(scanFinishedDataNode!=null);
		assertTrue(scanFinishedDataNode.getDataset()!=null && scanFinishedDataNode.getDataset() instanceof ILazyWriteableDataset);
		ILazyWriteableDataset scanFinishedDataset = (ILazyWriteableDataset) scanFinishedDataNode.getDataset();
		assertTrue(scanFinishedDataset.getRank()==1);
		assertTrue(Arrays.equals(scanFinishedDataset.getShape(), new int[] { 1 }));
		MockLazySaver scanFinishedSaver = new MockLazySaver();
		scanFinishedDataset.setSaver(scanFinishedSaver);
		
		// assert scan rank set correctly
		DataNode scanRankDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_RANK);
		assertEquals(scanRank, scanRankDataNode.getDataset().getSlice().getInt());
		
		// assert scan shape set correctly
		DataNode scanShapeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_SHAPE);
		assertNotNull(scanFinishedDataNode);
		IDataset shapeDataset = scanShapeDataNode.getDataset().getSlice();
		assertNotNull(shapeDataset);
		assertEquals(1, shapeDataset.getRank());
		assertEquals(Integer.class, shapeDataset.getElementClass());
		assertArrayEquals(new int[] { scanRank }, shapeDataset.getShape());
		for (int i = 0; i < scanShape.length; i++) {
			assertEquals(scanShape[i], shapeDataset.getInt(i));
		}
		
		// assert that the estimated time has been written
		DataNode estimatedTimeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_ESTIMATED_DURATION);
		assertNotNull(estimatedTimeDataNode);
		IDataset estimatedTimeDataset;
		try {
			estimatedTimeDataset = estimatedTimeDataNode.getDataset().getSlice();
		} catch (DatasetException e) {
			throw new AssertionError("Could not get data from lazy dataset", e);
		}
		
		assertEquals(String.class, estimatedTimeDataset.getElementClass());
		assertEquals(0, estimatedTimeDataset.getRank());
		assertArrayEquals(new int[]{}, estimatedTimeDataset.getShape());
		String estimatedTime = estimatedTimeDataset.getString();
		assertNotNull(estimatedTime);
		assertEquals("00:00:02.500", estimatedTime);
		
		// assert the actual time dataset has been created - note it hasn't been written to yet
		DataNode actualTimeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_DURATION);
		assertNotNull(actualTimeDataNode);
		ILazyDataset actualTimeDataset = actualTimeDataNode.getDataset();
		assertNotNull(actualTimeDataset);
		assertEquals(String.class, actualTimeDataset.getElementClass());
		
		// assert unique keys dataset created correctly
		NXcollection keysCollection = (NXcollection) solsticeScanCollection.getGroupNode(GROUP_NAME_KEYS);
		assertNotNull(keysCollection);
		
		DataNode uniqueKeysDataNode = keysCollection.getDataNode(FIELD_NAME_UNIQUE_KEYS);
		assertTrue(uniqueKeysDataNode!=null);
		assertTrue(uniqueKeysDataNode.getDataset()!=null && uniqueKeysDataNode.getDataset() instanceof ILazyWriteableDataset);
		ILazyWriteableDataset uniqueKeysDataset = (ILazyWriteableDataset) uniqueKeysDataNode.getDataset();
		assertTrue(uniqueKeysDataset.getRank()==scanRank);
		assertTrue(((LazyDataset) uniqueKeysDataset).getDType()==Dataset.INT32);
		assertTrue(Arrays.equals(uniqueKeysDataset.getChunking(), expectedChunking));
		MockLazySaver uniqueKeysSaver = new MockLazySaver();
		uniqueKeysDataset.setSaver(uniqueKeysSaver);
		
		// assert links to external nodes
		assertEquals(3, keysCollection.getNumberOfNodelinks());
		for (NexusObjectProvider<?> objectProvider : nexusObjectProviders) {
			for (String externalFilename : objectProvider.getExternalFileNames()) {
				String datasetName = externalFilename.replace("/", "__");
				SymbolicNode symbolicNode = keysCollection.getSymbolicNode(datasetName);
				assertNotNull(symbolicNode);
				assertEquals(objectProvider.getPropertyValue("uniqueKeys"), symbolicNode.getPath());
			}
		}
	}
	
	@Test
	public void testWriteScanPoints() throws Exception {
		// Arrange - we have to create the nexus object first 
		List<NexusObjectProvider<?>> nexusObjectProviders = new ArrayList<>();
		nexusObjectProviders.add(new ExternalFileWritingDetector());
		String[] positionerNames = new String[] { "xPos", "yPos" };
		for (String positionerName : positionerNames) {
			nexusObjectProviders.add(new ExternalFileWritingPositioner(positionerName));
		}

		ScanModel scanModel = new ScanModel();
		Iterable<IPosition> positions = Collections.nCopies(25, new StaticPosition());
		ScanEstimator scanEstimator = new ScanEstimator(positions, null, 100);
		scanModel.setScanInformation(new ScanInformation(scanEstimator));
		SolsticeScanMonitor solsticeScanMonitor = new SolsticeScanMonitor(scanModel);
		solsticeScanMonitor.setNexusObjectProviders(nexusObjectProviders);
		
		final int[] scanShape = new int[] { 8, 5 };
		final int scanRank = scanShape.length;
		NexusScanInfo scanInfo = new NexusScanInfo();
		scanInfo.setRank(scanRank);
		scanInfo.setShape(scanShape);
		int[] expectedChunking = new int[scanInfo.getRank()];
		Arrays.fill(expectedChunking, 1);

		// Act
		NXcollection solsticeScanCollection = solsticeScanMonitor.createNexusObject(scanInfo);
		
		// Assert
		assertNotNull(solsticeScanCollection);
		DataNode scanFinishedDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_FINISHED);
		ILazyWriteableDataset scanFinishedDataset = (ILazyWriteableDataset) scanFinishedDataNode.getDataset();
		MockLazySaver scanFinishedSaver = new MockLazySaver();
		scanFinishedDataset.setSaver(scanFinishedSaver);
		DataNode actualTimeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_DURATION);
		ILazyWriteableDataset actualTimeDataset = (ILazyWriteableDataset) actualTimeDataNode.getDataset();
		MockLazySaver actualTimeSaver = new MockLazySaver();
		actualTimeDataset.setSaver(actualTimeSaver);
		
		// assert scan shape set correctly
		DataNode scanShapeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_SHAPE);
		assertNotNull(scanFinishedDataNode);
		IDataset shapeDataset = scanShapeDataNode.getDataset().getSlice();
		assertNotNull(shapeDataset);
		assertEquals(1, shapeDataset.getRank());
		assertEquals(Integer.class, shapeDataset.getElementClass());
		assertArrayEquals(new int[] { scanRank }, shapeDataset.getShape());
		for (int i = 0; i < scanShape.length; i++) {
			assertEquals(scanShape[i], shapeDataset.getInt(i));
		}
		
		DataNode estimatedTimeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_ESTIMATED_DURATION);
		assertNotNull(estimatedTimeDataNode);
		IDataset estimatedTimeDataset;
		try {
			estimatedTimeDataset = estimatedTimeDataNode.getDataset().getSlice();
		} catch (DatasetException e) {
			throw new AssertionError("Could not get data from lazy dataset", e);
		}
		
		// assert that the estimated time has been written
		assertEquals(String.class, estimatedTimeDataset.getElementClass());
		assertEquals(0, estimatedTimeDataset.getRank());
		assertArrayEquals(new int[]{}, estimatedTimeDataset.getShape());
		String estimatedTime = estimatedTimeDataset.getString();
		assertNotNull(estimatedTime);
		assertEquals("00:00:02.500", estimatedTime);
		
		// assert the actual time dataset has been created - note it hasn't been written to yet
		assertNotNull(actualTimeDataset);
		assertEquals(String.class, actualTimeDataset.getElementClass());

		// TODO what can we assert about the value		
		// assert unique keys dataset created correctly
		NXcollection keysCollection = (NXcollection) solsticeScanCollection.getGroupNode(GROUP_NAME_KEYS);
		assertNotNull(keysCollection);
		
		DataNode uniqueKeysDataNode = keysCollection.getDataNode(FIELD_NAME_UNIQUE_KEYS);
		ILazyWriteableDataset uniqueKeysDataset = (ILazyWriteableDataset) uniqueKeysDataNode.getDataset();
		MockLazySaver uniqueKeysSaver = new MockLazySaver();
		uniqueKeysDataset.setSaver(uniqueKeysSaver);
		
		// assert links to external nodes
		assertEquals(3, keysCollection.getNumberOfNodelinks());
		for (NexusObjectProvider<?> objectProvider : nexusObjectProviders) {
			for (String externalFilename : objectProvider.getExternalFileNames()) {
				String datasetName = externalFilename.replace("/", "__");
				SymbolicNode symbolicNode = keysCollection.getSymbolicNode(datasetName);
				assertNotNull(symbolicNode);
				assertEquals(objectProvider.getPropertyValue("uniqueKeys"), symbolicNode.getPath());
			}
		}
		
		// test calling setPosition
		// arrange
		double[] pos = new double[] { 172.5, 56.3 };
		int[] indices = new int[] { 8, 3 };
		int stepIndex = 23;
		MapPosition position = new MapPosition();
		position.setStepIndex(stepIndex);
		List<Collection<String>> names = new ArrayList<>( positionerNames.length);
		for (int i = 0; i < positionerNames.length; i++) {
			position.put(positionerNames[i], pos[i]);
			position.putIndex(positionerNames[i], indices[i]);
			names.add(Arrays.asList(positionerNames[i]));
		}
		position.setDimensionNames(names);
		
		// act
		solsticeScanMonitor.setPosition(null, position);
		solsticeScanMonitor.scanFinished();

		// assert
		// check data written to scan finished dataset
		IDataset writtenToScanFinishedData = scanFinishedSaver.getLastWrittenData();
		assertNotNull(writtenToScanFinishedData);
		assertEquals(0, writtenToScanFinishedData.getRank());
		assertArrayEquals(new int[0], writtenToScanFinishedData.getShape());
		assertTrue(DTypeUtils.getDType(writtenToScanFinishedData)==Dataset.INT);
		assertEquals(1, writtenToScanFinishedData.getInt());
		
		// check data written to actual time dataset
		IDataset writtenToActualTimeDataset = actualTimeSaver.getLastWrittenData();
		assertNotNull(writtenToActualTimeDataset);
		assertEquals(0, writtenToActualTimeDataset.getRank());
		assertArrayEquals(new int[0], writtenToActualTimeDataset.getShape());
		assertTrue(DTypeUtils.getDType(writtenToActualTimeDataset)==Dataset.STRING);
		
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().
				appendPattern("HH:mm:ss").appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true).toFormatter();
		String actualTime = writtenToActualTimeDataset.getString();
		formatter.parse(actualTime);
		
		// check data written to unique keys dataset
		IDataset writtenToUniqueKeysData = uniqueKeysSaver.getLastWrittenData();
		assertNotNull(writtenToUniqueKeysData);
		int[] expectedShape = new int[scanInfo.getRank()];
		Arrays.fill(expectedShape, 1);
		assertArrayEquals(writtenToUniqueKeysData.getShape(), expectedShape);
		assertTrue(DTypeUtils.getDType(writtenToUniqueKeysData)==Dataset.INT);
		int[] valuePos = new int[scanRank]; // all zeros
		assertTrue(writtenToUniqueKeysData.getInt(valuePos)==(stepIndex+1));

		SliceND uniqueKeysSlice = uniqueKeysSaver.getLastSlice();
		assertTrue(uniqueKeysSlice!=null);
		assertArrayEquals(uniqueKeysSlice.getShape(), expectedShape);
		assertArrayEquals(uniqueKeysSlice.getStart(), indices);
		assertArrayEquals(uniqueKeysSlice.getStep(), expectedShape); // all ones
		int[] stopIndices = Arrays.stream(indices).map(x -> x + 1).toArray(); 
		assertArrayEquals(uniqueKeysSlice.getStop(), stopIndices);
	}
	
}
