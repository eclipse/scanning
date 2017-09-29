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

import static org.eclipse.scanning.sequencer.nexus.SolsticeConstants.SCANNABLE_NAME_SOLSTICE_SCAN_MONITOR;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertAxes;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertIndices;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertScanNotFinished;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertSolsticeScanGroup;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertTarget;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.nexus.INexusFileFactory;
import org.eclipse.dawnsci.nexus.NXdata;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXroot;
import org.eclipse.dawnsci.nexus.NXslit;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.scanning.api.AbstractScannable;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.AbstractRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.IPosition;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.scannable.MockScannableConfiguration;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.server.application.PseudoSpringParser;
import org.junit.Before;
import org.junit.Test;

public class PerScanMonitorTest extends NexusTest {

	private IScannable<?> perPointMonitor;
	private IScannable<?> perScanMonitor;
	private IScannable<?> stringPerScanMonitor;
	private IScannable<Number> dcs;

    @Before
	public void beforeTest() throws Exception {
		perPointMonitor = connector.getScannable("monitor1");
		perScanMonitor = connector.getScannable("perScanMonitor1");  // Ordinary scannable
		stringPerScanMonitor = connector.getScannable("stringPerScanMonitor");

		// Make a few detectors and models...
		PseudoSpringParser parser = new PseudoSpringParser();
		parser.parse(PerScanMonitorTest.class.getResourceAsStream("test_scannables.xml"));

		// TODO See this scannable which is a MockNeXusSlit
		// Use NexusNodeFactory to create children as correct for http://confluence.diamond.ac.uk/pages/viewpage.action?pageId=37814632
		this.dcs = connector.getScannable("dcs"); // Scannable created by spring with a model.
		dcs.setPosition(10.0);
		((MockScannableConnector) connector).setGlobalPerScanMonitorNames();
	}

	@Test
	public void modelCheck() throws Exception {
		MockScannableConfiguration conf = new MockScannableConfiguration("s1gapX", "s1gapY", "s1cenX", "s1cenY");
		assertEquals(conf, ((AbstractScannable<?>)dcs).getModel());
	}

	@Test
	public void testBasicScanWithPerPointAndPerScanMonitors() throws Exception {
		test(perPointMonitor, perScanMonitor, "perScanMonitor1");
	}

	@Test
	public void testBasicScanWithPerScanMonitor() throws Exception {
		test(null, perScanMonitor, "perScanMonitor1");
	}

	@Test
	public void testBasicScanWithStringPerScanMonitor() throws Exception {
		test(null, stringPerScanMonitor, "stringPerScanMonitor");
	}

	@Test
	public void testBasicScanWithLegacyPerScanMonitor() throws Exception {
		((MockScannableConnector) connector).setGlobalPerScanMonitorNames("perScanMonitor2");
		test(perPointMonitor, perScanMonitor, "perScanMonitor1", "perScanMonitor2");
	}

	@Test
	public void testBasicScanWithLegacyAndPrerequisitePerScanMonitors() throws Exception {
		((MockScannableConnector) connector).setGlobalPerScanMonitorPrerequisiteNames("perScanMonitor1", "perScanMonitor2");
		((MockScannableConnector) connector).setGlobalPerScanMonitorNames("perScanMonitor3");
		((MockScannableConnector) connector).setGlobalPerScanMonitorPrerequisiteNames("perScanMonitor3", "perScanMonitor4", "perScanMonitor5");
		((MockScannableConnector) connector).setGlobalPerScanMonitorPrerequisiteNames("perScanMonitor5", "perScanMonitor6");
		test(perPointMonitor, perScanMonitor, "perScanMonitor1", "perScanMonitor2",
				"perScanMonitor3", "perScanMonitor4", "perScanMonitor5", "perScanMonitor6");
	}

	@Test
	public void testScanWithConfiguredScannable() throws Exception {
		test(perPointMonitor, dcs, "dcs");
	}

