package org.eclipse.scanning.test.command;

import static org.junit.Assert.assertEquals;

import org.eclipse.scanning.command.Services;
import org.junit.Test;

public class MScanTest extends AbstractScanCommandsTest {


	public MScanTest() {
		super(false);
	}

	@Test
	public void testGridScan() {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True), det=detector('mandelbrot', 0.001))");
	}

	@Test
	public void testGridScanNoDetector() {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True))");
	}

	@Test
	public void testStepAroundGridScanNoDetector() {
		pi.exec("mscan([step(axis='energy', start=300, stop=310, step=5), grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True)])");
	}

	@Test
	public void testMultiStep() {
		pi.exec("mscan(mstep(axis='energy', stepModels=[StepModel('energy', 300, 310, 5)]))");
	}

	@Test
	public void testMulti1StepAroundGridScanNoDetector() {
		pi.exec("mscan([mstep(axis='energy', stepModels=[StepModel('energy', 300, 310, 5)]), grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True)])");
	}

	@Test
	public void testMulti2StepAroundGridScanNoDetector() {
		pi.exec("mscan([mstep(axis='energy', stepModels=[StepModel('energy', 290, 300, 5), StepModel('energy', 300, 310, 5)]), grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True)])");
	}

	@Test
	public void testGridWithROIScan() {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0.0, 1.0), stop=(10.0, 12.0), count=(3, 2), snake=False, roi=[circ(origin=(0.0, 1.0), radius=2.0)]), det=detector('mandelbrot', 0.001))");
	}

	@Test
	public void testGridScanWithGoodTimeout() {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True), det=detector('mandelbrot', 1.2, timeout=2))");
	}

	@Test
	public void testI15_1Case() {
		pi.exec("mscan(path=[grid(axes=('stage_x', 'stage_y'), start=(-1.5, -1.0), stop=(0.5, 1.0), count=(2, 2), snake=False)], monitorsPerScan=['s1MockNeXusSlit'], det=[detector('mandelbrot', 0.001, maxIterations=500, escapeRadius=10.0, columns=301, rows=241, points=100, maxRealCoordinate=1.5, maxImaginaryCoordinate=1.2, realAxisName='stage_x', imaginaryAxisName='stage_y', enableNoise=False, noiseFreeExposureTime=5.0, saveImage=True, saveSpectrum=True, saveValue=True)])");
	}

	@Test
	public void testStepWithTimeout() throws Exception {
		assertEquals(-1, Services.getScannableDeviceService().getScannable("stage_x").getTimeout());
		pi.exec("mscan(step(axis='stage_x', start=300, stop=310, step=1, timeout=2))");
		assertEquals(2, Services.getScannableDeviceService().getScannable("stage_x").getTimeout());
		Services.getScannableDeviceService().getScannable("stage_x").setTimeout(-1);
	}

	@Test
	public void testGridWithTimeout() throws Exception {
		assertEquals(-1, Services.getScannableDeviceService().getScannable("xNex").getTimeout());
		assertEquals(-1, Services.getScannableDeviceService().getScannable("yNex").getTimeout());
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True, timeout=1), det=detector('mandelbrot', 0.001))");
		assertEquals(1, Services.getScannableDeviceService().getScannable("xNex").getTimeout());
		assertEquals(1, Services.getScannableDeviceService().getScannable("yNex").getTimeout());
		Services.getScannableDeviceService().getScannable("xNex").setTimeout(-1);
		Services.getScannableDeviceService().getScannable("yNex").setTimeout(-1);
	}

}
