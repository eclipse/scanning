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

import static org.eclipse.dawnsci.nexus.builder.data.NexusDataBuilder.ATTR_NAME_AXES;
import static org.eclipse.dawnsci.nexus.builder.data.NexusDataBuilder.ATTR_NAME_SIGNAL;
import static org.eclipse.dawnsci.nexus.builder.data.NexusDataBuilder.ATTR_NAME_TARGET;
import static org.eclipse.dawnsci.nexus.builder.data.NexusDataBuilder.ATTR_SUFFIX_INDICES;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_DURATION;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_ESTIMATED_DURATION;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_FINISHED;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_SCAN_SHAPE;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.FIELD_NAME_UNIQUE_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_KEYS;
import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.GROUP_NAME_SOLSTICE_SCAN;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Iterator;
import java.util.List;

import org.eclipse.dawnsci.analysis.api.tree.Attribute;
import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.Node;
import org.eclipse.dawnsci.analysis.api.tree.NodeLink;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.DTypeUtils;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.PositionIterator;

/**
 * 
 * Copied to avoid dependency on org.eclipse.dawnsci.nexus.test which is not on the dawnsci p2
 * 
 * @author Matthew Gerring
 *
 */
public class NexusAssert {

	public static void assertAxes(NXdata nxData, String... expectedValues) {
		if (expectedValues.length == 0) return; // axes not written if no axes to write (a scalar signal field)
		Attribute axesAttr = nxData.getAttribute(ATTR_NAME_AXES);
		assertNotNull(axesAttr);
		assertEquals(1, axesAttr.getRank());
		assertEquals(expectedValues.length, axesAttr.getShape()[0]);
		IDataset value = axesAttr.getValue();
		for (int i = 0; i < expectedValues.length; i++) {
			assertTrue(value.getString(i).equals(expectedValues[i]));
		}
	}

	public static void assertIndices(NXdata nxData, String axisName, int... indices) {
		Attribute indicesAttr = nxData.getAttribute(axisName + ATTR_SUFFIX_INDICES);
		assertNotNull(indicesAttr);
		assertEquals(1, indicesAttr.getRank());
		assertEquals(indices.length, indicesAttr.getShape()[0]);
		IDataset value = indicesAttr.getValue();
		for (int i = 0; i < indices.length; i++) {
			assertEquals(indices[i], value.getInt(i));
		}
	}
	
	public static void assertTarget(NXdata nxData, String destName, NXroot nxRoot, String targetPath) {
		DataNode dataNode = nxData.getDataNode(destName);
		assertNotNull(dataNode);
		Attribute targetAttr = dataNode.getAttribute(ATTR_NAME_TARGET);
		assertNotNull(targetAttr);
		assertEquals(1, targetAttr.getSize());
		assertEquals(targetPath, targetAttr.getFirstElement());
		
		NodeLink nodeLink = nxRoot.findNodeLink(targetPath);
		assertTrue(nodeLink.isDestinationData());
		assertTrue(nodeLink.getDestination()==dataNode);
	}
	
	public static void assertSignal(NXdata nxData, String expectedSignalFieldName) {
		Attribute signalAttr = nxData.getAttribute(ATTR_NAME_SIGNAL);
		assertNotNull(signalAttr);
		assertEquals(1, signalAttr.getSize());
		assertEquals(expectedSignalFieldName, signalAttr.getFirstElement());
		assertNotNull(nxData.getDataNode(expectedSignalFieldName));
	}
	
	public static void assertSolsticeScanGroup(NXentry entry, boolean snake, int... sizes) {
		assertSolsticeScanGroup(entry, false, snake, sizes);
	}
	
	public static void assertSolsticeScanGroup(NXentry entry, boolean malcolmScan, boolean snake, int... sizes) {
		assertSolsticeScanGroup(entry, malcolmScan, null, snake, sizes);
	}
	