	private void test(IScannable<?> monitor, IScannable<?> perScanMonitor,
			String... expectedPerScanMonitorNames) throws Exception {
		int[] shape = new int[] { 8, 5 };
		long before = System.currentTimeMillis();
		// Tell configure detector to write 1 image into a 2D scan
		IRunnableDevice<ScanModel> scanner = createStepScan(monitor, perScanMonitor, shape);
		assertScanNotFinished(getNexusRoot(scanner).getEntry());
		scanner.run(null);
		long after = System.currentTimeMillis();
		System.out.println("Running "+product(shape)+" points took "+(after-before)+" ms");

		checkNexusFile(scanner, shape, expectedPerScanMonitorNames);
	}

	private int product(int[] shape) {
		int total = 1;
		for (int i : shape) total*=i;
		return total;
	}

	private void checkNexusFile(IRunnableDevice<ScanModel> scanner, int[] sizes,
			String[] expectedPerScanMonitorNames) throws Exception {

		final ScanModel scanModel = ((AbstractRunnableDevice<ScanModel>) scanner).getModel();

		NXroot rootNode = getNexusRoot(scanner);
		NXentry entry = rootNode.getEntry();
		NXinstrument instrument = entry.getInstrument();

		// check the scan points have been written correctly
		assertSolsticeScanGroup(entry, false, false, sizes);

		DataNode dataNode = null;
		IDataset dataset = null;
		int[] shape = null;

		// check metadata scannables
		checkPerScanMonitors(scanModel, instrument,
				new HashSet<>(Arrays.asList(expectedPerScanMonitorNames)));

		final IPosition pos = scanModel.getPositionIterable().iterator().next();
		final Collection<String> scannableNames = pos.getNames();

		List<IScannable<?>> perPoint  = scanModel.getMonitors().stream()
				.filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_POINT)
				.filter(scannable -> !scannable.getName().equals(SCANNABLE_NAME_SOLSTICE_SCAN_MONITOR))
				.collect(Collectors.toList());
        final boolean hasMonitor = perPoint != null && !perPoint.isEmpty();

		String dataGroupName = hasMonitor ? perPoint.get(0).getName() : pos.getNames().get(0);
		NXdata nxData = entry.getData(dataGroupName);
		assertNotNull(nxData);

		// Check axes
		String[] expectedAxesNames = scannableNames.stream().map(x -> x + "_value_set").toArray(String[]::new);
		assertAxes(nxData, expectedAxesNames);

