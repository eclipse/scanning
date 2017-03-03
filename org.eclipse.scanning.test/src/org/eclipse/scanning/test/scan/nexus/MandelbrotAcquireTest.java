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

import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertAxes;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSignal;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.analysis.api.tree.TreeFile;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXdetector;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NexusFile;
import org.eclipse.dawnsci.nexus.NexusUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.PositionIterator;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.junit.Before;
import org.junit.Test;

public class MandelbrotAcquireTest extends NexusTest {
	
	private static IWritableDetector<MandelbrotModel> detector;

	@Before
	public void before() throws Exception {
		MandelbrotModel model = createMandelbrotModel();
		detector = (IWritableDetector<MandelbrotModel>)dservice.createRunnableDevice(model);
		assertNotNull(detector);
		detector.addRunListener(new IRunListener() {
			@Override
			public void runPerformed(RunEvent evt) throws ScanningException{
				//System.out.println("Ran mandelbrot detector @ "+evt.getPosition());
			}
		});
	}
	
	@Test
	public void testAcquire() throws Exception {
		IRunnableDevice<ScanModel> scanner = createAcquireScan(detector, output);
		scanner.run(null);
	
		checkNexusFile(scanner);
	}
	
	private NXroot getNexusRoot(IRunnableDevice<ScanModel> scanner) throws Exception {
		String filePath = ((AbstractRunnableDevice<ScanModel>) scanner).getModel().getFilePath();

		INexusFileFactory fileFactory = org.eclipse.dawnsci.nexus.ServiceHolder.getNexusFileFactory();
		NexusFile nf = fileFactory.newNexusFile(filePath);
		nf.openToRead();
		
		TreeFile nexusTree = NexusUtils.loadNexusTree(nf);
		return (NXroot) nexusTree.getGroupNode();
	}
	
	private void checkNexusFile(IRunnableDevice<ScanModel> scanner) throws Exception {
		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();
		assertEquals(DeviceState.READY, scanner.getDeviceState());

		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();
		
		LinkedHashMap<String, List<String>> signalFieldAxes = new LinkedHashMap<>();
		// axis for additional dimensions of a datafield, e.g. image
		signalFieldAxes.put(NXdetector.NX_DATA, Arrays.asList("real", "imaginary"));
		signalFieldAxes.put("spectrum", Arrays.asList("spectrum_axis"));
		signalFieldAxes.put("value", Collections.emptyList());
		
		String detectorName = scanModel.getDetectors().get(0).getName();
		NXdetector detector = instrument.getDetector(detectorName);
		// map of detector data field to name of nxData group where that field is the @signal field
		Map<String, String> expectedDataGroupNames =
				signalFieldAxes.keySet().stream().collect(Collectors.toMap(Function.identity(),
				x -> detectorName + (x.equals(NXdetector.NX_DATA) ? "" : "_" + x)));

		// validate the main NXdata generated by the NexusDataBuilder
		Map<String, NXdata> nxDataGroups = entry.getChildren(NXdata.class);
		assertEquals(signalFieldAxes.size(), nxDataGroups.size());
		assertTrue(nxDataGroups.keySet().containsAll(
				expectedDataGroupNames.values()));
		for (String nxDataGroupName : nxDataGroups.keySet()) {
			NXdata nxData = entry.getData(nxDataGroupName);

			String sourceFieldName = nxDataGroupName.equals(detectorName) ? NXdetector.NX_DATA :
				nxDataGroupName.substring(nxDataGroupName.indexOf('_') + 1);
			assertSignal(nxData, sourceFieldName);
			// check the nxData's signal field is a link to the appropriate source data node of the detector
			DataNode dataNode = detector.getDataNode(sourceFieldName);
			IDataset dataset = dataNode.getDataset().getSlice();
			assertSame(dataNode, nxData.getDataNode(sourceFieldName));
			assertTarget(nxData, sourceFieldName, rootNode, "/entry/instrument/" + detectorName
					+ "/" + sourceFieldName);

			// check that the other primary data fields of the detector haven't been added to this NXdata
			for (String primaryDataFieldName : signalFieldAxes.keySet()) {
				if (!primaryDataFieldName.equals(sourceFieldName)) {
					assertNull(nxData.getDataNode(primaryDataFieldName));
				}
			}

			int[] shape = dataset.getShape();

			// Make sure none of the numbers are NaNs. The detector
			// is expected to fill this scan with non-nulls.
			final PositionIterator it = new PositionIterator(shape);
			while (it.hasNext()) {
				int[] next = it.getPos();
				assertFalse(Double.isNaN(dataset.getDouble(next)));
			}

			// Check axes
			List<String> expectedAxesNames = signalFieldAxes.get(sourceFieldName);
			assertAxes(nxData, expectedAxesNames.toArray(new String[expectedAxesNames.size()]));
		}
	}
	
	private IRunnableDevice<ScanModel> createAcquireScan(final IRunnableDevice<?> detector, File file) throws Exception {
		StaticModel emptyModel = new StaticModel();
		IPointGenerator<?> gen = gservice.createGenerator(emptyModel);
//		gen = gservice.createCompoundGenerator(gen); // not required for this test, fails as EmptyGenerator not implemented in Jython
		
		// Create the model for an acquire scan
		ScanModel smodel = new ScanModel();
		smodel.setDetectors(detector);
		
		// Create a file to scan into.
		smodel.setFilePath(file.getAbsolutePath());
		smodel.setPositionIterable(gen);
		System.out.println("File writing to "+smodel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				System.out.println("Running acquire scan for detector " + detector.getName());
			}
		});

		return scanner;
	}

}
