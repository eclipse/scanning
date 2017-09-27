package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.points.IPointGenerator;
import org.eclipse.scanning.api.points.models.MultiStepModel;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.RandomLineDevice;
import org.eclipse.scanning.example.detector.RandomLineModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScanTimeoutTest  extends NexusTest {

	private RandomLineDevice linedetector;
	
	@Before
	public void before() throws ScanningException {
		final RandomLineModel rlModel = new RandomLineModel();
		rlModel.setTimeout(10);
		linedetector = (RandomLineDevice)dservice.createRunnableDevice(rlModel);
		assertNotNull(linedetector);		
	}
	
	@After
	public void after() throws Exception {
		linedetector.reset();
	}

	@Test
	public void testLine() throws Exception {

		IRunnableDevice<ScanModel> scanner = createScanner(linedetector,  2, 2);
		scanner.run(null);
		
		assertEquals(1, linedetector.getCount("configure"));
		assertEquals(4, linedetector.getCount("run"));
		assertEquals(4, linedetector.getCount("write"));
	}
	
	@Test(expected=ScanningException.class) 
	public void testLineTimeoutThrowsException() throws Exception {
		
		try {
			linedetector.getModel().setExposureTime(2); // Sleeps for 2 seconds.
			linedetector.getModel().setTimeout(1);      // Only 1 second allowed.
	
			// All scannables should have their name set ok
			IRunnableDevice<ScanModel> scanner = createScanner(linedetector,  2, 2);
			scanner.run(null);
			
		} finally {
			linedetector.getModel().setExposureTime(0); 
			linedetector.getModel().setTimeout(-1); 
		}
	}
	
	@Test(expected=ScanningException.class) 
	public void testLineThrowsWriteException() throws Exception {
		
		try {
			linedetector.setThrowWriteExceptions(true);
	
			// All scannables should have their name set ok
			IRunnableDevice<ScanModel> scanner = createScanner(linedetector,  2, 2);
			scanner.run(null);
			
		} finally {
			linedetector.setThrowWriteExceptions(false);
		}
	}

	
	@Test
	public void testMultiStep() throws Exception {

		IRunnableDevice<ScanModel> scanner = createMultiStepScanner(linedetector);
		long before = System.currentTimeMillis();
		scanner.run();
		long after = System.currentTimeMillis();
		long time  = after-before;
		assertTrue("The time to run the scan must be less than 2000 but it was "+time+"ms", time<2000);
	
		assertEquals(4, linedetector.getCount("configure"));
		assertEquals(0.001,  ((RandomLineModel)linedetector.getValue("configure", 0)).getExposureTime(), 0.000001);
		assertEquals(0.0015, ((RandomLineModel)linedetector.getValue("configure", 1)).getExposureTime(), 0.000001);
		assertEquals(0.002,  ((RandomLineModel)linedetector.getValue("configure", 2)).getExposureTime(), 0.000001);
		assertEquals(0.003,  ((RandomLineModel)linedetector.getValue("configure", 3)).getExposureTime(), 0.000001);
		
		assertEquals(21, linedetector.getCount("run"));
		assertEquals(21, linedetector.getCount("write"));
	}

	
	private IRunnableDevice<ScanModel> createScanner(IRunnableDevice<?> device, int... shape) throws Exception {
		
		ScanModel smodel = createGridScanModel(device, output, true, shape);
		return dservice.createRunnableDevice(smodel, null);
	}

	private IRunnableDevice<ScanModel> createMultiStepScanner(IRunnableDevice<?> device) throws Exception {
		
		MultiStepModel model = new MultiStepModel();
		model.setName("x");
		model.addRange(10, 20, 2,    0.0015); // Te = 0.0015
		model.addRange(25, 50, 5,    0.002);  // Te = 0.002
		model.addRange(100, 500, 50, 0.003);  // Te = 0.003
		
		IPointGenerator<?> gen = gservice.createGenerator(model);
		assertEquals(21, gen.size());

		ScanModel smodel = new ScanModel();
		smodel.setPositionIterable(gen);
		smodel.setDetectors(device);
		smodel.setFilePath(output.getCanonicalPath());
		
		return dservice.createRunnableDevice(smodel, null);
	}

}