		int[] defaultDimensionMappings = IntStream.range(0, sizes.length).toArray();
		int i = -1;
		for (String  scannableName : scannableNames) {

		    i++;

			NXpositioner positioner = instrument.getPositioner(scannableName);
			assertNotNull(positioner);

			dataNode = positioner.getDataNode("value_set");
			dataset = dataNode.getDataset().getSlice();
			shape = dataset.getShape();
			assertEquals(1, shape.length);
			assertEquals(sizes[i], shape[0]);

			String nxDataFieldName = scannableName + "_value_set";
			assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
			assertIndices(nxData, nxDataFieldName, i);
			assertTarget(nxData, nxDataFieldName, rootNode,
					"/entry/instrument/" + scannableName + "/value_set");

			// Actual values should be scanD
			dataNode = positioner.getDataNode(NXpositioner.NX_VALUE);
			dataset = dataNode.getDataset().getSlice();
			shape = dataset.getShape();
			assertArrayEquals(sizes, shape);

			nxDataFieldName = scannableName + "_" + NXpositioner.NX_VALUE;
			assertSame(dataNode, nxData.getDataNode(nxDataFieldName));
			assertIndices(nxData, nxDataFieldName, defaultDimensionMappings);
			assertTarget(nxData, nxDataFieldName, rootNode,
					"/entry/instrument/" + scannableName + "/" + NXpositioner.NX_VALUE);
		}
	}

	private void checkPerScanMonitors(final ScanModel scanModel, NXinstrument instrument,
			Set<String> expectedPerScanMonitorNames) throws Exception {
		DataNode dataNode;
		Dataset dataset;
		Set<String> perScanMonitorNames = scanModel.getMonitors().stream()
				.filter(scannable -> scannable.getMonitorRole()==MonitorRole.PER_SCAN)
				.map(scannable -> scannable.getName()).collect(Collectors.toSet());
		assertEquals(expectedPerScanMonitorNames, perScanMonitorNames);

		for (String perScanMonitorName : perScanMonitorNames) {
			NXpositioner positioner = instrument.getPositioner(perScanMonitorName);
			if (positioner != null) {
				assertEquals(perScanMonitorName, positioner.getNameScalar());

				if (perScanMonitorName.startsWith("string")) {
					String expectedValue = (String) stringPerScanMonitor.getPosition();

				} else {
					int num = Integer.parseInt(perScanMonitorName.substring("perScanMonitor".length()));
					double expectedValue = num * 10.0;

					dataNode = positioner.getDataNode("value_set"); // TODO should not be here for per scan monitor
					assertNotNull(dataNode);
					dataset = DatasetUtils.sliceAndConvertLazyDataset(dataNode.getDataset());
					assertEquals(1, dataset.getSize());
					assertEquals(Dataset.FLOAT64, dataset.getDType());
					assertEquals(expectedValue, dataset.getElementDoubleAbs(0), 1e-15);

					dataNode = positioner.getDataNode(NXpositioner.NX_VALUE);
					assertNotNull(dataNode);
					dataset = DatasetUtils.sliceAndConvertLazyDataset(dataNode.getDataset());
					assertEquals(1, dataset.getSize());
					assertEquals(Dataset.FLOAT64, dataset.getDType());
					assertEquals(expectedValue, dataset.getElementDoubleAbs(0), 1e-15);
				}
			} else {
				NXslit slit = instrument.getChild(perScanMonitorName, NXslit.class);

				assertNotNull(slit);
				assertEquals(perScanMonitorName, slit.getString("name")); // There is no NXslit.getNameScaler() or NXslit.NX_NAME

				double expectedValue = 10.0;

				dataNode = slit.getDataNode(NXslit.NX_X_GAP);
				assertNotNull(dataNode);
				dataset = DatasetUtils.sliceAndConvertLazyDataset(dataNode.getDataset());
				assertEquals(1, dataset.getSize());
				assertEquals(Dataset.FLOAT64, dataset.getDType());
				assertEquals(expectedValue, dataset.getElementDoubleAbs(0), 1e-15);

				dataNode = slit.getDataNode(NXslit.NX_Y_GAP);
				assertNotNull(dataNode);
				dataset = DatasetUtils.sliceAndConvertLazyDataset(dataNode.getDataset());
				assertEquals(1, dataset.getSize());
				assertEquals(Dataset.FLOAT64, dataset.getDType());
				assertEquals(expectedValue, dataset.getElementDoubleAbs(0), 1e-15);
			}
		}
	}

	private IRunnableDevice<ScanModel> createStepScan(IScannable<?> monitor,
			IScannable<?> perScanMonitor, int... size) throws Exception {

		IPointGenerator<?>[] gens = new IPointGenerator<?>[size.length];
		// We add the outer scans, if any
		for (int dim = size.length-1; dim>-1; dim--) {
			final StepModel model;
			if (size[dim]-1>0) {
				model = new StepModel("neXusScannable"+(dim+1), 10,20,9.9d/(size[dim]-1));
			} else {
				model = new StepModel("neXusScannable"+(dim+1), 10,20,30); // Will generate one value at 10
			}
			final IPointGenerator<?> step = gservice.createGenerator(model);
			gens[dim] = step;
		}

		IPointGenerator<?> gen = gservice.createCompoundGenerator(gens);

		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		if (perScanMonitor != null) {
			perScanMonitor.setMonitorRole(MonitorRole.PER_SCAN);
			perScanMonitor.setActivated(true);
		}
		smodel.setMonitors(monitor, perScanMonitor);

		// Create a file to scan into.
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to " + smodel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);

		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException{
				try {
					System.out.println("Running acquisition scan of size "+fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}

	public static INexusFileFactory getFileFactory() {
		return fileFactory;
	}

	public static void setFileFactory(INexusFileFactory fileFactory) {
		PerScanMonitorTest.fileFactory = fileFactory;
	}

}
