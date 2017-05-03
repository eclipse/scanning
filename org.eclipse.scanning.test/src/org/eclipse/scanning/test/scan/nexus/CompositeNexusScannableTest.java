package org.eclipse.scanning.test.scan.nexus;

import static org.eclipse.scanning.test.scan.nexus.NexusAssert.assertScanNotFinished;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.List;

import org.eclipse.dawnsci.nexus.INexusDevice;
import org.eclipse.dawnsci.nexus.NXbeam;
import org.eclipse.dawnsci.nexus.NXcollection;
import org.eclipse.dawnsci.nexus.NXentry;
import org.eclipse.dawnsci.nexus.NXinstrument;
import org.eclipse.dawnsci.nexus.NXobject;
import org.eclipse.dawnsci.nexus.NXpositioner;
import org.eclipse.dawnsci.nexus.NXslit;
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
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.example.scannable.CompositeNexusScannable;
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
			
			return new NexusObjectWrapper<NXbeam>("beam", nxBeam);
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
		CompositeNexusScannable<Object, NXslit> primarySlit = new CompositeNexusScannable<>();
		primarySlit.setName("primary_slit");
		primarySlit.setNexusClass(NexusBaseClass.NX_SLIT);
		primarySlit.setNexusCategory(NexusBaseClass.NX_INSTRUMENT);
		primarySlit.setScannableNames(Arrays.asList(beam.getName()));
		((MockScannableConnector) connector).register(primarySlit);
		
		NXentry entry = createAndRunScan(primarySlit);
		assertEquals(7, entry.getNumberOfGroupNodes());
		NXinstrument instrument = entry.getInstrument();
		assertEquals(4, instrument.getNumberOfGroupNodes());
		
		NXslit nxSlit = (NXslit) instrument.getGroupNode("primary_slit");
		assertNotNull(nxSlit);
		assertSame(NexusBaseClass.NX_SLIT, nxSlit.getNexusBaseClass());
		assertEquals(1, nxSlit.getNumberOfGroupNodes());
		assertEquals(0, nxSlit.getNumberOfDataNodes());
		
		NXbeam beam = (NXbeam) nxSlit.getGroupNode("beam");
		assertNotNull(beam);
		
		assertEquals(0.123, beam.getIncident_beam_divergenceScalar(), 1e-15);
		assertEquals(0.456, beam.getFinal_beam_divergenceScalar(), 1e-15);
	}
	
	@Test
	public void testCompositeNexusScannable() throws Exception {
		List<String> scannableNames = Arrays.asList("neXusScannable1", "neXusScannable2", "neXusScannable3");
		
		CompositeNexusScannable<Object, NXobject> composite = new CompositeNexusScannable<>();
		composite.setName("composite");
		composite.setNexusClass(NexusBaseClass.NX_COLLECTION);
		composite.setScannableNames(scannableNames);
		((MockScannableConnector) connector).register(composite);
		
		NXentry entry = createAndRunScan(composite);
		assertEquals(8, entry.getNumberOfGroupNodes());
		NXinstrument instrument = entry.getInstrument();
		assertEquals(3, instrument.getNumberOfGroupNodes());
		
		NXcollection compositeCollection = (NXcollection) entry.getGroupNode("composite");
		assertNotNull(compositeCollection);
		assertSame(NexusBaseClass.NX_COLLECTION, compositeCollection.getNexusBaseClass());
		assertEquals(scannableNames.size(), compositeCollection.getNumberOfGroupNodes());
		
		for (String scannableName : scannableNames) {
			NXpositioner positioner = (NXpositioner) compositeCollection.getGroupNode(scannableName);
			assertNotNull(positioner);
			assertEquals(scannableName, positioner.getNameScalar());
			assertNotNull(positioner.getValue());
		}
	}
	
	private NXentry createAndRunScan(CompositeNexusScannable<?, ?> compositeScannable) throws Exception {
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
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		gmodel.setSnake(false);
		
		IPointGenerator<?> gen = gservice.createGenerator(gmodel);
		
		// Create the model for a scan.
		final ScanModel  smodel = new ScanModel();
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

}
