package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.eclipse.scanning.api.annotation.scan.FileDeclared;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.StaticModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.junit.Test;

/**
 * Test the use of Malcolm to acquire data without moving any scannables.<br>
 * This would be used for software scans using detectors that can only acquire via Malcolm.
 *
 */
public class MalcolmStaticScanTest extends AbstractMalcolmScanTest {

	@Test
	public void test0d() throws Exception {
		runMalcolmScan();
	}

	@Test
	public void test1d() throws Exception {
		runMalcolmScan(5);
	}

	@Test
	public void test3d() throws Exception {
		runMalcolmScan(5, 3, 2);
	}

	@Override
	protected DummyMalcolmModel createMalcolmModel() {
		final DummyMalcolmModel model = createMalcolmModelTwoDetectors();
		model.setAxesToMove(Collections.emptyList());
		return model;
	}

	private void runMalcolmScan(int... size) throws Exception {
		final IRunnableDevice<ScanModel> scanner = createScanner(size);
		scanner.run();

		checkSize(scanner, size);
		checkFiles();
		checkNexusFile(scanner, false, size);
		assertEquals(DeviceState.ARMED, scanner.getDeviceState());
	}

	// Create a scan where the Malcolm device just does a static scan, but there may be outer scannables
	private IRunnableDevice<ScanModel> createScanner(int... size) throws Exception {
		final IPointGenerator<?> pointGenerator;
		if (size.length == 0) {
			// Static generator for the Malcolm device
			pointGenerator = gservice.createGenerator(new StaticModel());
		} else {
			// Step scan generators for any other dimensions
			final IPointGenerator<?>[] stepGenerators = new IPointGenerator<?>[size.length];
			if (size.length > 0) {
				for (int dim = size.length - 1; dim >= 0; dim--) {
					final StepModel model;
					if (size[dim] - 1 > 0) {
						model = new StepModel("neXusScannable" + (dim + 1), 10, 20, 9.99d / (size[dim] - 1));
					} else {
						model = new StepModel("neXusScannable" + (dim + 1), 10, 20, 30);
					}
					final IPointGenerator<?> stepGenerator = gservice.createGenerator(model);
					stepGenerators[dim] = stepGenerator;
				}
			}
			pointGenerator = gservice.createCompoundGenerator(stepGenerators);
		}

		// Create the model for a scan.
		final ScanModel scanModel = new ScanModel();
		scanModel.setPositionIterable(pointGenerator);
		scanModel.setDetectors(malcolmDevice);
		// Cannot set the generator from @PreConfigure in this unit test.
		((AbstractMalcolmDevice<?>) malcolmDevice).setPointGenerator(pointGenerator);

		// Create a file to scan into.
		scanModel.setFilePath(output.getAbsolutePath());
		System.out.println("File writing to " + scanModel.getFilePath());

		// Create a scan and run it without publishing events
		final IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(scanModel, null);
		((IRunnableEventDevice<ScanModel>) scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				try {
					System.out.println("Running acquisition size of scan " + pointGenerator.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});

		return scanner;
	}

	private void checkFiles() {
		assertEquals(3, participant.getCount(FileDeclared.class));
		final List<String> paths = participant.getPaths();
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("detector.h5")));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("detector2.h5")));
	}
}
