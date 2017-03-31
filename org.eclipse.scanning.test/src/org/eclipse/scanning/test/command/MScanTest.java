package org.eclipse.scanning.test.command;

import org.junit.Test;

public class MScanTest extends AbstractScanCommandsTest {


	public MScanTest() {
		super(false);
	}

	@Test
	public void testGridScan() throws Exception {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True), det=detector('mandelbrot', 0.001))");
	}
		
	@Test
	public void testGridScanNoDetector() throws Exception {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True))");
	}
	
	@Test
	public void testGridWithROIScan() throws Exception {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0.0, 1.0), stop=(10.0, 12.0), count=(3, 2), snake=False, roi=[circ(origin=(0.0, 1.0), radius=2.0)]), det=detector('mandelbrot', 0.001))");
	}
		
	@Test
	public void testGridScanWithGoodTimeout() throws Exception {
		pi.exec("mscan(grid(axes=('xNex', 'yNex'), start=(0, 0), stop=(10, 10), count=(2, 2), snake=True), det=detector('mandelbrot', 1.2, timeout=2))");
	}

	@Test
	public void testI15_1Case() throws Exception {
		pi.exec("mscan(path=[grid(axes=('stage_x', 'stage_y'), start=(-1.5, -1.0), stop=(0.5, 1.0), count=(2, 2), snake=False)], mon=['s1MockNeXusSlit'], det=[detector('mandelbrot', 0.001, maxIterations=500, escapeRadius=10.0, columns=301, rows=241, points=100, maxRealCoordinate=1.5, maxImaginaryCoordinate=1.2, realAxisName='stage_x', imaginaryAxisName='stage_y', enableNoise=False, noiseFreeExposureTime=5.0, saveImage=True, saveSpectrum=True, saveValue=True)])");
	}

}
