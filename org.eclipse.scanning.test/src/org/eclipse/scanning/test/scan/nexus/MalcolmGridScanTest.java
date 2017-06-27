package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.scanning.api.annotation.scan.FileDeclared;
import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.device.IRunnableEventDevice;
import org.eclipse.scanning.api.event.scan.DeviceState;
import org.eclipse.scanning.api.points.GeneratorException;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.BoundingBox;
import org.eclipse.scanning.api.points.models.GridModel;
import org.eclipse.scanning.api.points.models.StepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.event.IRunListener;
import org.eclipse.scanning.api.scan.event.RunEvent;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.malcolm.DummyMalcolmModel;
import org.eclipse.scanning.malcolm.core.AbstractMalcolmDevice;
import org.junit.Test;

public class MalcolmGridScanTest extends AbstractMalcolmScanTest {
	
	@Test
	public void test2DMalcolmScan() throws Exception {
		testMalcolmScan(false, 8, 5);
	}
	
	@Test
	public void test2DMalcolmSnakeScan() throws Exception {
		testMalcolmScan(true, 8, 5);
	}
	
	@Test
	public void test2DMalcolmSnakeWithOddNumberOfLinesScan() throws Exception {
		testMalcolmScan(true, 7, 5);
	}
	
	@Test
	public void test3DMalcolmScan() throws Exception {
		testMalcolmScan(false, 3, 2, 5);
	}
	
	@Test
	public void test3DMalcolmSnakeScan() throws Exception {
		testMalcolmScan(true, 3, 2, 5);
	}
	
	@Test
	public void test4DMalcolmScan() throws Exception {
		testMalcolmScan(false,3,3,2, 2);
	}
	
	@Test
	public void test5DMalcolmScan() throws Exception {
		testMalcolmScan(false,1,1,1,2, 2);
	}
	
	@Test
	public void test8DMalcolmScan() throws Exception {
		testMalcolmScan(false,1,1,1,1,1,1,2, 2);
	}

	@Override
	protected DummyMalcolmModel createMalcolmModel() {
		final DummyMalcolmModel model = createMalcolmModelTwoDetectors();
		model.setAxesToMove(Arrays.asList("stage_x", "stage_y" ));
		model.setPositionerNames(Arrays.asList("stage_x", "j1", "j2", "j3"));
		model.setMonitorNames(Arrays.asList("i0"));

		return model;
	}
	
	private void testMalcolmScan(boolean snake, int... shape) throws Exception {
		IRunnableDevice<ScanModel> scanner = createMalcolmGridScan(malcolmDevice, output, snake, shape); // Outer scan of another scannable, for instance temp.
		scanner.run(null);
		
		checkSize(scanner, shape);
		checkFiles();

		// Check we reached armed (it will normally throw an exception on error)
		assertEquals(DeviceState.ARMED, scanner.getDeviceState());
		checkNexusFile(scanner, snake, shape); // Step model is +1 on the size
	}

	private IRunnableDevice<ScanModel> createMalcolmGridScan(final IRunnableDevice<?> malcolmDevice, File file, boolean snake, int... size) throws Exception {
		
		// Create scan points for a grid and make a generator
		GridModel gmodel = new GridModel(); // Note stage_x and stage_y scannables controlled by malcolm
		gmodel.setFastAxisName("stage_x");
		gmodel.setFastAxisPoints(size[size.length-1]);
		gmodel.setSlowAxisName("stage_y");
		gmodel.setSlowAxisPoints(size[size.length-2]);
		gmodel.setBoundingBox(new BoundingBox(0,0,3,3));
		gmodel.setSnake(snake);
		
		IPointGenerator<?> gen = gservice.createGenerator(gmodel);
		
		IPointGenerator<?>[] gens = new IPointGenerator<?>[size.length - 1];
		if (size.length > 2) {
			for (int dim = size.length - 3; dim > -1; dim--) {
				final StepModel model;
				if (size[dim]-1>0) {
					model = new StepModel("neXusScannable"+(dim+1), 10,20,9.99d/(size[dim]-1));
				} else {
					model = new StepModel("neXusScannable"+(dim+1), 10,20,30);
				}
				final IPointGenerator<?> step = gservice.createGenerator(model);
				gens[dim] = step;
			}
		}
		gens[size.length - 2] = gen;
		
		gen = gservice.createCompoundGenerator(gens);
		
		// Create the model for a scan.
		final ScanModel smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(malcolmDevice);
		// Cannot set the generator from @PreConfigure in this unit test.
		((AbstractMalcolmDevice<?>)malcolmDevice).setPointGenerator(gen);
		
		// Create a file to scan into.
		smodel.setFilePath(file.getAbsolutePath());
		System.out.println("File writing to " + smodel.getFilePath());
		
		// Create a scan and run it without publishing events
		IRunnableDevice<ScanModel> scanner = dservice.createRunnableDevice(smodel, null);
		
		final IPointGenerator<?> fgen = gen;
		((IRunnableEventDevice<ScanModel>)scanner).addRunListener(new IRunListener() {
			@Override
			public void runWillPerform(RunEvent evt) throws ScanningException {
				try {
					System.out.println("Running acquisition size of scan "+fgen.size());
				} catch (GeneratorException e) {
					throw new ScanningException(e);
				}
			}
		});
		
		return scanner;
	}
	
	private void checkFiles() {
		assertEquals(4, participant.getCount(FileDeclared.class));
		List<String> paths = participant.getPaths();
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("detector.h5")));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("detector2.h5")));
		assertTrue(paths.stream().anyMatch(path -> path.endsWith("panda.h5")));
	}
}
