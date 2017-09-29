package org.eclipse.scanning.test.scan;

import static org.junit.Assert.assertEquals;

import org.eclipse.scanning.api.CountableScannable;
import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.device.IScannableDeviceService;
import org.eclipse.scanning.api.points.MapPosition;
import org.eclipse.scanning.api.scan.event.IPositioner;
import org.eclipse.scanning.example.scannable.MockCountingPositionScannable;
import org.eclipse.scanning.example.scannable.MockScannableConnector;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class SetPositionTest {

	private static IRunnableDeviceService  dservice;
	private static IScannableDeviceService connector;
	private static CountableScannable<Number> cpsGood;
	private static CountableScannable<Number> cpsBad;

	@BeforeClass
	public static void before() {
		MockScannableConnector msc = new MockScannableConnector(null);
		cpsGood = new MockCountingPositionScannable("cpsGood", 10, true);
		cpsBad  = new MockCountingPositionScannable("cpsBad", 10, false);
		msc.register(cpsGood);
		msc.register(cpsBad);

		connector = msc;
		dservice  = new RunnableDeviceServiceImpl(connector);
	}

	@After
	public void reset() throws Exception {
		cpsGood.setPosition(10, null);
		cpsBad.setPosition(10, null);
		cpsGood.resetCount();
		cpsBad.resetCount();
	}

	@Test
	public void testMoveNoExtraGetPosition() throws Exception {

		// Something without
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("cpsGood:0:20"));

        assertEquals(0, cpsGood.getCount("getPosition"));
        assertEquals(20d, cpsGood.getPosition().doubleValue(), 0.0000001);
        assertEquals(1, cpsGood.getCount("setPosition"));
        assertEquals(1, cpsGood.getCount("getPosition"));
	}

	@Test
	public void testMoveExtraGetPosition() throws Exception {

		// Something without
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("cpsBad:0:20"));

        assertEquals(1, cpsBad.getCount("getPosition"));
        assertEquals(20d, cpsBad.getPosition().doubleValue(), 0.0000001);
        assertEquals(1, cpsBad.getCount("setPosition"));
        assertEquals(2, cpsBad.getCount("getPosition"));
	}


	@Test
	public void testMoveTwoThingsTogether() throws Exception {

		// Something without
		IPositioner     pos    = dservice.createPositioner();
        pos.setPosition(new MapPosition("cpsBad:0:20, cpsGood:0:20"));

        assertEquals(0, cpsGood.getCount("getPosition"));
        assertEquals(20d, cpsGood.getPosition().doubleValue(), 0.0000001);
        assertEquals(1, cpsGood.getCount("setPosition"));
        assertEquals(1, cpsGood.getCount("getPosition"));

        assertEquals(1, cpsBad.getCount("getPosition"));
        assertEquals(20d, cpsBad.getPosition().doubleValue(), 0.0000001);
        assertEquals(1, cpsBad.getCount("setPosition"));
        assertEquals(2, cpsBad.getCount("getPosition"));
	}

}
