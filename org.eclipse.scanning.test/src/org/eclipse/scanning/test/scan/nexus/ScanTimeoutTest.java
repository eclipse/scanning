package org.eclipse.scanning.test.scan.nexus;

import static org.junit.Assert.assertNotNull;

import org.eclipse.scanning.api.device.IRunnableDevice;
import org.eclipse.scanning.api.scan.ScanningException;
import org.eclipse.scanning.api.scan.models.ScanModel;
import org.eclipse.scanning.example.detector.RandomLineDevice;
import org.eclipse.scanning.example.detector.RandomLineModel;
import org.junit.Before;
import org.junit.Test;

public class ScanTimeoutTest  extends NexusTest {

	private IRunnableDevice<?> linedetector;
	
	@Before
	public void before() throws ScanningException {
				
		RandomLineModel rmodel = new RandomLineModel();
		rmodel.setTimeout(1); // second
		linedetector = dservice.createRunnableDevice(rmodel);
		assertNotNull(linedetector);
				
		((RandomLineDevice)linedetector).setWriteSleep(2); // Sleeps for 2 seconds.
	}

	@Test(expected=ScanningException.class) 
	public void testLineTimeoutThrowsException() throws Exception {
		
		// All scannables should have their name set ok
		IRunnableDevice<ScanModel> scanner = createScanner(linedetector,  2, 2);
		scanner.run(null);
	}
	
	private IRunnableDevice<ScanModel> createScanner(IRunnableDevice<?> device, int... shape) throws Exception {
		
		ScanModel smodel = createGridScanModel(device, output, true, shape);
		return dservice.createRunnableDevice(smodel, null);
	}

}