	public static void assertSolsticeScanGroup(NXentry entry, boolean malcolmScan,
			List<String> expectedExternalFiles, boolean snake, int... sizes) {
		assertScanFinished(entry);
		
		NXcollection solsticeScanCollection = entry.getCollection(GROUP_NAME_SOLSTICE_SCAN);
		assertNotNull(solsticeScanCollection);

		assertScanShape(solsticeScanCollection, sizes);
		assertScanTimes(solsticeScanCollection);
		 
		NXcollection keysCollection = (NXcollection) solsticeScanCollection.getGroupNode(GROUP_NAME_KEYS);
		assertNotNull(keysCollection);
		if (!malcolmScan) {
			assertUniqueKeys(keysCollection, snake, sizes);
		}
		if (expectedExternalFiles != null && !expectedExternalFiles.isEmpty()) {
			assertUniqueKeysExternalFileLinks(keysCollection, expectedExternalFiles, malcolmScan, sizes);
		}
	}
	
	private static void assertScanShape(NXcollection solsticeScanCollection, int... sizes) {
		DataNode shapeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_SHAPE);
		assertNotNull(shapeDataNode);
		IDataset shapeDataset;
		try {
			shapeDataset = shapeDataNode.getDataset().getSlice();
		} catch (DatasetException e) {
			throw new AssertionError("Could not get data from lazy dataset", e);
		}
		assertEquals(Integer.class, shapeDataset.getElementClass());
		assertEquals(1, shapeDataset.getRank());
		assertArrayEquals(new int[] { sizes.length }, shapeDataset.getShape());
		for (int i = 0; i < sizes.length; i++) {
			assertEquals(sizes[i], shapeDataset.getInt(i));
		}
	}
	
	private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder().
			appendPattern("HH:mm:ss").appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true).toFormatter();

	private static void assertScanTimes(NXcollection solsticeScanCollection) {
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
		formatter.parse(estimatedTime); // throws exception if not a valid time
		
		DataNode actualTimeDataNode = solsticeScanCollection.getDataNode(FIELD_NAME_SCAN_DURATION);
		assertNotNull(actualTimeDataNode);
		IDataset actualTimeDataset;
		try {
			actualTimeDataset = actualTimeDataNode.getDataset().getSlice();
		} catch (DatasetException e) {
			throw new AssertionError("Could not get data from lazy dataset", e);
		}
		
		// written as a 1d dataset of rank 1, as we can't write a scalar lazy writeable dataset
		assertEquals(String.class, actualTimeDataset.getElementClass());
		assertEquals(1, actualTimeDataset.getRank());
		assertArrayEquals(new int[]{ 1 }, actualTimeDataset.getShape());
		String actualTime = actualTimeDataset.getString(0);
		assertNotNull(actualTime);
		formatter.parse(estimatedTime); // throws exception if not a valid time
	}

	private static void assertUniqueKeys(NXcollection keysCollection, boolean snake, int... sizes) {
		// check the unique keys field - contains the step number for each scan
		// point
		DataNode dataNode = keysCollection.getDataNode(FIELD_NAME_UNIQUE_KEYS);
		assertNotNull(dataNode);
		IDataset dataset;
		try {
			dataset = dataNode.getDataset().getSlice();
		} catch (DatasetException e) {
			throw new AssertionError("Could not get data from lazy dataset", e);
		}
		assertEquals(Dataset.INT32, DTypeUtils.getDType(dataset));
		assertEquals(sizes.length, dataset.getRank());
		final int[] shape = dataset.getShape();
		assertArrayEquals(sizes, shape);
		PositionIterator iter = new PositionIterator(shape);

		int expectedPos = 1;

		if (snake) {
			final int lineSize = shape[shape.length - 1];
			boolean isBackwardLine = false;
			while (iter.hasNext()) { // hasNext also increments the position iterator (ugh!)
				assertEquals(expectedPos, dataset.getInt(iter.getPos()));
				if (!isBackwardLine && expectedPos % lineSize == 0) { // end of forward line of snake scan
					isBackwardLine = true;
					expectedPos = expectedPos + lineSize;
				} else if (isBackwardLine && expectedPos % lineSize == 1) { // end of backward line of snake scan
					isBackwardLine = false;
					expectedPos = expectedPos + lineSize;
				} else if (isBackwardLine) {
					expectedPos--;
				} else {
					expectedPos++;
				}
			}
		} else {
			while (iter.hasNext()) { // hasNext also increments the position iterator (ugh!)
				assertEquals(expectedPos, dataset.getInt(iter.getPos()));
				expectedPos++;
			}
		}
	}
	
	private static void assertUniqueKeysExternalFileLinks(NXcollection keysCollection,
			List<String> expectedExternalFiles, boolean malcolmScan, int... sizes) {
		for (String externalFileName : expectedExternalFiles) {
			String datasetName = externalFileName.replace("/", "__");
			DataNode dataNode = keysCollection.getDataNode(datasetName);
			assertNotNull(dataNode);
			assertEquals(sizes.length, dataNode.getRank());
		}
	}
	
	public static void assertScanFinished(NXentry entry) {
		assertScanFinished(entry, true);
	}

	public static void assertScanNotFinished(NXentry entry) {
		assertScanFinished(entry, false);
	}
	
	private static void assertScanFinished(NXentry entry, boolean finished) {
		NXcollection scanPointsCollection = entry.getCollection(GROUP_NAME_SOLSTICE_SCAN);
		assertNotNull(scanPointsCollection);
		
		// check the scan finished boolean is set to true
		DataNode dataNode = scanPointsCollection.getDataNode(FIELD_NAME_SCAN_FINISHED);
		assertNotNull(dataNode);
		IDataset dataset;
		try {
			dataset = dataNode.getDataset().getSlice();
		} catch (DatasetException e) {
			throw new AssertionError("Could not get data from lazy dataset", e);
		}
		assertEquals(Dataset.INT32, DTypeUtils.getDType(dataset)); // HDF5 doesn't support boolean datasets
		assertEquals(1, dataset.getRank());
		assertArrayEquals(new int[] {1}, dataset.getShape());
		assertEquals(finished, dataset.getBoolean(0));
	}
	
	public static void assertDataNodesEqual(final String path,
			final DataNode expectedDataNode, final DataNode actualDataNode) {
		// check number of attributes same (i.e. actualDataNode has no additional attributes)
		// additional attribute "target" is allowed, this is added automatically when saving the file
		int expectedNumAttributes = expectedDataNode.getNumberOfAttributes();
		if (expectedDataNode.containsAttribute("target") && !actualDataNode.containsAttribute("target")) {
			expectedNumAttributes--;
		}
		assertEquals(expectedNumAttributes, actualDataNode.getNumberOfAttributes());
		
		// check attributes properties same for each attribute
		Iterator<String> attributeNameIterator = expectedDataNode.getAttributeNameIterator();
		while (attributeNameIterator.hasNext()) {
			String attributeName = attributeNameIterator.next();
			String attrPath = path + Node.ATTRIBUTE + attributeName;
			Attribute expectedAttr = expectedDataNode.getAttribute(attributeName);
			Attribute actualAttr = actualDataNode.getAttribute(attributeName);
			if (!expectedAttr.getName().equals("target")) {
				assertNotNull(attrPath, expectedAttr);
				assertAttributesEquals(attrPath, expectedAttr, actualAttr);
			}
		}

		assertEquals(path, expectedDataNode.getTypeName(), actualDataNode.getTypeName());
		assertEquals(path, expectedDataNode.isAugmented(), actualDataNode.isAugmented());
		assertEquals(path, expectedDataNode.isString(), actualDataNode.isString());
		assertEquals(path, expectedDataNode.isSupported(), actualDataNode.isSupported());
		assertEquals(path, expectedDataNode.isUnsigned(), actualDataNode.isUnsigned());
		assertEquals(path, expectedDataNode.getMaxStringLength(), actualDataNode.getMaxStringLength());
		// TODO reinstate lines below and check why they break - dataNode2 is null
//		assertArrayEquals(path, dataNode1.getMaxShape(), dataNode2.getMaxShape());
//		assertArrayEquals(path, dataNode1.getChunkShape(), dataNode2.getChunkShape());
		assertEquals(path, expectedDataNode.getString(), actualDataNode.getString());
		assertDatasetsEqual(path, expectedDataNode.getDataset(), actualDataNode.getDataset());
	}
	
	public static void assertAttributesEquals(final String path, final Attribute expectedAttr,
			final Attribute actualAttr) {
		assertEquals(path, expectedAttr.getName(), actualAttr.getName());
		assertEquals(path, expectedAttr.getTypeName(), actualAttr.getTypeName());
		assertEquals(path, expectedAttr.getFirstElement(), actualAttr.getFirstElement());
		assertEquals(path, expectedAttr.getSize(), actualAttr.getSize());
		if (expectedAttr.getSize() == 1 && expectedAttr.getRank() == 1 && actualAttr.getRank() == 0) {
			// TODO fix examples now that we can save scalar (or zero-ranked) datasets
			actualAttr.getValue().setShape(1);
		}
		assertEquals(path, expectedAttr.getRank(), actualAttr.getRank());
		assertArrayEquals(path, expectedAttr.getShape(), actualAttr.getShape());
		assertDatasetsEqual(path, expectedAttr.getValue(), actualAttr.getValue());
	}

	public static void assertDatasetsEqual(final String path, final ILazyDataset expectedDataset,
			final ILazyDataset actualDataset) {
		// Note: dataset names can be different, as long as the containing data node names are the same
		// assertEquals(dataset1.getName(), dataset2.getName());
		// assertEquals(dataset1.getClass(), dataset2.getClass());
		assertEquals(path, expectedDataset.getElementClass(), actualDataset.getElementClass());
		assertEquals(path, expectedDataset.getElementsPerItem(), actualDataset.getElementsPerItem());
		assertEquals(path, expectedDataset.getSize(), actualDataset.getSize());
		if (expectedDataset.getSize() == 1 && expectedDataset.getRank() == 1 && actualDataset.getRank() == 0) {
			// TODO fix examples now that we can save scalar (or zero-ranked) datasets
			actualDataset.setShape(1);
		}
		assertEquals(path, expectedDataset.getRank(), actualDataset.getRank());
		assertArrayEquals(path, expectedDataset.getShape(), actualDataset.getShape());
		assertDatasetDataEqual(path, expectedDataset, actualDataset);

		// TODO: in future also check metadata
	}


	private static void assertDatasetDataEqual(final String path,
			final ILazyDataset expectedDataset, final ILazyDataset actualDataset) {
		if (expectedDataset instanceof Dataset && actualDataset instanceof Dataset) {
			assertEquals(path, expectedDataset, actualDataset); // uses Dataset.equals() method
		} else {
			assertEquals(expectedDataset.getSize(), actualDataset.getSize());
			if (expectedDataset.getSize() == 0) {
				return;
			}
			
			// getSlice() with no args loads whole dataset if a lazy dataset
			IDataset expectedSlice;
			IDataset actualSlice;
			try {
				expectedSlice = expectedDataset.getSlice();
				actualSlice = actualDataset.getSlice();
			} catch (DatasetException e) {
				throw new AssertionError("Could not get data from lazy dataset", e.getCause());
			}

			final int datatype = DTypeUtils.getDType(actualDataset);
			PositionIterator positionIterator = new PositionIterator(actualDataset.getShape());
			while (positionIterator.hasNext()) {
				int[] position = positionIterator.getPos();
				switch (datatype) {
				case Dataset.BOOL:
					assertEquals(path, expectedSlice.getBoolean(position), actualSlice.getBoolean(position));
					break;
				case Dataset.INT8:
					assertEquals(path, expectedSlice.getByte(position), actualSlice.getByte(position));
					break;
				case Dataset.INT32:
					assertEquals(path, expectedSlice.getInt(position), actualSlice.getInt(position));
					break;
				case Dataset.INT64:
					assertEquals(path, expectedSlice.getLong(position), actualSlice.getLong(position));
					break;
				case Dataset.FLOAT32:
					assertEquals(path, expectedSlice.getFloat(position), actualSlice.getFloat(position), 1e-7);
					break;
				case Dataset.FLOAT64:
					assertEquals(path, expectedSlice.getDouble(position), actualSlice.getDouble(position), 1e-15);
					break;
				case Dataset.STRING:
				case Dataset.DATE:
					assertEquals(path, expectedSlice.getString(position), actualSlice.getString(position));
					break;
				case Dataset.COMPLEX64:
				case Dataset.COMPLEX128:
				case Dataset.OBJECT:
					assertEquals(path, expectedSlice.getObject(position), actualSlice.getObject(position));
					break;
				}
			}
		}
	}

}
