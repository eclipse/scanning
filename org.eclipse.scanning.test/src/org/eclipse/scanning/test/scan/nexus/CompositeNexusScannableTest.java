package org.eclipse.scanning.test.scan.nexus;

import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertDatasetValue;
import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertScanNotFinished;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.dawnsci.analysis.api.tree.DataNode;
import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXbeam;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXslit;
import org.eclipse.dawnsci.nexus.NXtransformations;
import org.eclipse.dawnsci.nexus.NexusBaseClass;
import org.eclipse.dawnsci.nexus.NexusException;
import org.eclipse.dawnsci.nexus.NexusNodeFactory;
import org.eclipse.dawnsci.nexus.NexusScanInfo;
import org.eclipse.dawnsci.nexus.builder.NexusObjectProvider;
import org.eclipse.dawnsci.nexus.builder.NexusObjectWrapper;
import org.eclipse.scanning.api.IScannable;
import org.eclipse.scanning.api.MonitorRole;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.device.IWritableDetector;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.device.composite.ChildFieldNode;
import org.eclipse.scanning.device.composite.ChildGroupNode;
import org.eclipse.scanning.device.composite.ChildNode;
import org.eclipse.scanning.device.composite.CompositeNexusScannable;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.scannable.MockNeXusScannable;
import org.eclipse.scanning.example.scannable.MockScannable;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompositeNexusScannableTest extends NexusTest {

	private static final class BeamPerScanMonitor extends MockScannable implements INexusDevice<NXbeam> {

		BeamPerScanMonitor() {
			setName("beam");
		}

		@Override
		public NexusObjectProvider<NXbeam> getNexusProvider(NexusScanInfo info) throws NexusException {
			final NXbeam nxBeam = NexusNodeFactory.createNXbeam();

			nxBeam.setIncident_beam_divergenceScalar(0.123);
			nxBeam.setFinal_beam_divergenceScalar(0.456);

			return new NexusObjectWrapper<NXbeam>(getName(), nxBeam);
		}

	}

	private static final class TransformsScannable extends MockScannable implements INexusDevice<NXtransformations> {

		TransformsScannable() {
			setName("transforms");
		}

		@Override
		public NexusObjectProvider<NXtransformations> getNexusProvider(NexusScanInfo info) throws NexusException {
			final NXtransformations nxTransformations = NexusNodeFactory.createNXtransformations();

			// TODO: should this be a CompositeNexusScannable also?
			nxTransformations.setField("x_centre", 123.456);
			nxTransformations.setAttribute("x_centre", "transformation_type", "translation");
			nxTransformations.setAttribute("x_centre", "vector", new int[] { 1, 0, 0 });
			nxTransformations.setAttribute("x_centre", "depends_on", "y_centre");
			nxTransformations.setAttribute("x_centre", "units", "mm");
			nxTransformations.setAttribute("x_centre", "controller_record", "x centre controller name");

			nxTransformations.setField("y_centre", 789.012);
			nxTransformations.setAttribute("y_centre", "transformation_type", "translation");
			nxTransformations.setAttribute("y_centre", "vector", new int[] { 0, 1, 0 });
			nxTransformations.setAttribute("y_centre", "depends_on", ".");
			nxTransformations.setAttribute("y_centre", "offset", new int[] { 0, 0, -14500 });
			nxTransformations.setAttribute("y_centre", "units", "mm");
			nxTransformations.setAttribute("y_centre", "controller_record", "y centre controller name");

			return new NexusObjectWrapper<NXtransformations>(getName(), nxTransformations);
		}

	}

	private static final class SlitMotorScannable extends MockScannable implements INexusDevice<NXpositioner> {

		private final String nexusGroupName;
		private final String description;

		SlitMotorScannable(String name, double position, String nexusGroupName, String description) {
			super(name, position);
			this.nexusGroupName = nexusGroupName;
			this.description = description;
		}

		public String getNexusGroupName() {
			return nexusGroupName;
		}

		public String getDescription() {
			return description;
		}

		@Override
		public NexusObjectProvider<NXpositioner> getNexusProvider(NexusScanInfo info) throws NexusException {
			final NXpositioner nxPositioner = NexusNodeFactory.createNXpositioner();
			nxPositioner.setNameScalar(getName());
			nxPositioner.setDescriptionScalar(description);
			nxPositioner.setController_recordScalar(getName() + "_controller"); // this would be the EPICS name

			// Write the value. Note: we would initialize a lazy dataset if the scannable could be scanned
			nxPositioner.setValueScalar(getPosition());

			return new NexusObjectWrapper<NXpositioner>(getName(), nxPositioner);
		}

	}

	private static IWritableDetector<?> detector;
	private static BeamPerScanMonitor beam;

	private static final int[] SCAN_SIZE = { 2, 3 };

	@BeforeClass
	public static void beforeClass() throws Exception {
		MandelbrotModel model = createMandelbrotModel();
		detector = (IWritableDetector<?>) dservice.createRunnableDevice(model);
		assertNotNull(detector);

		beam = new BeamPerScanMonitor();
		((MockScannableConnector) connector).register(beam);
	}

	@Test
	public void testNXSlitCompositeNexusScannable() throws Exception {
		// Arrange
		CompositeNexusScannable<NXslit> primarySlit = new CompositeNexusScannable<>();
		primarySlit.setName("primary_slit");
		primarySlit.setNexusClass(NexusBaseClass.NX_SLIT);
		primarySlit.setNexusCategory(NexusBaseClass.NX_INSTRUMENT);

		List<ChildNode> childNodes = new ArrayList<>();

		// create the nodes for x_gap and y_gap
		final String gapXName = "s1gapX"; // TODO Move to @Before method?
		IScannable<?> s1gapX = new MockNeXusScannable(gapXName, 1.234, 1);
		((MockScannableConnector) connector).register(s1gapX);
		final String gapYName = "s1gapY";
		IScannable<?> s1gapY = new MockNeXusScannable(gapYName, 2.345, 1);
		((MockScannableConnector) connector).register(s1gapY);

		childNodes.add(new ChildFieldNode(gapXName, NXpositioner.NX_VALUE, "x_gap"));
		childNodes.add(new ChildFieldNode(gapYName, NXpositioner.NX_VALUE, "y_gap"));

		// create the transformations scannable
		// TODO: should we add a mechanism within ComplexCompositeNexusScannable
		// rather than
		// create a new scannable for this?
		TransformsScannable transformsScannable = new TransformsScannable();
		transformsScannable.setName("transforms");
		((MockScannableConnector) connector).register(transformsScannable);

		ChildGroupNode transformsNode = new ChildGroupNode();
		transformsNode.setScannableName(transformsScannable.getName());
		childNodes.add(transformsNode);

		// create the beam
		BeamPerScanMonitor beamScannable = new BeamPerScanMonitor();
		((MockScannableConnector) connector).register(beamScannable);

		ChildGroupNode beamNode = new ChildGroupNode();
		beamNode.setScannableName(beamScannable.getName());
		beamNode.setGroupName("beam");
		childNodes.add(beamNode);

		// create the motors composite scannable
		CompositeNexusScannable<NXcollection> motorsCompositeScannable = new CompositeNexusScannable<>();
		motorsCompositeScannable.setName("primary_slit_motors");
		((MockScannableConnector) connector).register(motorsCompositeScannable);

		List<SlitMotorScannable> slitMotorScannables = new ArrayList<>();
		slitMotorScannables.add(new SlitMotorScannable("s1dsX", 1.0, "downstream_x", "Downstream X position"));
		slitMotorScannables.add(new SlitMotorScannable("s1dsY", 2.0, "downstream_y", "Downstream Y position"));
		slitMotorScannables.add(new SlitMotorScannable("s1usX", 3.0, "upstream_x", "Upstream X position"));
		slitMotorScannables.add(new SlitMotorScannable("s1usY", 4.0, "upstream_y", "Upstream Y position"));
		slitMotorScannables.forEach(scannable -> ((MockScannableConnector) connector).register(scannable));
		motorsCompositeScannable.setChildNodes(slitMotorScannables.stream().
				map(scannable -> new ChildGroupNode(scannable.getName(), scannable.getNexusGroupName())).
				collect(Collectors.toList()));

		ChildGroupNode motorsNode = new ChildGroupNode();
		motorsNode.setScannableName("primary_slit_motors");
		motorsNode.setGroupName("motors");
		childNodes.add(motorsNode);

		// add the groups to the child primary slit and register the primary
		// slit scannable
		primarySlit.setChildNodes(childNodes);
		((MockScannableConnector) connector).register(primarySlit);

		// Act: run the scan
		NXentry entry = createAndRunScan(primarySlit);

		// Assert: check the nexus file
		assertEquals(7, entry.getNumberOfGroupNodes());
		NXinstrument instrument = entry.getInstrument();
		assertEquals(4, instrument.getNumberOfGroupNodes());

		NXslit nxSlit = (NXslit) instrument.getGroupNode("primary_slit");
		assertNotNull(nxSlit);
		assertSame(NexusBaseClass.NX_SLIT, nxSlit.getNexusBaseClass());
		assertEquals(3, nxSlit.getNumberOfGroupNodes());
		assertEquals(2, nxSlit.getNumberOfDataNodes());

		// assertEquals(1.234, nxSlit.getX_gapScalar().doubleValue(), 1e-15);
		// TODO reinstate when DAQ-599 fixed
		assertDatasetValue(1.234, nxSlit.getDataset("x_gap"));
		// assertEquals(2.345, nxSlit.getY_gapScalar().doubleValue(), 1e-15);
		assertDatasetValue(2.345, nxSlit.getDataset("y_gap"));

		NXtransformations transforms = (NXtransformations) nxSlit.getGroupNode("transforms");
		assertDatasetValue(123.456, transforms.getDataset("x_centre"));
		assertDatasetValue("translation", transforms.getAttr("x_centre", "transformation_type"));
		assertDatasetValue(new int[] { 1, 0, 0 }, transforms.getAttr("x_centre", "vector"));
		assertDatasetValue("y_centre", transforms.getAttr("x_centre", "depends_on"));
		assertDatasetValue("mm", transforms.getAttr("x_centre", "units"));
		assertDatasetValue("x centre controller name", transforms.getAttr("x_centre", "controller_record"));

		assertDatasetValue(789.012, transforms.getDataset("y_centre"));
		assertDatasetValue("translation", transforms.getAttr("y_centre", "transformation_type"));
		assertDatasetValue(new int[] { 0, 1, 0 }, transforms.getAttr("y_centre", "vector"));
		assertDatasetValue(".", transforms.getAttr("y_centre", "depends_on"));
		assertDatasetValue(new int[] { 0, 0, -14500 }, transforms.getAttr("y_centre", "offset"));
		assertDatasetValue("mm", transforms.getAttr("y_centre", "units"));
		assertDatasetValue("y centre controller name", transforms.getAttr("y_centre", "controller_record"));

		NXbeam beam = (NXbeam) nxSlit.getGroupNode("beam");
		assertNotNull(beam);

		assertEquals(0.123, beam.getIncident_beam_divergenceScalar(), 1e-15);
		assertEquals(0.456, beam.getFinal_beam_divergenceScalar(), 1e-15);

		NXcollection motors = (NXcollection) nxSlit.getGroupNode("motors");
		assertNotNull(motors);
		assertEquals(0, motors.getNumberOfDataNodes());
		assertEquals(4, motors.getNumberOfGroupNodes());
		for (SlitMotorScannable slitMotorScannable : slitMotorScannables) {
			NXpositioner positioner = (NXpositioner) motors.getGroupNode(slitMotorScannable.getNexusGroupName());
			assertNotNull(positioner);

			assertEquals(slitMotorScannable.getName(), positioner.getNameScalar());
			assertEquals(slitMotorScannable.getDescription(), positioner.getDescriptionScalar());
			assertEquals(slitMotorScannable.getName() + "_controller", positioner.getController_recordScalar());
			assertEquals(slitMotorScannable.getPosition(), positioner.getValueScalar());
		}
	}

	@Test
	public void testCompositeNexusScannable() throws Exception {
		// Create a list of 3 ChildFieldNodes and 5 ChildGroupNodes
		final int numFieldNodes = 3;
		final int numGroupNodes = 5;
		final int numTotalNodes = numFieldNodes + numGroupNodes;
		IntFunction<ChildNode> toNode = i -> i <= numFieldNodes
				? new ChildFieldNode("neXusScannable" + i, NXpositioner.NX_VALUE, "pos" + i)
				: new ChildGroupNode("neXusScannable" + i);
		List<ChildNode> childNodes = IntStream.range(1, numTotalNodes + 1).mapToObj(toNode)
				.collect(Collectors.toList());

		CompositeNexusScannable<NXobject> composite = new CompositeNexusScannable<>();
		composite.setName("composite");
		composite.setNexusClass(NexusBaseClass.NX_COLLECTION);
		composite.setChildNodes(childNodes);
		((MockScannableConnector) connector).register(composite);

		NXentry entry = createAndRunScan(composite);
		assertEquals(8, entry.getNumberOfGroupNodes()); // NXinstrument, NXdata groups, etc
		NXinstrument instrument = entry.getInstrument();
		assertEquals(3, instrument.getNumberOfGroupNodes()); // mandelbrot, xNex, yNex

		NXcollection compositeCollection = (NXcollection) entry.getGroupNode("composite");
		assertNotNull(compositeCollection);
		assertSame(NexusBaseClass.NX_COLLECTION, compositeCollection.getNexusBaseClass());
		assertEquals(numFieldNodes, compositeCollection.getNumberOfDataNodes());
		assertEquals(numGroupNodes, compositeCollection.getNumberOfGroupNodes());

		for (ChildNode childNode : childNodes) {
			final String scannableName = childNode.getScannableName();
			if (childNode instanceof ChildGroupNode) {
				// check scannables added as groups
				NXpositioner positioner = (NXpositioner) compositeCollection.getGroupNode(scannableName);
				assertNotNull(positioner);
				assertEquals(scannableName, positioner.getNameScalar());
				assertNotNull(positioner.getValue());
			} else if (childNode instanceof ChildFieldNode) {
				// check scannables added as fields
				DataNode fieldNode = compositeCollection
						.getDataNode(((ChildFieldNode) childNode).getDestinationFieldName());
				assertNotNull(fieldNode);
				assertArrayEquals(new int[0], fieldNode.getDataset().getSlice().getShape());
			}
		}
	}

	private NXentry createAndRunScan(CompositeNexusScannable<?> compositeScannable) throws Exception {
		IRunnableDevice<ScanModel> scanner = createGridScan(compositeScannable);
		assertScanNotFinished(getNexusRoot(scanner).getEntry());
		scanner.run(null);

		return checkNexusFile(scanner, false, SCAN_SIZE);
	}

	private IRunnableDevice<ScanModel> createGridScan(IScannable<?> perScanMonitor) throws Exception {
		GridModel gmodel = new GridModel();
		gmodel.setFastAxisName("xNex");
		gmodel.setFastAxisPoints(SCAN_SIZE[1]);
		gmodel.setSlowAxisName("yNex");
		gmodel.setSlowAxisPoints(SCAN_SIZE[0]);
		gmodel.setBoundingBox(new BoundingBox(0, 0, 3, 3));
		gmodel.setSnake(false);

		IPointGenerator<?> gen = gservice.createGenerator(gmodel);

		// Create the model for a scan.
		final ScanModel smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		if (perScanMonitor != null) {
			perScanMonitor.setMonitorRole(MonitorRole.PER_SCAN);
			perScanMonitor.setActivated(true);
		}
		smodel.setDetectors(detector);

		smodel.setMonitors(perScanMonitor);

		// Create a file to scan into.
		smodel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to " + smodel.getFilePath());

		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);

		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>) scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				try {
					System.out.println("Running acquisition scan of size " + fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}

}
